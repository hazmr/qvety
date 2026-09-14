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
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the bearer token, then loads the user row and rejects the token when the user is inactive or
 * the token's session version no longer matches. That lookup is what makes deactivation and password
 * reset immediate without a session store. No @Transactional here: a proxied filter breaks Tomcat init;
 * the repository call is transactional on its own.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
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
            var practiceId = UUID.fromString(jwt.getClaimAsString("practiceId"));
            // The row read goes through RLS under the tenant the signed token names.
            var user = TenantContext.runAs(new TenantScope(practiceId, userId),
                () -> users.findById(userId).orElse(null));
            int tokenVersion = jwt.getClaim("sv") instanceof Number n ? n.intValue() : -1;
            if (user == null || !user.isActive() || !user.getPracticeId().equals(practiceId)
                    || user.getSessionVersion() != tokenVersion) {
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
