package com.allanvital.leaderelection;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "leader.election")

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LeaderElectionProperties {

    private boolean enabled = true;
    private String lockName = "default";
    private String ownerId;
    private Duration leaseDuration = Duration.ofSeconds(15);
    private Duration renewInterval = Duration.ofSeconds(5);
    private Duration acquireInterval = Duration.ofSeconds(2);
    private Duration lockTimeout = Duration.ofSeconds(2);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Duration leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public Duration getRenewInterval() {
        return renewInterval;
    }

    public void setRenewInterval(Duration renewInterval) {
        this.renewInterval = renewInterval;
    }

    public Duration getAcquireInterval() {
        return acquireInterval;
    }

    public void setAcquireInterval(Duration acquireInterval) {
        this.acquireInterval = acquireInterval;
    }

    public Duration getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(Duration lockTimeout) {
        this.lockTimeout = lockTimeout;
    }
}
