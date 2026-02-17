package be.codeforbelgium.openinzichten.aspect;

import be.codeforbelgium.openinzichten.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    private RateLimitAspect rateLimitAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        rateLimitAspect = new RateLimitAspect();
        // Make these stubbings lenient as some tests throw before they are used
        lenient().when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("TestController.testMethod()");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rateLimit_unauthenticated_throwsUnauthorized() throws Throwable {
        // Arrange
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        RateLimit rateLimit = createRateLimit(3, 60);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthenticated requests cannot be rate limited.", exception.getReason());
        verify(proceedingJoinPoint, never()).proceed();
    }

    @Test
    void rateLimit_authenticationWithNullName_throwsUnauthorized() throws Throwable {
        // Arrange
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(null);

        RateLimit rateLimit = createRateLimit(3, 60);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthenticated requests cannot be rate limited.", exception.getReason());
        verify(proceedingJoinPoint, never()).proceed();
    }

    @Test
    void rateLimit_firstRequest_proceedsSuccessfully() throws Throwable {
        // Arrange
        setupAuthentication("user1");
        RateLimit rateLimit = createRateLimit(3, 60);
        Object expectedResult = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // Act
        Object result = rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Assert
        assertEquals(expectedResult, result);
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    void rateLimit_withinLimit_allRequestsSucceed() throws Throwable {
        // Arrange
        setupAuthentication("user2");
        RateLimit rateLimit = createRateLimit(3, 60);
        Object expectedResult = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // Act - Make 3 requests (at the limit)
        for (int i = 0; i < 3; i++) {
            Object result = rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
            assertEquals(expectedResult, result);
        }

        // Assert
        verify(proceedingJoinPoint, times(3)).proceed();
    }

    @Test
    void rateLimit_exceedsLimit_throwsTooManyRequests() throws Throwable {
        // Arrange
        setupAuthentication("user3");
        RateLimit rateLimit = createRateLimit(3, 60);
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - Make 3 successful requests
        for (int i = 0; i < 3; i++) {
            rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        }

        // Assert - 4th request should fail
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertEquals("Rate limit exceeded. Try again later.", exception.getReason());
        verify(proceedingJoinPoint, times(3)).proceed(); // Only the first 3 succeeded
    }

    @Test
    void rateLimit_differentUsers_isolatedLimits() throws Throwable {
        // Arrange
        RateLimit rateLimit = createRateLimit(2, 60);
        Object expectedResult = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // Act - User1 makes 2 requests
        setupAuthentication("user1");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // User2 makes 2 requests
        setupAuthentication("user2");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Assert - Both users should succeed (limits are isolated)
        verify(proceedingJoinPoint, times(4)).proceed();

        // User1's 3rd request should fail
        setupAuthentication("user1");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        // User2's 3rd request should also fail
        setupAuthentication("user2");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
    }

    @Test
    void rateLimit_differentEndpoints_separateLimits() throws Throwable {
        // Arrange
        setupAuthentication("user1");
        RateLimit rateLimit = createRateLimit(2, 60);
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - Make 2 requests to endpoint1
        when(signature.toShortString()).thenReturn("Controller.endpoint1()");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Make 2 requests to endpoint2
        when(signature.toShortString()).thenReturn("Controller.endpoint2()");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Assert - All 4 requests should succeed (different endpoints)
        verify(proceedingJoinPoint, times(4)).proceed();

        // 3rd request to endpoint1 should fail
        when(signature.toShortString()).thenReturn("Controller.endpoint1()");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        // 3rd request to endpoint2 should also fail
        when(signature.toShortString()).thenReturn("Controller.endpoint2()");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
    }

    @Test
    void rateLimit_timeWindowExpires_allowsNewRequests() throws Throwable {
        // Arrange
        setupAuthentication("user4");
        RateLimit rateLimit = createRateLimit(2, 1); // 2 requests per 1 second
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - Make 2 requests
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // 3rd request should fail
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        // Wait for the time window to expire and then assert a new request succeeds
        await().pollDelay(Duration.ofMillis(1100)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            // Ensure the async assertion thread has a SecurityContext
            setupAuthentication("user4");
            Object result = rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
            assertNotNull(result);
        });
        // Verify proceed was called three times total (two before, one after window)
        verify(proceedingJoinPoint, times(3)).proceed();
    }

    @Test
    void rateLimit_customLimitValues_respectsConfiguration() throws Throwable {
        // Arrange
        setupAuthentication("user5");
        RateLimit rateLimit = createRateLimit(5, 30); // 5 requests per 30 seconds
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - Make 5 requests (at the limit)
        for (int i = 0; i < 5; i++) {
            rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        }

        // Assert - 6th request should fail
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
        verify(proceedingJoinPoint, times(5)).proceed();
    }

    @Test
    void rateLimit_singleRequestLimit_allowsOnlyOne() throws Throwable {
        // Arrange
        setupAuthentication("user6");
        RateLimit rateLimit = createRateLimit(1, 60); // Only 1 request per minute
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - First request succeeds
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Assert - Second request fails immediately
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    void rateLimit_proceedThrowsException_exceptionPropagated() throws Throwable {
        // Arrange
        setupAuthentication("user7");
        RateLimit rateLimit = createRateLimit(3, 60);
        RuntimeException expectedException = new RuntimeException("Service error");
        when(proceedingJoinPoint.proceed()).thenThrow(expectedException);

        // Act & Assert
        RuntimeException actualException = assertThrows(RuntimeException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        assertEquals(expectedException, actualException);
        assertEquals("Service error", actualException.getMessage());
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    void rateLimit_multipleUsersSimultaneously_independentTracking() throws Throwable {
        // Arrange
        RateLimit rateLimit = createRateLimit(2, 60);
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act & Assert - User A makes 2 requests
        setupAuthentication("userA");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // User B makes 2 requests
        setupAuthentication("userB");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // User C makes 2 requests
        setupAuthentication("userC");
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // All should succeed
        verify(proceedingJoinPoint, times(6)).proceed();

        // Each user's 3rd request should fail
        setupAuthentication("userA");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        setupAuthentication("userB");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        setupAuthentication("userC");
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
    }

    @Test
    void rateLimit_slidingWindow_removesOldTimestamps() throws Throwable {
        // Arrange
        setupAuthentication("user8");
        RateLimit rateLimit = createRateLimit(2, 1); // 2 requests per 1 second
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - Make 2 requests at T0
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Wait 600ms (still within window) then assert 3rd request fails
        await().pollDelay(Duration.ofMillis(600)).atMost(Duration.ofMillis(800)).untilAsserted(() -> {
            setupAuthentication("user8");
            assertThrows(ResponseStatusException.class,
                    () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
        });

        // Wait another 1200ms (total >1s - window expired) and assert a new request succeeds
        await().pollDelay(Duration.ofMillis(1200)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            setupAuthentication("user8");
            Object result = rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
            assertNotNull(result);
        });

        verify(proceedingJoinPoint, times(3)).proceed();
    }

    @Test
    void rateLimit_zeroSecondsWindow_immediateExpiry() throws Throwable {
        // Arrange
        setupAuthentication("user9");
        RateLimit rateLimit = createRateLimit(1, 0); // 1 request per 0 seconds (immediate expiry)
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - First request
        rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);

        // Small delay to ensure timestamp difference; use Awaitility instead of Thread.sleep
        await().pollDelay(Duration.ofMillis(10)).atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            setupAuthentication("user9");
            rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        });

        // Assert
        verify(proceedingJoinPoint, times(2)).proceed();
    }

    @Test
    void rateLimit_highLimit_allowsManyRequests() throws Throwable {
        // Arrange
        setupAuthentication("user10");
        RateLimit rateLimit = createRateLimit(100, 60); // 100 requests per minute
        when(proceedingJoinPoint.proceed()).thenReturn(new Object());

        // Act - Make 100 requests
        for (int i = 0; i < 100; i++) {
            rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit);
        }

        // Assert - 101st request should fail
        assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));
        verify(proceedingJoinPoint, times(100)).proceed();
    }

    @Test
    void rateLimit_emptySecurityContext_throwsUnauthorized() throws Throwable {
        // Arrange
        SecurityContextHolder.clearContext();
        RateLimit rateLimit = createRateLimit(3, 60);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitAspect.rateLimit(proceedingJoinPoint, rateLimit));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthenticated requests cannot be rate limited.", exception.getReason());
        verify(proceedingJoinPoint, never()).proceed();
    }

    // Helper methods

    private void setupAuthentication(String username) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
    }

    private RateLimit createRateLimit(int requests, int perSeconds) {
        return new RateLimit() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }

            @Override
            public int requests() {
                return requests;
            }

            @Override
            public int perSeconds() {
                return perSeconds;
            }
        };
    }
}
