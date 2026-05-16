package com.allanvital.leaderelection;


import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public final class DefaultLeaderLatch implements LeaderLatch, AutoCloseable {

    private final DefaultLeaderLatchLog log = new DefaultLeaderLatchLog();

    private final LeaderElectionConfiguration configuration;
    private final LeaseIdentity leaseIdentity;
    private final LeaderLeaseStore leaseStore;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean leader = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DefaultLeaderLatch(LeaderElectionConfiguration configuration, LeaderLeaseStore leaseStore) {
        this(configuration, leaseStore, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "leader-election-loop");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public DefaultLeaderLatch(LeaderElectionConfiguration configuration, LeaderLeaseStore leaseStore, ScheduledExecutorService scheduler) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.leaseIdentity = new LeaseIdentity(
                this.configuration.lockName(),
                this.configuration.ownerId(),
                this.configuration.leaseDuration()
        );
        this.leaseStore = Objects.requireNonNull(leaseStore, "leaseStore must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        running.set(true);
        log.starting(configuration);
        scheduleNext(0L);
    }

    public void stop() {
        running.set(false);
        leader.set(false);
        scheduler.shutdownNow();
        log.stopped(configuration);
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public boolean isLeader() {
        return leader.get();
    }

    private void scheduleNext(long delayMs) {
        if (!running.get()) {
            return;
        }
        scheduler.schedule(this::runCycle, delayMs, TimeUnit.MILLISECONDS);
    }

    private void runCycle() {
        if (!running.get()) {
            return;
        }

        boolean wasLeader = leader.get();
        LeaseOperationResult result = null;
        try {
            if (wasLeader) {
                result = leaseStore.renew(leaseIdentity);
                leader.set(result == LeaseOperationResult.RENEWED);
            } else {
                result = leaseStore.acquire(leaseIdentity);
                boolean isLeaderNow = result == LeaseOperationResult.ACQUIRED || result == LeaseOperationResult.RENEWED;
                leader.set(isLeaderNow);
            }
        } catch (RuntimeException ex) {
            leader.set(false);
            log.cycleFailed(configuration, wasLeader, ex);
        }

        boolean isLeaderNow = leader.get();
        if (!wasLeader && isLeaderNow) {
            log.acquired(configuration, result);
        } else if (wasLeader && !isLeaderNow) {
            log.lost(configuration, result);
        }
        long nextMs;
        if (isLeaderNow) {
            nextMs = configuration.renewInterval().toMillis();
        } else {
            nextMs = configuration.acquireInterval().toMillis();
        }
        scheduleNext(nextMs);
    }

}
