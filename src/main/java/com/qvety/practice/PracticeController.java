package com.qvety.practice;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice")
@Tag(name = "practice")   // generated Angular client: PracticeApi
public class PracticeController {

    private final PracticeService service;

    public PracticeController(PracticeService service) {
        this.service = service;
    }

    /**
     * Part 02 scaffolding: the practice comes from the X-Practice-Id header. Part 03 replaces the
     * header with the JWT claim and deletes this parameter.
     */
    @GetMapping
    public PracticeDto getPractice(@RequestHeader("X-Practice-Id") UUID practiceId) {
        return service.get(practiceId);
    }
}
