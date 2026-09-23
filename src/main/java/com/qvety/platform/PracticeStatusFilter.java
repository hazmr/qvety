package com.qvety.platform;

import com.qvety.auth.AuthenticatedUser;
import com.qvety.common.ApiError;
import com.qvety.practice.PracticeStatus;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qvety.tenant.SystemContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * What a practice's status allows, enforced on every request rather than trusted to the UI.
 *
 * `past_due` changes nothing but adds a header the client turns into a banner. `suspended` keeps the
 * clinic readable but refuses writes with 423, so a practice that stops paying can still look up a phone
 * number. `closed` refuses everything except the practice's own export, because the data stays theirs.
 *
 * The status is cached for 60 seconds and dropped on change, so a suspension takes effect within a minute
 * without a database read on every request.
 */
@Component
public class PracticeStatusFilter extends OncePerRequestFilter {

    /** The client reads this and shows the banner; it says nothing a token does not already say. */
    public static final String PAST_DUE_HEADER = "X-Practice-Past-Due";
    static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String EXPORT_PATH = "/api/v1/practice/export";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final PlatformPracticeRepository practices;
    private final MessageSource messages;
    private final ObjectMapper json;
    private final Cache<UUID, PracticeStatus> cache =
        Caffeine.newBuilder().expireAfterWrite(CACHE_TTL).maximumSize(10_000).build();

    public PracticeStatusFilter(PlatformPracticeRepository practices, MessageSource messages, ObjectMapper json) {
        this.practices = practices;
        this.messages = messages;
        this.json = json;
    }

    /** Called when the super admin changes a status, so the clinic does not wait out the TTL. */
    public void forget(UUID practiceId) {
        cache.invalidate(practiceId);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            chain.doFilter(request, response);   // anonymous, or a platform token: no practice to judge
            return;
        }
        var status = statusOf(user.practiceId());
        if (status == null) {
            chain.doFilter(request, response);
            return;
        }
        var path = request.getRequestURI();
        switch (status) {
            case closed -> {
                if (!EXPORT_PATH.equals(path)) {
                    refuse(response, HttpServletResponse.SC_FORBIDDEN, "platform.closed");
                    return;
                }
            }
            case suspended -> {
                if (!READ_METHODS.contains(request.getMethod())) {
                    refuse(response, 423, "platform.suspended");   // Locked: the clinic is readable, not writable
                    return;
                }
            }
            case past_due -> response.setHeader(PAST_DUE_HEADER, "true");
            default -> { }
        }
        chain.doFilter(request, response);
    }

    private PracticeStatus statusOf(UUID practiceId) {
        return cache.get(practiceId, id -> SystemContext.call(() -> practices.findOnePractice(id)).stream()
            .findFirst().map(p -> p.getStatus()).orElse(null));
    }

    private void refuse(HttpServletResponse response, int code, String messageKey) throws IOException {
        var message = messages.getMessage(messageKey, null, messageKey, LocaleContextHolder.getLocale());
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(new ApiError(messageKey, message, null)));
    }
}
