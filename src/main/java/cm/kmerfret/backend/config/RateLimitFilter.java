package cm.kmerfret.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter simple en mémoire — protège les endpoints d'authentification.
 * Fenêtre glissante de 60s, max 10 requêtes par IP.
 * Pour la production, remplacer par Bucket4j + Redis.
 */
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS   = 60_000L;

    private record BucketEntry(AtomicInteger count, long windowStart) {}

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // Appliquer uniquement aux endpoints d'auth
        String path = request.getRequestURI();
        if (!path.startsWith("/api/auth/")) {
            chain.doFilter(req, resp);
            return;
        }

        String ip  = getClientIp(request);
        long   now = System.currentTimeMillis();

        BucketEntry entry = buckets.compute(ip, (k, existing) -> {
            if (existing == null || (now - existing.windowStart()) > WINDOW_MS) {
                return new BucketEntry(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (entry.count().get() > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Trop de tentatives. Réessayez dans 60 secondes.\"}");
            return;
        }

        chain.doFilter(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
