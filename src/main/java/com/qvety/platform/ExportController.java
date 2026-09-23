package com.qvety.platform;

import com.qvety.auth.CurrentUser;
import com.qvety.auth.PlatformPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Both doors to the same export. The practice admin takes their own data at any status, including closed;
 * the super admin takes a copy for a clinic that asks for help, and that read is recorded.
 *
 * The practice-side endpoint lives in this package although its path is /api/v1: feature packages must
 * not import platform, and the export belongs to the platform side of the product.
 *
 * The document is written straight to the response rather than through a StreamingResponseBody. That
 * would re-dispatch the request asynchronously, and the authorization filter runs again on that dispatch
 * with an empty security context, refusing a response already half written. Writing here keeps it one
 * dispatch, and virtual threads make holding the request thread cheap.
 */
@RestController
@Tag(name = "platform")
public class ExportController {

    private final ExportService export;
    private final PracticeAdminService admin;
    private final CurrentUser currentUser;

    public ExportController(ExportService export, PracticeAdminService admin, CurrentUser currentUser) {
        this.export = export;
        this.admin = admin;
        this.currentUser = currentUser;
    }

    /** The clinic's own copy. Reachable even when the practice is closed; the status filter lets this path through. */
    @GetMapping(value = "/api/v1/practice/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public void exportOwnPractice(HttpServletResponse response) throws IOException {
        stream(currentUser.practiceId(), response);
    }

    @GetMapping(value = "/api/platform/practices/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + PlatformPrincipal.AUTHORITY + "')")
    public void exportPractice(@PathVariable UUID id, HttpServletResponse response) throws IOException {
        admin.recordExport(id);   // consented access is still audited access
        stream(id, response);
    }

    private void stream(UUID practiceId, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"qvety-export-" + practiceId + ".json\"");
        export.writeTo(practiceId, response.getOutputStream());
        response.flushBuffer();
    }
}
