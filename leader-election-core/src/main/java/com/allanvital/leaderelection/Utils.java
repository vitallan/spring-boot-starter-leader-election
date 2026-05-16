package com.allanvital.leaderelection;

import java.time.Duration;
import java.util.Objects;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Utils {

    public static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    public static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

}
