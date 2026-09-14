package com.qvety.clients;

import com.qvety.common.DomainException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everyone reads; front desk and admin write. Rules: reachable (phone or email), archived is final for
 * edits, duplicates warn but never block. Cross-tenant ids are invisible under RLS and answer 404.
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
        var page = q == null || q.isBlank() ? clients.findByArchivedAtIsNull(pageable) : clients.search(q.trim(), pageable);
        return page.map(mapper::toDto);
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

    private List<ClientDto> warnings(Client saved) {
        return clients.findDuplicateCandidates(saved.getFullName(), saved.getPhone(), saved.getId())
            .stream().map(mapper::toDto).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
