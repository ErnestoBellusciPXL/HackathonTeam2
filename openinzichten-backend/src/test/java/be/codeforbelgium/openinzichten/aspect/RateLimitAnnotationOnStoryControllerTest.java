package be.codeforbelgium.openinzichten.aspect;

import be.codeforbelgium.openinzichten.annotation.RateLimit;
import be.codeforbelgium.openinzichten.api.controllers.StoryController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RateLimitAnnotationOnStoryControllerTest {

    @Test
    void reformatEndpoint_hasExpectedRateLimitAnnotation() throws NoSuchMethodException {
        Method method = StoryController.class.getDeclaredMethod("reformatStory", be.codeforbelgium.openinzichten.api.request.StoryReformatRequest.class);
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        assertNotNull(rateLimit, "RateLimit annotation must be present on /ai/reformat endpoint");
        assertEquals(3, rateLimit.requests(), "Expected 3 requests allowed");
        assertEquals(3600, rateLimit.perSeconds(), "Expected window of 3600 seconds (1 hour)");
    }
}

