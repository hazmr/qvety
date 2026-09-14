package com.qvety.config;

import com.qvety.auth.JwtFilter;
import com.qvety.auth.PasswordChangeGateFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Stateless chain. JwtFilter sets the principal from a bearer token; PasswordChangeGateFilter blocks
 * everything but /me and /me/password while a password change is pending. Role checks live on the
 * services (@PreAuthorize), not here, so a new endpoint cannot forget them.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter,
                                            PasswordChangeGateFilter gateFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                // Angular static files and the SPA fallback
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class)
            .addFilterAfter(gateFilter, JwtFilter.class)
            .headers(h -> h
                // SPA served from the jar: same-origin scripts only, never framed. HSTS is added by Spring
                // Security automatically on HTTPS requests (behind Caddy in part 16).
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                .frameOptions(f -> f.deny())
                .contentTypeOptions(c -> { })
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );
        return http.build();
    }

    /** The filters run inside the security chain only; stop Boot from also registering them with the servlet container. */
    @Bean
    FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        var reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    FilterRegistrationBean<PasswordChangeGateFilter> gateFilterRegistration(PasswordChangeGateFilter filter) {
        var reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
