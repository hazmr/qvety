package com.qvety.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Part 01 baseline: stateless chain, health and the Angular app open, everything else denied.
 * Part 03 adds the JWT filter and the /api/v1 rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // Angular static files and the SPA fallback
                .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/*.ico", "/assets/**", "/media/**").permitAll()
                // Part 02 scaffolding: no auth yet, the practice comes from X-Practice-Id. Part 03 removes this line.
                .requestMatchers("/api/v1/practice").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .headers(Customizer.withDefaults());
        return http.build();
    }
}
