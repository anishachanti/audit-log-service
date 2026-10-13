package com.anish.auditlogservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;


import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain auditSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Read-only endpoints: readers and admins
                .requestMatchers(
                    HttpMethod.GET,
                    "/audit/events",
                    "/audit/events/export",
                    "/audit/events/compliance/report",
                    "/audit/verify"
                ).hasAnyRole("AUDIT_READER", "AUDIT_ADMIN")

                // Create, archive, redact, or run retention: admins only
                .requestMatchers(
                    HttpMethod.POST,
                    "/audit/events",
                    "/audit/events/retention/archive-expired"
                ).hasRole("AUDIT_ADMIN")
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/audit/events/**"
                ).hasRole("AUDIT_ADMIN")

                // Any other route requires admin access
                .anyRequest().hasRole("AUDIT_ADMIN")
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${audit.security.admin.username}") String adminUsername,
            @Value("${audit.security.admin.password}") String adminPassword,
            @Value("${audit.security.reader.username}") String readerUsername,
            @Value("${audit.security.reader.password}") String readerPassword,
            @Value("${audit.security.reader.resource-ids}") String readerResourceIds) {

        UserDetails admin = User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("AUDIT_ADMIN")
                .build();

        List<GrantedAuthority> readerAuthorities = new ArrayList<>();
        readerAuthorities.add(new SimpleGrantedAuthority("ROLE_AUDIT_READER"));

        for (String resourceId : readerResourceIds.split(",")) {
             String trimmedResourceId = resourceId.trim();

             if (!trimmedResourceId.isEmpty()) {
                 readerAuthorities.add(new SimpleGrantedAuthority("RESOURCE_ID:" + trimmedResourceId));
             }
        }

        UserDetails reader = User.withUsername(readerUsername)
                .password(passwordEncoder.encode(readerPassword))
                .authorities(readerAuthorities)
                .build();

        return new InMemoryUserDetailsManager(admin, reader);
    }
}