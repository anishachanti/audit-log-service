package com.anish.auditlogservice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class CreateEventRateLimitInterceptor
        implements HandlerInterceptor, WebMvcConfigurer {

    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Counter> counters =
            new ConcurrentHashMap<>();

    public CreateEventRateLimitInterceptor(
            @Value("${audit.rate-limit.create.max-requests:10}") int maxRequests,
            @Value("${audit.rate-limit.create.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1_000;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/audit/events");
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        Counter counter = counters.computeIfAbsent(
                authentication.getName(),
                ignored -> new Counter());

        if (counter.allow(maxRequests, windowMillis)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Event creation rate limit exceeded\"}");
        return false;
    }

    private static final class Counter {
        private long windowStartedAt = System.currentTimeMillis();
        private int requestCount;

        synchronized boolean allow(int maxRequests, long windowMillis) {
            long now = System.currentTimeMillis();

            if (now - windowStartedAt >= windowMillis) {
                windowStartedAt = now;
                requestCount = 0;
            }

            if (requestCount >= maxRequests) {
                return false;
            }

            requestCount++;
            return true;
        }
    }
}