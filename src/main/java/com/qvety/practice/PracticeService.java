package com.qvety.practice;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PracticeService {

    private final PracticeRepository repository;
    private final PracticeMapper mapper;

    public PracticeService(PracticeRepository repository, PracticeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** 404 for unknown ids; never 403, so the existence of another practice is not revealed. */
    @Transactional(readOnly = true)
    public PracticeDto get(UUID id) {
        return repository.findById(id)
            .map(mapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "practice_not_found"));
    }
}
