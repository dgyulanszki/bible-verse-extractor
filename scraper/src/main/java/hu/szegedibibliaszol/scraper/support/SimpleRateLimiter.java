package hu.szegedibibliaszol.scraper.support;

public class SimpleRateLimiter {

    private final long minDelayMillis;
    private long lastRequestAt;

    public SimpleRateLimiter(long minDelayMillis) {
        this.minDelayMillis = Math.max(0, minDelayMillis);
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long waitTime = (lastRequestAt + minDelayMillis) - now;
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Rate limiter interrupted while waiting.", ex);
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }
}

