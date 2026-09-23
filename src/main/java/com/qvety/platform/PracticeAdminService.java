package com.qvety.platform;

import com.qvety.auth.PlatformPrincipal;
import com.qvety.common.DomainException;
import com.qvety.common.PhoneNormalizer;
import com.qvety.practice.PracticeStatus;
import com.qvety.reference.StarterCatalogSeeder;
import com.qvety.tenant.SystemContext;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The practice lifecycle, as seen from Qvety. Super-admin only, and every call runs in system context:
 * a practice is something you look at from outside rather than from within.
 *
 * Nothing here is transactional. System context must already be active when a transaction begins, so the
 * context is entered here and {@link PracticeAdminTransactions} holds the work that runs inside it.
 */
@Service
@PreAuthorize("hasAuthority('" + PlatformPrincipal.AUTHORITY + "')")
public class PracticeAdminService {

    /** No look-alike characters: the super admin reads this out over the phone. */
    private static final String PASSWORD_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PracticeAdminTransactions transactions;
    private final PasswordEncoder passwords;
    private final PracticeStatusFilter statusFilter;
    private final StarterCatalogSeeder catalog;

    public PracticeAdminService(PracticeAdminTransactions transactions, PasswordEncoder passwords,
                                PracticeStatusFilter statusFilter, StarterCatalogSeeder catalog) {
        this.transactions = transactions;
        this.passwords = passwords;
        this.statusFilter = statusFilter;
        this.catalog = catalog;
    }

    public List<PracticeSummaryDto> list() {
        return SystemContext.call(() -> transactions.findAll().stream().map(PracticeSummaryDto::of).toList());
    }

    public PracticeSummaryDto get(UUID id) {
        return SystemContext.call(() -> PracticeSummaryDto.of(transactions.findOne(id)));
    }

    public PracticeCreatedDto create(CreatePracticeRequest request) {
        var phone = PhoneNormalizer.toE164(request.adminPhone())
            .orElseThrow(() -> DomainException.badRequest("phone.invalid", "adminPhone"));
        var country = request.country().toUpperCase();
        // Checked before anything is written. The practices table also has a check constraint, so an
        // unsupported country is refused twice; this is the half that gives a readable answer.
        if (!catalog.supports(country)) {
            throw DomainException.badRequest("practice.country_unsupported", "country");
        }
        var temporaryPassword = temporaryPassword();

        var practiceId = SystemContext.call(() ->
            transactions.createPractice(request, country, phone, passwords.encode(temporaryPassword)));

        var created = SystemContext.call(() -> transactions.findOne(practiceId));
        // The only moment the password is readable; it is handed over in person or by WhatsApp.
        return new PracticeCreatedDto(PracticeSummaryDto.of(created), phone, temporaryPassword);
    }

    /** A manual move. The reason is required: its audit row is what answers "why were we suspended?". */
    public PracticeSummaryDto changeStatus(UUID id, PracticeStatus status, String reason) {
        if (reason == null || reason.isBlank()) {
            throw DomainException.badRequest("platform.reason_required", "reason");
        }
        var staffId = currentStaffId();
        var updated = SystemContext.call(() -> transactions.setStatus(id, status.name(), reason, staffId));
        statusFilter.forget(id);   // the clinic feels it now, not after the cache expires
        return PracticeSummaryDto.of(updated);
    }

    /** The spec requires a super admin's read of clinical data to be recorded, consented or not. */
    public void recordExport(UUID practiceId) {
        var staffId = currentStaffId();
        SystemContext.run(() -> transactions.recordExport(practiceId, staffId));
    }

    private static UUID currentStaffId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof PlatformPrincipal p ? p.userId() : null;
    }

    private static String temporaryPassword() {
        var sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
