package com.qvety.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint proving the veterinarian-flag check works independently of role.
 * Lives in src/test so nothing ships; part 13 puts the same annotation on real clinical methods.
 */
@RestController
public class VetFlagProbeController {

    @GetMapping("/api/v1/test/vet-only")
    @PreAuthorize("@access.isVeterinarian()")
    public String vetOnly() {
        return "ok";
    }
}
