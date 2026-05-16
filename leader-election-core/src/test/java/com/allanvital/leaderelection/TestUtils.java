package com.allanvital.leaderelection;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestUtils {

    public static void waitMaxTwoSeconds(BooleanSupplier condition) throws Exception {
        waitUntil(condition, Duration.ofSeconds(2));
    }

    private static void waitUntil(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met before timeout");
            }
            Thread.sleep(20L);
        }
    }

}
