package com.allanvital.leaderelection;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Allan Vital (https://allanvital.com)
 */
abstract class AbstractJpaLeaderLeaseStoreIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String lockName;
    private final String NODE_A = "node-a";
    private final String NODE_B = "node-b";

    @BeforeEach
    public void setup() {
        lockName = "lock-" + UUID.randomUUID();
    }

    @Test
    public void acquiresThenBlocksOtherOwnerUntilExpiry() throws Exception {
        JpaLeaderLeaseStore store = new JpaLeaderLeaseStore(entityManager);

        LeaseOperationResult firstAcquire = execInTransaction(() -> store.acquire(getId(NODE_A)));
        LeaseOperationResult blockedAcquire = execInTransaction(() -> store.acquire(getId(NODE_B)));

        waitUntil(() -> execInTransaction(() -> store.acquire(getId(NODE_B))) == LeaseOperationResult.ACQUIRED,
                Duration.ofSeconds(10));

        assertThat(firstAcquire).isEqualTo(LeaseOperationResult.ACQUIRED);
        assertThat(blockedAcquire).isEqualTo(LeaseOperationResult.HELD_BY_OTHER);
    }

    @Test
    public void renewRequiresCurrentOwner() {
        JpaLeaderLeaseStore store = new JpaLeaderLeaseStore(entityManager);

        LeaseOperationResult acquire = execInTransaction(() -> store.acquire(getId(NODE_A)));
        LeaseOperationResult renewByOwner = execInTransaction(() -> store.renew(getId(NODE_A)));
        LeaseOperationResult renewByOther = execInTransaction(() -> store.renew(getId(NODE_B)));

        assertThat(acquire).isEqualTo(LeaseOperationResult.ACQUIRED);
        assertThat(renewByOwner).isEqualTo(LeaseOperationResult.RENEWED);
        assertThat(renewByOther).isEqualTo(LeaseOperationResult.NOT_OWNER);
    }

    private LeaseOperationResult execInTransaction(Supplier<LeaseOperationResult> operation) {
        return transactionTemplate.execute(status -> operation.get());
    }

    private LeaseIdentity getId(String owner) {
        return new LeaseIdentity(lockName, owner, Duration.ofSeconds(2));
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met before timeout");
            }
            Thread.sleep(50L);
        }
    }

}
