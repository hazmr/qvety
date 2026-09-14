package com.qvety.practice;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping
    public PracticeDto getPractice() {
        return service.get();
    }

    @PutMapping
    public PracticeDto updatePractice(@Valid @RequestBody PracticeUpdateRequest request) {
        return service.update(request);
    }
}
