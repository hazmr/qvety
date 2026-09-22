package com.qvety.scheduling;

import com.qvety.auth.CurrentUser;
import com.qvety.clients.ClientRepository;
import com.qvety.common.DomainException;
import com.qvety.patients.PatientRepository;
import com.qvety.practice.PracticeRepository;
import com.qvety.reference.AppointmentTypeRepository;
import com.qvety.reference.RoomRepository;
import com.qvety.users.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking and the clinic day. Everyone reads; front desk and admin book and move a patient through the
 * day. Rules: the veterinarian must carry the flag, the room and type must be active, overlap is refused
 * by the database and reported as 409, booking outside opening hours warns but saves, and a status only
 * moves along the transition table on {@link AppointmentStatus}. Cancelling keeps the row.
 */
@Service
public class AppointmentService {

    /** Postgres exclusion_violation: the two overlap constraints are the only ones on this table. */
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final AppointmentRepository appointments;
    private final PracticeHoursRepository hours;
    private final PatientRepository patients;
    private final ClientRepository clients;
    private final UserRepository users;
    private final RoomRepository rooms;
    private final AppointmentTypeRepository types;
    private final PracticeRepository practices;
    private final AppointmentMapper mapper;
    private final CurrentUser currentUser;
    private final EntityManager entityManager;

    public AppointmentService(AppointmentRepository appointments, PracticeHoursRepository hours,
                              PatientRepository patients, ClientRepository clients, UserRepository users,
                              RoomRepository rooms, AppointmentTypeRepository types, PracticeRepository practices,
                              AppointmentMapper mapper, CurrentUser currentUser, EntityManager entityManager) {
        this.appointments = appointments;
        this.hours = hours;
        this.patients = patients;
        this.clients = clients;
        this.users = users;
        this.rooms = rooms;
        this.types = types;
        this.practices = practices;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.entityManager = entityManager;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<AppointmentDto> list(LocalDate date, UUID veterinarianId) {
        return dayRows(date, veterinarianId).stream().map(mapper::toDto).toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public AppointmentDto get(UUID id) {
        return mapper.toDto(load(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FRONT_DESK')")
    @Transactional
    public AppointmentSavedDto create(AppointmentRequest request) {
        var appointment = new Appointment();
        apply(request, appointment);
        if (request.isWalkIn()) {
            // A walk-in is already in the building: it is checked in from the start and stays a walk-in.
            appointment.setOrigin(AppointmentOrigin.walk_in);
            appointment.setStatus(AppointmentStatus.checked_in);
            appointment.setCheckedInAt(OffsetDateTime.now());
        }
        var saved = save(appointment);
        return new AppointmentSavedDto(mapper.toDto(saved), hoursWarnings(saved));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FRONT_DESK')")
    @Transactional
    public AppointmentSavedDto update(UUID id, AppointmentRequest request, long version) {
        var appointment = load(id);
        if (appointment.getVersion() != version) {   // the caller edited an older copy (two tabs, two people)
            throw DomainException.conflict("stale_update");
        }
        if (!appointment.getStatus().holdsItsSlot()) {
            throw DomainException.conflict("appointment.closed");
        }
        apply(request, appointment);   // origin is not updatable; walkIn is ignored here
        var saved = save(appointment);
        return new AppointmentSavedDto(mapper.toDto(saved), hoursWarnings(saved));
    }

    /**
     * The only way a status moves. An unlisted move is a conflict, not a validation error: the row is
     * fine, the caller is looking at a stale screen.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONT_DESK')")
    @Transactional
    public AppointmentDto changeStatus(UUID id, AppointmentStatus next) {
        var appointment = load(id);
        if (appointment.getStatus() == next) {
            return mapper.toDto(appointment);   // idempotent: the desk pressed twice
        }
        if (!appointment.getStatus().canMoveTo(next)) {
            throw DomainException.conflict("appointment.bad_transition");
        }
        if (next == AppointmentStatus.checked_in) {
            appointment.setCheckedInAt(OffsetDateTime.now());
        }
        appointment.setStatus(next);
        // Moving back into a slot-holding status can collide with a booking made meanwhile.
        return mapper.toDto(save(appointment));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public DayDto day(LocalDate date, UUID veterinarianId) {
        var zone = practiceZone();
        var day = date == null ? LocalDate.now(zone) : date;
        var columns = new LinkedHashMap<UUID, List<AppointmentDto>>();
        var names = new LinkedHashMap<UUID, String>();
        for (var a : dayRows(day, veterinarianId)) {
            var vet = a.getVeterinarian();
            names.putIfAbsent(vet.getId(), vet.getFullName());
            columns.computeIfAbsent(vet.getId(), k -> new ArrayList<>()).add(mapper.toDto(a));
        }
        var open = hours.findByWeekday(weekday(day));
        return new DayDto(day, zone.getId(),
            open.map(PracticeHours::getOpens).orElse(null),
            open.map(PracticeHours::getCloses).orElse(null),
            names.entrySet().stream().map(e -> new DayColumnDto(e.getKey(), e.getValue(), columns.get(e.getKey()))).toList());
    }

    /** A view over today's appointments, never a stored table: the board is always what the rows say now. */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public BoardDto board() {
        var zone = practiceZone();
        var today = LocalDate.now(zone);
        var rows = appointments.findBoard(startOfDay(today, zone), startOfDay(today.plusDays(1), zone));
        return new BoardDto(column(rows, AppointmentStatus.checked_in),
            column(rows, AppointmentStatus.in_progress), column(rows, AppointmentStatus.completed));
    }

    private static List<BoardEntryDto> column(List<AppointmentRepository.BoardRow> rows, AppointmentStatus status) {
        return rows.stream()
            .filter(r -> AppointmentStatus.valueOf(r.getStatus()) == status)
            .map(r -> new BoardEntryDto(r.getId(), r.getPatientName(), r.getClientName(), r.getVeterinarianName(),
                r.getRoomName(), r.getStartsAt().atOffset(ZoneOffset.UTC), AppointmentStatus.valueOf(r.getStatus()),
                AppointmentOrigin.valueOf(r.getOrigin()), r.getMinutesWaiting(), r.getReason()))
            .toList();
    }

    private List<Appointment> dayRows(LocalDate date, UUID veterinarianId) {
        var zone = practiceZone();
        var day = date == null ? LocalDate.now(zone) : date;
        return appointments.findDay(startOfDay(day, zone), startOfDay(day.plusDays(1), zone), veterinarianId);
    }

    /** Copies the request onto the row, loading and checking every reference it names. */
    private void apply(AppointmentRequest request, Appointment appointment) {
        var patient = patients.findById(request.patientId())
            .orElseThrow(() -> DomainException.badRequest("patient.not_found", "patientId"));
        var vet = users.findById(request.veterinarianId())
            .orElseThrow(() -> DomainException.badRequest("user_not_found", "veterinarianId"));
        if (!vet.isVeterinarian()) {
            // The clinical act is the vet's; the flag decides, never the role.
            throw DomainException.badRequest("appointment.not_a_veterinarian", "veterinarianId");
        }
        if (!vet.isActive()) {
            throw DomainException.badRequest("appointment.veterinarian_inactive", "veterinarianId");
        }
        var room = rooms.findById(request.roomId())
            .orElseThrow(() -> DomainException.badRequest("reference.not_found", "roomId"));
        if (!room.isActive()) {
            throw DomainException.badRequest("appointment.room_inactive", "roomId");
        }
        var type = types.findById(request.appointmentTypeId())
            .orElseThrow(() -> DomainException.badRequest("reference.not_found", "appointmentTypeId"));
        if (!type.isActive()) {
            throw DomainException.badRequest("appointment.type_inactive", "appointmentTypeId");
        }
        appointment.setPatient(patient);
        // The client comes from the patient, never from the request: an appointment always names the
        // owner of record at the time of booking.
        appointment.setClient(clients.findById(patient.getClient().getId())
            .orElseThrow(() -> DomainException.badRequest("client.not_found", "patientId")));
        appointment.setVeterinarian(vet);
        appointment.setRoom(room);
        appointment.setAppointmentType(type);
        appointment.setStartsAt(request.start());
        appointment.setEndsAt(request.start().plusMinutes(request.durationMinutes()));
        appointment.setReason(blankToNull(request.reason()));
        appointment.setNotes(blankToNull(request.notes()));
    }

    /**
     * The database owns the overlap rule, so the flush is where a double booking surfaces. Two desk
     * sessions can pass a service-side check in the same second; only the constraint is safe.
     */
    private Appointment save(Appointment appointment) {
        try {
            appointments.save(appointment);
            entityManager.flush();
            return appointment;
        } catch (DataIntegrityViolationException e) {
            if (isExclusionViolation(e)) {
                throw DomainException.conflict("appointment.overlap");
            }
            throw e;
        }
    }

    private static boolean isExclusionViolation(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.sql.SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /** Outside opening hours is a warning, never a refusal: clinics do see patients after closing. */
    private List<String> hoursWarnings(Appointment appointment) {
        var zone = practiceZone();
        var start = appointment.getStartsAt().atZoneSameInstant(zone);
        var end = appointment.getEndsAt().atZoneSameInstant(zone);
        var open = hours.findByWeekday(weekday(start.toLocalDate()));
        if (open.isEmpty()) {
            return List.of("appointment.outside_hours");   // the clinic is shut that weekday
        }
        var h = open.get();
        boolean inside = !start.toLocalTime().isBefore(h.getOpens())
            && !end.toLocalTime().isAfter(h.getCloses())
            && start.toLocalDate().equals(end.toLocalDate());
        return inside ? List.of() : List.of("appointment.outside_hours");
    }

    private Appointment load(UUID id) {
        return appointments.findById(id).orElseThrow(() -> DomainException.notFound("appointment.not_found"));
    }

    /** Everything is stored as an instant; the practice timezone is what turns a date into a window. */
    private ZoneId practiceZone() {
        return ZoneId.of(practices.findById(currentUser.practiceId()).orElseThrow().getTimezone());
    }

    private static OffsetDateTime startOfDay(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).toOffsetDateTime();
    }

    /** Postgres EXTRACT(DOW) numbering, which the practice_hours rows use: 0 = Sunday. */
    private static short weekday(LocalDate date) {
        return (short) (date.getDayOfWeek().getValue() % 7);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
