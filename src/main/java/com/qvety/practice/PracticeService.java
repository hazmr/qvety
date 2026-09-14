package com.qvety.practice;

import com.qvety.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PracticeService {

    private final PracticeRepository repository;
    private final PracticeMapper mapper;
    private final CurrentUser currentUser;

    public PracticeService(PracticeRepository repository, PracticeMapper mapper, CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    /** The caller's own practice, from the token. */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PracticeDto get() {
        return mapper.toDto(load());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PracticeDto update(PracticeUpdateRequest request) {
        var practice = load();
        mapper.updateFromRequest(request, practice);
        return mapper.toDto(repository.saveAndFlush(practice));
    }

    private Practice load() {
        return repository.findById(currentUser.practiceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "practice_not_found"));
    }
}
