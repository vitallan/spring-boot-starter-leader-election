package com.allanvital.leaderelection.sample;

import com.allanvital.leaderelection.LeaderLatch;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
@Testcontainers(disabledWithoutDocker = true)
class LeaderElectionSampleMySqlIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test
    void electsSingleLeader_noChurn_thenFailsOver() throws Exception {
        SampleAppInstance nodeA = null;
        SampleAppInstance nodeB = null;
        try {
            nodeA = SampleAppInstance.start(mysql, "node-a", "sample-lock");
            nodeB = SampleAppInstance.start(mysql, "node-b", "sample-lock");

            LeaderLatch latchA = nodeA.leaderLatch();
            LeaderLatch latchB = nodeB.leaderLatch();

            waitUntil("leader elected", Duration.ofSeconds(20), () -> exactlyOneLeader(latchA, latchB));
            boolean aIsLeader = latchA.isLeader();

            assertNoChurn(Duration.ofSeconds(20), latchA, latchB, aIsLeader);

            if (aIsLeader) {
                nodeA.close();
                nodeA = null;
                waitUntil("failover to node-b", Duration.ofSeconds(20), latchB::isLeader);
            } else {
                nodeB.close();
                nodeB = null;
                waitUntil("failover to node-a", Duration.ofSeconds(20), latchA::isLeader);
            }

            waitUntil("post-failover stable leader", Duration.ofSeconds(5), () -> exactlyOneLeader(latchA, latchB));
        } finally {
            if (nodeA != null) {
                nodeA.close();
            }
            if (nodeB != null) {
                nodeB.close();
            }
        }
    }

    private static boolean exactlyOneLeader(LeaderLatch a, LeaderLatch b) {
        return a.isLeader() ^ b.isLeader();
    }

    private static void assertNoChurn(Duration duration, LeaderLatch latchA, LeaderLatch latchB, boolean aIsLeaderAtStart)
            throws Exception {
        long deadline = System.nanoTime() + duration.toNanos();
        while (System.nanoTime() < deadline) {
            boolean nowA = latchA.isLeader();
            boolean nowB = latchB.isLeader();
            if (nowA == nowB) {
                fail("expected exactly one leader during steady state, but got: a=" + nowA + " b=" + nowB);
            }
            if (nowA != aIsLeaderAtStart) {
                fail("leader churn detected: initialLeader=" + (aIsLeaderAtStart ? "node-a" : "node-b") +
                        " nowLeader=" + (nowA ? "node-a" : "node-b"));
            }
            Thread.sleep(200L);
        }
    }

    private static void waitUntil(String description, Duration timeout, BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("timeout waiting for condition: " + description);
            }
            Thread.sleep(50L);
        }
    }
}
