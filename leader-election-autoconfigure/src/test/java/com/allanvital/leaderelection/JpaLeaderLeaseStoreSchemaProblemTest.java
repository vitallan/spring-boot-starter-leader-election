package com.allanvital.leaderelection;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class JpaLeaderLeaseStoreSchemaProblemTest {

    @Test
    void detectsSchemaProblemInExceptionMessage() {
        LeaderLeaseSchemaProblemReporter reporter = new LeaderLeaseSchemaProblemReporter();
        RuntimeException ex = new RuntimeException("relation leader_lock does not exist");
        assertThat(reporter.isSchemaProblem(ex)).isTrue();
    }

    @Test
    void logsSchemaProblemOnlyOnceAndKeepsReturningFailed() {
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.createQuery("select current_timestamp")).thenThrow(
                new RuntimeException("relation leader_lock does not exist")
        );

        Logger logger = (Logger) LoggerFactory.getLogger(LeaderLeaseSchemaProblemReporter.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.ERROR);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            JpaLeaderLeaseStore store = new JpaLeaderLeaseStore(entityManager);
            LeaseIdentity id = new LeaseIdentity("default", "node-1", Duration.ofSeconds(2));

            assertThat(store.acquire(id)).isEqualTo(LeaseOperationResult.FAILED);
            assertThat(store.acquire(id)).isEqualTo(LeaseOperationResult.FAILED);

            long errorCount = appender.list.stream().filter(e -> e.getLevel() == Level.ERROR).count();
            assertThat(errorCount).isEqualTo(1);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains("Leader election table");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        }
    }
}
