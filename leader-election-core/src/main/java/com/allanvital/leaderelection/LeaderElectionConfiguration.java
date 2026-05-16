package com.allanvital.leaderelection;


import java.time.Duration;
import java.util.Objects;

import static com.allanvital.leaderelection.Utils.requirePositive;
import static com.allanvital.leaderelection.Utils.requireText;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record LeaderElectionConfiguration(
        String lockName,
        String ownerId,
        Duration leaseDuration,
        Duration renewInterval,
        Duration acquireInterval
) {

    public LeaderElectionConfiguration {
        requireText(lockName, "lockName");
        requireText(ownerId, "ownerId");
        requirePositive(leaseDuration, "leaseDuration");
        requirePositive(renewInterval, "renewInterval");
        requirePositive(acquireInterval, "acquireInterval");
        if (!renewInterval.minus(leaseDuration).isNegative()) {
            throw new IllegalArgumentException("renewInterval must be less than leaseDuration");
        }
    }

}
