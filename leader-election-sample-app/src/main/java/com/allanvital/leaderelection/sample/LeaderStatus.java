package com.allanvital.leaderelection.sample;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record LeaderStatus(boolean leader, String lockName, String ownerId) {
}
