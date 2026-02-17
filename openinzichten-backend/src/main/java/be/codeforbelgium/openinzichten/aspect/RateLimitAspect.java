package be.codeforbelgium.openinzichten.aspect;

import be.codeforbelgium.openinzichten.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {
    private final Map<String, List<Long>> requests = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit)
            throws Throwable {
        String userId = extractUserIdFromJwt();

        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthenticated requests cannot be rate limited.");
        }

        String key = pjp.getSignature().toShortString() + ":" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - (rateLimit.perSeconds() * 1000L);

        requests.compute(key, (k, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }
            // Remove old timestamps outside the time window
            timestamps.removeIf(t -> t < windowStart);
            timestamps.add(now);
            return timestamps;
        });

        if (requests.get(key).size() > rateLimit.requests()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Try again later.");
        }

        return pjp.proceed();
    }

    private String extractUserIdFromJwt() {
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }

        return null;
    }
}
