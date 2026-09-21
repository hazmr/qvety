package com.qvety.auth;

import com.qvety.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * While must_change_password is set, every endpoint except /me and /me/password answers 403 password_change_required.
 * The body is the same ApiError shape as the controllers write, localized per Accept-Language.
 */
@Component
public class PasswordChangeGateFilter extends OncePerRequestFilter {

    static final String CODE = "password_change_required";

    private final MessageSource messages;
    private final ObjectMapper json;

    public PasswordChangeGateFilter(MessageSource messages, ObjectMapper json) {
        this.messages = messages;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user && user.mustChangePassword()) {
            var path = request.getRequestURI();
            if (!path.equals("/api/v1/me") && !path.equals("/api/v1/me/password")) {
                var message = messages.getMessage(CODE, null, CODE, LocaleContextHolder.getLocale());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(json.writeValueAsString(new ApiError(CODE, message, null)));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
