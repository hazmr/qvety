package com.qvety.auth;

import com.qvety.tenant.TenantContext;
import com.qvety.tenant.TenantScope;
import com.qvety.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the bearer token, then loads the user row and rejects the token when the user is inactive or
 * the token's session version no longer matches. Two token shapes pass through here: a practice token
 * carrying practiceId, and a platform token carrying {@code platform: true} and no practice at all. That lookup is what makes deactivation and password
 * reset immediate without a session store. No @Transactional here: a proxied filter breaks Tomcat init;
 * the repository call is transactional on its own.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;
    /** Absent only if the platform package is ever removed; the filter then simply rejects platform tokens. */
    private final Optional<PlatformAuthentication> platform;

    public JwtFilter(JwtService jwtService, UserRepository users, Optional<PlatformAuthentication> platform) {
        this.jwtService = jwtService;
        this.users = users;
        this.platform = platform;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            var jwt = jwtService.verify(header.substring(7));
            var userId = UUID.fromString(jwt.getSubject());
            int version = jwt.getClaim("sv") instanceof Number n ? n.intValue() : -1;
            // A platform token carries no practiceId and never enters a tenant. It is still authenticated
            // here rather than ignored, so a super admin on a clinic endpoint is refused by authorization
            // (403) instead of looking like an anonymous caller (401).
            if (Boolean.TRUE.equals(jwt.getClaim("platform"))) {
                platform.flatMap(p -> p.verify(userId, version)).ifPresent(principal -> {
                    var authorities = List.of(new SimpleGrantedAuthority(PlatformPrincipal.AUTHORITY));
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
                });
                chain.doFilter(request, response);
                return;
            }
            var practiceId = UUID.fromString(jwt.getClaimAsString("practiceId"));
            // The row read goes through RLS under the tenant the signed token names.
            var user = TenantContext.runAs(new TenantScope(practiceId, userId),
                () -> users.findById(userId).orElse(null));
            if (user == null || !user.isActive() || !user.getPracticeId().equals(practiceId)
                    || user.getSessionVersion() != version) {
                chain.doFilter(request, response);   // no principal set: the chain answers 401
                return;
            }
            var principal = new AuthenticatedUser(
                user.getId(), user.getPracticeId(), user.getRole(), user.isVeterinarian(),
                user.getFullName(), user.isMustChangePassword());
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase()));
            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
