package com.qvety.platform;

import com.qvety.common.DomainException;
import com.qvety.practice.Practice;
import com.qvety.reference.StarterCatalogSeeder;
import com.qvety.users.User;
import com.qvety.users.UserRepository;
import com.qvety.users.UserRole;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional half of {@link PracticeAdminService}.
 *
 * It is a separate bean because system context has to be active when the transaction *begins*: the hook
 * decides whether to set `app.practice_id` at that moment, so entering the context inside an already
 * open transaction is too late and the transaction is refused. The service enters the context, then
 * calls in here, and the transaction starts inside it.
 */
@Component
class PracticeAdminTransactions {

    private final PlatformPracticeRepository practices;
    private final PlatformAuditRepository audit;
    private final UserRepository users;
    private final StarterCatalogSeeder catalog;
    private final JdbcTemplate jdbc;

    PracticeAdminTransactions(PlatformPracticeRepository practices, PlatformAuditRepository audit,
                              UserRepository users, StarterCatalogSeeder catalog, JdbcTemplate jdbc) {
        this.practices = practices;
        this.audit = audit;
        this.users = users;
        this.catalog = catalog;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    List<Practice> findAll() {
        return practices.findEveryPractice();
    }

    @Transactional(readOnly = true)
    Practice findOne(UUID id) {
        return practices.findOnePractice(id).stream().findFirst()
            .orElseThrow(() -> DomainException.notFound("practice_not_found"));
    }

    /**
     * One transaction for the whole practice. The practices row needs privilege, so it goes through the
     * definer function; the catalog and the first user are ordinary tenant writes, so `app.practice_id`
     * is set on the same connection once the id exists and row-level security applies from there on.
     * An unmapped country throws inside the catalog step and takes all of it back.
     */
    @Transactional
    UUID createPractice(CreatePracticeRequest request, String country, String phone, String passwordHash) {
        var practiceId = practices.createPractice(request.name().trim(), country,
            request.currency().toUpperCase(), request.locale(), request.timezone(), trialDays());

        // From here the transaction is inside the new practice, so the inserts below pass through RLS.
        jdbc.queryForObject("SELECT set_config('app.practice_id', ?, true)", String.class, practiceId.toString());

        catalog.seed(practiceId, country);

        var admin = new User();
        admin.setPracticeId(practiceId);
        admin.setPhone(phone);
        admin.setEmail(request.adminEmail() == null || request.adminEmail().isBlank()
            ? null : request.adminEmail().trim().toLowerCase());
        admin.setFullName(request.adminName().trim());
        admin.setRole(UserRole.admin);
        admin.setVeterinarian(request.isAdminVeterinarian());
        admin.setPasswordHash(passwordHash);
        admin.setMustChangePassword(true);
        users.saveAndFlush(admin);

        write(practiceId, "practice.created", Map.of("country", country, "name", request.name().trim()), null);
        return practiceId;
    }

    @Transactional
    Practice setStatus(UUID id, String status, String reason, UUID staffId) {
        findOne(id);   // 404 before anything is written
        var updated = practices.setStatus(id, status).stream().findFirst()
            .orElseThrow(() -> DomainException.notFound("practice_not_found"));
        write(id, "practice.status_changed", Map.of("status", status, "reason", reason.trim()), staffId);
        return updated;
    }

    @Transactional
    void recordExport(UUID practiceId, UUID staffId) {
        write(practiceId, "practice.exported", Map.of(), staffId);
    }

    private void write(UUID practiceId, String action, Map<String, Object> details, UUID staffId) {
        var entry = new PlatformAuditEntry();
        entry.setPlatformUserId(staffId);
        entry.setAction(action);
        entry.setTargetType("practice");
        entry.setTargetId(practiceId);
        entry.setDetails(details.isEmpty() ? null : details);
        audit.saveAndFlush(entry);
    }

    private int trialDays() {
        return jdbc.queryForObject("SELECT value::int FROM platform_settings WHERE key = 'trial_days'", Integer.class);
    }
}
