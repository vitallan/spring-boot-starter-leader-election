package com.allanvital.leaderelection;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.allanvital.leaderelection.LeaseOperationResult.*;
import static com.allanvital.leaderelection.TestUtils.waitMaxTwoSeconds;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class DefaultLeaderLatchTest {

    private DefaultLeaderLatch latch;
    private TestLeaderLeaseStore store;
    private LeaderElectionConfiguration defaultConf;

    @BeforeEach
    public void setup() {
        store = new TestLeaderLeaseStore();
        defaultConf = new LeaderElectionConfiguration(
                "default",
                "node-1",
                Duration.ofSeconds(2),
                Duration.ofMillis(100),
                Duration.ofMillis(100)
        );
    }

    @Test
    public void acquiresLeadershipAndRenews() throws Exception {
        store.enqueueAcquireResult(ACQUIRED);
        store.enqueueRenewResults(RENEWED, RENEWED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> latch.isLeader());
        waitMaxTwoSeconds(() -> store.renewCalls() > 0);

        assertTrue(latch.isLeader());
    }

    @Test
    public void losesLeadershipWhenRenewFails() throws Exception {
        store.enqueueAcquireResult(ACQUIRED);
        store.enqueueRenewResults(RENEWED, NOT_OWNER);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> store.renewCalls() > 1);
        waitMaxTwoSeconds(() -> !latch.isLeader());

        assertFalse(latch.isLeader());
    }

    @Test
    public void staysFollowerWhenAcquireIsHeldByOther() throws Exception {
        store.enqueueAcquireResults(HELD_BY_OTHER, HELD_BY_OTHER, HELD_BY_OTHER);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> store.acquireCalls() > 1);

        assertFalse(latch.isLeader());
        assertEquals(0, store.renewCalls());
    }

    @Test
    public void eventuallyBecomesLeaderAfterTransientAcquireFailures() throws Exception {
        store.enqueueAcquireResults(FAILED, HELD_BY_OTHER, ACQUIRED);
        store.enqueueRenewResult(RENEWED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> latch.isLeader());
        waitMaxTwoSeconds(() -> store.renewCalls() > 0);

        assertTrue(latch.isLeader());
    }

    @Test
    public void reAcquiresLeadershipAfterLosingIt() throws Exception {
        store.enqueueAcquireResults(ACQUIRED, ACQUIRED);
        store.enqueueRenewResult(NOT_OWNER);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> store.renewCalls() > 0);
        waitMaxTwoSeconds(() -> !latch.isLeader());
        waitMaxTwoSeconds(() -> latch.isLeader());

        assertTrue(latch.isLeader());
        assertTrue(store.acquireCalls() >= 2);
    }

    @Test
    public void stopAlwaysResetsLeadershipFlag() throws Exception {
        store.enqueueAcquireResult(ACQUIRED);
        store.enqueueRenewResult(RENEWED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> latch.isLeader());
        latch.stop();

        assertFalse(latch.isLeader());
    }

    @Test
    public void startIsIdempotentAndDoesNotCreateParallelLoops() throws Exception {
        store.enqueueAcquireResult(ACQUIRED);
        store.enqueueRenewResults(RENEWED, RENEWED, RENEWED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();
        latch.start();

        waitMaxTwoSeconds(() -> latch.isLeader());
        waitMaxTwoSeconds(() -> store.renewCalls() > 1);

        assertEquals(1, store.acquireCalls());
    }

    @Test
    public void stopBeforeStartIsSafeAndKeepsFollowerState() {
        latch = new DefaultLeaderLatch(defaultConf, store);

        latch.stop();

        assertFalse(latch.isLeader());
        assertEquals(0, store.acquireCalls());
        assertEquals(0, store.renewCalls());
    }

    @Test
    public void closeIsEquivalentToStopAndIsIdempotent() throws Exception {
        store.enqueueAcquireResult(ACQUIRED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();
        waitMaxTwoSeconds(() -> latch.isLeader());

        latch.close();
        latch.close();

        assertFalse(latch.isLeader());
    }

    @Test
    public void becomesFollowerWhenAcquireThrowsAndRetries() throws Exception {
        ThrowingLeaderLeaseStore throwingStore = ThrowingLeaderLeaseStore.throwOnFirstAcquireThen(ACQUIRED);
        latch = new DefaultLeaderLatch(defaultConf, throwingStore);

        latch.start();

        waitMaxTwoSeconds(() -> throwingStore.acquireCalls() > 1);
        waitMaxTwoSeconds(() -> latch.isLeader());

        assertTrue(latch.isLeader());
    }

    @Test
    public void becomesFollowerWhenRenewThrowsAndThenReacquires() throws Exception {
        ThrowingLeaderLeaseStore throwingStore = ThrowingLeaderLeaseStore.throwOnFirstRenewThen(ACQUIRED, ACQUIRED);
        latch = new DefaultLeaderLatch(defaultConf, throwingStore);

        latch.start();

        waitMaxTwoSeconds(() -> throwingStore.renewCalls() > 0);
        waitMaxTwoSeconds(() -> !latch.isLeader());
        waitMaxTwoSeconds(() -> latch.isLeader());

        assertTrue(latch.isLeader());
        assertTrue(throwingStore.acquireCalls() >= 2);
    }

    @Test
    public void acquireReturningRenewedAlsoMarksLeader() throws Exception {
        store.enqueueAcquireResult(RENEWED);
        store.enqueueRenewResult(RENEWED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> latch.isLeader());
        waitMaxTwoSeconds(() -> store.renewCalls() > 0);

        assertTrue(latch.isLeader());
    }

    @Test
    public void whileLeaderDoesNotCallAcquire() throws Exception {
        store.enqueueAcquireResult(ACQUIRED);
        store.enqueueRenewResults(RENEWED, RENEWED);

        latch = new DefaultLeaderLatch(defaultConf, store);
        latch.start();

        waitMaxTwoSeconds(() -> store.renewCalls() > 1);

        assertEquals(1, store.acquireCalls());
    }

    @AfterEach
    public void tearDown() {
        if (latch != null) {
            latch.stop();
            latch.close();
        }
    }

}
