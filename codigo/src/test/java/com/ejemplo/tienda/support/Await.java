package com.ejemplo.tienda.support;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Waits, with a time limit, until something happens on another thread. */
public final class Await {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private Await() {
    }

    public static void until(String description, BooleanSupplier condition) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting: " + description, error);
            }
        }
        if (!condition.getAsBoolean()) {
            throw new AssertionError("Timed out after " + TIMEOUT.toSeconds() + "s waiting: " + description);
        }
    }
}
