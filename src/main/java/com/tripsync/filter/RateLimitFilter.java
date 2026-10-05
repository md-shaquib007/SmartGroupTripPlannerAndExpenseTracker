package com.tripsync.filter;

import com.tripsync.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@WebFilter("/api/*")
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final long ONE_MINUTE_MS = 60_000L;

    private static class RequestTracker {
        final long startTime = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final Map<String, RequestTracker> ipTrackers = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();
        if (path.endsWith("/api/health")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(req);
        long now = System.currentTimeMillis();

        // Perform periodic cleanup if map grows
        if (ipTrackers.size() > 2_000) {
            ipTrackers.entrySet().removeIf(entry -> (now - entry.getValue().startTime) > ONE_MINUTE_MS);
        }

        RequestTracker tracker = ipTrackers.compute(clientIp, (ip, existing) -> {
            if (existing == null || (now - existing.startTime) > ONE_MINUTE_MS) {
                return new RequestTracker();
            }
            return existing;
        });

        int requests = tracker.count.incrementAndGet();

        if (requests > MAX_REQUESTS_PER_MINUTE) {
            resp.setHeader("Retry-After", "60");
            JsonUtil.writeError(resp, 429, "Rate limit exceeded. Maximum 120 requests per minute allowed.");
            return;
        }

        resp.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        resp.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - requests)));

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String candidate = xForwardedFor.split(",")[0].trim();
            if (candidate.length() <= 45 && candidate.matches("^[a-fA-F0-9:.]+$")) {
                return candidate;
            }
        }
        String remoteAddr = req.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
