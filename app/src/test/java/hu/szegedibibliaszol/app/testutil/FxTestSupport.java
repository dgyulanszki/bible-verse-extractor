package hu.szegedibibliaszol.app.testutil;

import javafx.application.Platform;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public final class FxTestSupport {

    private static volatile boolean initialized;

    private FxTestSupport() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (FxTestSupport.class) {
            if (initialized) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    latch.countDown();
                });
            } catch (IllegalStateException ex) {
                Platform.setImplicitExit(false);
                latch.countDown();
            }
            awaitLatch(latch);
            initialized = true;
        }
    }

    public static <T> T runOnFxThread(Callable<T> callable) {
        initialize();
        if (Platform.isFxApplicationThread()) {
            return callUnchecked(callable);
        }
        FutureTask<T> futureTask = new FutureTask<>(callable);
        Platform.runLater(futureTask);
        try {
            return futureTask.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("JavaFX test task failed.", ex);
        }
    }

    public static void runOnFxThread(Runnable runnable) {
        runOnFxThread(() -> {
            runnable.run();
            return null;
        });
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX runtime did not start in time.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX runtime.", ex);
        }
    }

    private static <T> T callUnchecked(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception ex) {
            throw new IllegalStateException("JavaFX test task failed.", ex);
        }
    }
}


