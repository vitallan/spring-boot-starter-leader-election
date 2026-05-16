package com.allanvital.leaderelection;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class TestLeaderLeaseStore implements LeaderLeaseStore {

    private final Queue<LeaseOperationResult> acquireResults = new ConcurrentLinkedQueue<>();
    private final Queue<LeaseOperationResult> renewResults = new ConcurrentLinkedQueue<>();
    private final AtomicInteger acquireCalls = new AtomicInteger();
    private final AtomicInteger renewCalls = new AtomicInteger();

    public void enqueueAcquireResult(LeaseOperationResult result) {
        acquireResults.add(result);
    }

    public void enqueueAcquireResults(LeaseOperationResult... results) {
        for (LeaseOperationResult result : results) {
            this.enqueueAcquireResult(result);
        }
    }

    public void enqueueRenewResult(LeaseOperationResult result) {
        renewResults.add(result);
    }

    public void enqueueRenewResults(LeaseOperationResult... results) {
        for (LeaseOperationResult result : results) {
            this.enqueueRenewResult(result);
        }
    }

    public int acquireCalls() {
        return acquireCalls.get();
    }

    public int renewCalls() {
        return renewCalls.get();
    }

    @Override
    public LeaseOperationResult acquire(LeaseIdentity leaseIdentity) {
        acquireCalls.incrementAndGet();
        return next(acquireResults, LeaseOperationResult.HELD_BY_OTHER);
    }

    @Override
    public LeaseOperationResult renew(LeaseIdentity leaseIdentity) {
        renewCalls.incrementAndGet();
        return next(renewResults, LeaseOperationResult.NOT_OWNER);
    }

    private static LeaseOperationResult next(Queue<LeaseOperationResult> queue, LeaseOperationResult fallback) {
        LeaseOperationResult result = queue.poll();
        return result != null ? result : fallback;
    }
}
