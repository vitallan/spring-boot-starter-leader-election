package com.allanvital.leaderelection;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class ThrowingLeaderLeaseStore implements LeaderLeaseStore {

    private final TestLeaderLeaseStore delegate = new TestLeaderLeaseStore();
    private final AtomicInteger acquireCalls = new AtomicInteger();
    private final AtomicInteger renewCalls = new AtomicInteger();
    private final boolean throwOnFirstAcquire;
    private final boolean throwOnFirstRenew;

    private ThrowingLeaderLeaseStore(boolean throwOnFirstAcquire, boolean throwOnFirstRenew) {
        this.throwOnFirstAcquire = throwOnFirstAcquire;
        this.throwOnFirstRenew = throwOnFirstRenew;
    }

    public static ThrowingLeaderLeaseStore throwOnFirstAcquireThen(LeaseOperationResult... nextAcquireResults) {
        ThrowingLeaderLeaseStore store = new ThrowingLeaderLeaseStore(true, false);
        store.delegate.enqueueAcquireResults(nextAcquireResults);
        return store;
    }

    public static ThrowingLeaderLeaseStore throwOnFirstRenewThen(LeaseOperationResult... nextAcquireResults) {
        ThrowingLeaderLeaseStore store = new ThrowingLeaderLeaseStore(false, true);
        store.delegate.enqueueAcquireResult(LeaseOperationResult.ACQUIRED);
        store.delegate.enqueueAcquireResults(nextAcquireResults);
        return store;
    }

    public int acquireCalls() {
        return acquireCalls.get();
    }

    public int renewCalls() {
        return renewCalls.get();
    }

    @Override
    public LeaseOperationResult acquire(LeaseIdentity leaseIdentity) {
        int current = acquireCalls.incrementAndGet();
        if (throwOnFirstAcquire && current == 1) {
            throw new RuntimeException("simulated acquire failure");
        }
        return delegate.acquire(leaseIdentity);
    }

    @Override
    public LeaseOperationResult renew(LeaseIdentity leaseIdentity) {
        int current = renewCalls.incrementAndGet();
        if (throwOnFirstRenew && current == 1) {
            throw new RuntimeException("simulated renew failure");
        }
        return delegate.renew(leaseIdentity);
    }
}
