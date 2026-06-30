package hu.szegedibibliaszol.scraper.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRateLimiterTest {

    @Test
    void acquireDoesNotFailWhenDelayIsNegative() {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(-50);

        assertDoesNotThrow(rateLimiter::acquire);
    }

    @Test
    void acquireThrowsIllegalStateWhenThreadIsInterruptedDuringWait() {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(1_000);
        rateLimiter.setLastRequestAt(System.currentTimeMillis());

        Thread.currentThread().interrupt();
        Throwable throwable = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, rateLimiter::acquire);

        assertInstanceOf(InterruptedException.class, throwable.getCause());
        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(Thread.interrupted());
    }

    @Test
    void acquireWaitsWhenTheMinimumDelayHasNotElapsedYet() {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(30);
        rateLimiter.setLastRequestAt(System.currentTimeMillis());

        assertDoesNotThrow(rateLimiter::acquire);

        assertTrue(rateLimiter.lastRequestAt() > 0);
    }
}

