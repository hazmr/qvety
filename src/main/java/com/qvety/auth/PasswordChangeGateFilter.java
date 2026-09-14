package com.qvety.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** While must_change_password is set, every endpoint except /me and /me/password answers 403 password_change_required. */
@Component
public class PasswordChangeGateFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user && user.mustChangePassword()) {
            var path = request.getRequestURI();
            if (!path.equals("/api/v1/me") && !path.equals("/api/v1/me/password")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"password_change_required\",\"message\":\"Password change required\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
