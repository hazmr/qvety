package com.qvety.clients;

import com.qvety.common.DomainException;
import com.qvety.common.PhoneNormalizer;
import com.qvety.common.TextNormalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everyone reads; front desk and admin write. Rules: reachable (phone or email), phones must parse as
 * Egyptian numbers, archived is final for edits, duplicates warn but never block. Search, duplicates and
 * name sorting run on the folded name and E.164 phones (docs/domain/search-and-normalization.md).
 * Cross-tenant ids are invisible under RLS and answer 404.
 */
@Service
public class ClientService {

    private final ClientRepository clients;
    private final ClientMapper mapper;

    public ClientService(ClientRepository clients, ClientMapper mapper) {
        this.clients = clients;
        this.mapper = mapper;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<ClientDto> list(String q, Pageable pageable) {
        return search(q, pageable).map(mapper::toDto);
    }

    private Page<Client> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return clients.findByArchivedAtIsNull(foldedSort(pageable));
        }
        var phone = PhoneNormalizer.toE164(q);
        if (phone.isPresent()) {
            return clients.searchByPhone(phone.get(), foldedSort(pageable));
        }
        // the native query orders by similarity itself; a sort on the Pageable would be appended after it
        return clients.searchByName(TextNormalizer.fold(q), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }

    /** Sorting by name means the folded name, so "أحمد" and "احمد" sit together; other properties pass through. */
    private static Pageable foldedSort(Pageable pageable) {
        var sort = Sort.by(pageable.getSort().stream()
            .map(o -> o.getProperty().equals("fullName") ? o.withProperty("fullNameNormalized") : o).toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ClientDto get(UUID id) {
        return mapper.toDto(load(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FRONT_DESK')")
    @Transactional
    public ClientSavedDto create(ClientRequest request) {
        var normalized = normalize(request);
        requireReachable(normalized);
        var client = new Client();
        mapper.apply(normalized, client);
        fold(client);
        var saved = clients.saveAndFlush(client);
        return new ClientSavedDto(mapper.toDto(saved), warnings(saved));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FRONT_DESK')")
    @Transactional
    public ClientSavedDto update(UUID id, ClientRequest request, long version) {
        var client = load(id);
        if (client.isArchived()) {
            throw DomainException.conflict("client.archived");
        }
        var normalized = normalize(request);
        requireReachable(normalized);
        if (client.getVersion() != version) {   // the caller edited an older copy (two tabs, two people)
            throw DomainException.conflict("stale_update");
        }
        mapper.apply(normalized, client);
        fold(client);
        var saved = clients.saveAndFlush(client);
        return new ClientSavedDto(mapper.toDto(saved), warnings(saved));
    }

    /** Never a delete: the row and everything hanging off it stay. */
    @PreAuthorize("hasAnyRole('ADMIN', 'FRONT_DESK')")
    @Transactional
    public ClientDto archive(UUID id) {
        var client = load(id);
        if (!client.isArchived()) {
            client.setArchivedAt(OffsetDateTime.now());
        }
        return mapper.toDto(client);
    }

    private Client load(UUID id) {
        return clients.findById(id).orElseThrow(() -> DomainException.notFound("client.not_found"));
    }

    /** Blank strings become null so the reachability check and the unique-ish duplicate scan see real absence. */
    private static ClientRequest normalize(ClientRequest r) {
        return new ClientRequest(r.fullName().trim(), blankToNull(r.preferredName()), blankToNull(r.phone()),
            blankToNull(r.phoneSecondary()), blankToNull(r.email()) == null ? null : r.email().trim().toLowerCase(),
            blankToNull(r.address()), blankToNull(r.notes()), blankToNull(r.preferredLocale()));
    }

    private static void requireReachable(ClientRequest r) {
        if (r.phone() == null && r.email() == null) {
            throw DomainException.badRequest("client.unreachable");
        }
    }

    /** The stored search columns: folded name, E.164 phones. Typed values stay on the row as typed. */
    private static void fold(Client c) {
        c.setFullNameNormalized(TextNormalizer.fold(c.getFullName()));
        c.setPhoneE164(toE164(c.getPhone(), "phone"));
        c.setPhoneSecondaryE164(toE164(c.getPhoneSecondary(), "phoneSecondary"));
    }

    private static String toE164(String typed, String field) {
        if (typed == null) {
            return null;
        }
        return PhoneNormalizer.toE164(typed).orElseThrow(() -> DomainException.badRequest("phone.invalid", field));
    }

    private List<ClientDto> warnings(Client saved) {
        return clients.findDuplicateCandidates(saved.getFullNameNormalized(), saved.getPhoneE164(),
                saved.getPhoneSecondaryE164(), saved.getId())
            .stream().map(mapper::toDto).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
