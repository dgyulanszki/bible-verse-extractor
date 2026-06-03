package hu.szegedibibliaszol.scraper.support;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRateLimiterTest {

    @Test
    void acquireDoesNotFailWhenDelayIsNegative() {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(-50);

        assertDoesNotThrow(rateLimiter::acquire);
    }

    @Test
    void acquireThrowsIllegalStateWhenThreadIsInterruptedDuringWait() throws Exception {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(1_000);
        Field lastRequestAtField = SimpleRateLimiter.class.getDeclaredField("lastRequestAt");
        lastRequestAtField.setAccessible(true);
        lastRequestAtField.setLong(rateLimiter, System.currentTimeMillis());

        Thread.currentThread().interrupt();
        Throwable throwable = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, rateLimiter::acquire);

        assertInstanceOf(InterruptedException.class, throwable.getCause());
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    @Test
    void acquireWaitsWhenTheMinimumDelayHasNotElapsedYet() throws Exception {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(30);
        Field lastRequestAtField = SimpleRateLimiter.class.getDeclaredField("lastRequestAt");
        lastRequestAtField.setAccessible(true);
        lastRequestAtField.setLong(rateLimiter, System.currentTimeMillis());

        assertDoesNotThrow(rateLimiter::acquire);

        Object lastRequestAt = lastRequestAtField.get(rateLimiter);
        assertNotNull(lastRequestAt);
        assertTrue(((Long) lastRequestAt) > 0);
    }
}

