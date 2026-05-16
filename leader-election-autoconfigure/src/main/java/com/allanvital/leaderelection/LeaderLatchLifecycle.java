package com.allanvital.leaderelection;

import org.springframework.context.SmartLifecycle;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class LeaderLatchLifecycle implements SmartLifecycle {

    private final DefaultLeaderLatch leaderLatch;

    private volatile boolean running;

    LeaderLatchLifecycle(DefaultLeaderLatch leaderLatch) {
        this.leaderLatch = leaderLatch;
    }

    @Override
    public void start() {
        leaderLatch.start();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        leaderLatch.stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; //last to start
    }

}
