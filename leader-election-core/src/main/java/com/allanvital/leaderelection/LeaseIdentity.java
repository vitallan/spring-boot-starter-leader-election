package com.allanvital.leaderelection;


import java.time.Duration;

import static com.allanvital.leaderelection.Utils.requirePositive;
import static com.allanvital.leaderelection.Utils.requireText;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record LeaseIdentity(String lockName, String ownerId, Duration leaseDuration) {

    public LeaseIdentity {
        requireText(lockName, "lockName");
        requireText(ownerId, "ownerId");
        requirePositive(leaseDuration, "leaseDuration");
    }

}
