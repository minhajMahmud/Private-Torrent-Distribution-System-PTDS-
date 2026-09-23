package com.ptds.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic sliding-window rate limiter for sensitive auth endpoints (login,
 * register, forgot-password). Production deployments should back this with
 * Redis (see app.redis config) for correctness across multiple instances;
 * this in-memory version is sufficient for a single-instance / Phase-1 demo.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private record Bucket(AtomicInteger count, long windowStart) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final String[] LIMITED_PATHS = {
            "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean limited = false;
        for (String p : LIMITED_PATHS) {
            if (path.equals(p)) { limited = true; break; }
        }

        if (limited) {
            String key = clientKey(request) + ":" + path;
            long now = Instant.now().toEpochMilli();

            Bucket bucket = buckets.compute(key, (k, existing) -> {
                if (existing == null || now - existing.windowStart() > WINDOW_MS) {
                    return new Bucket(new AtomicInteger(1), now);
                }
                existing.count().incrementAndGet();
                return existing;
            });

            if (bucket.count().get() > MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded, try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
