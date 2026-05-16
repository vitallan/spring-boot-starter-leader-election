package com.allanvital.leaderelection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class DefaultLeaderLatchLog {

    private static final Logger log = LoggerFactory.getLogger(DefaultLeaderLatch.class);

    void starting(LeaderElectionConfiguration configuration) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(
                "Leader latch starting lockName={} ownerId={} leaseDuration={} renewInterval={} acquireInterval={}",
                configuration.lockName(),
                configuration.ownerId(),
                configuration.leaseDuration(),
                configuration.renewInterval(),
                configuration.acquireInterval()
        );
    }

    void stopped(LeaderElectionConfiguration configuration) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(
                "Leader latch stopped lockName={} ownerId={}",
                configuration.lockName(),
                configuration.ownerId()
        );
    }

    void acquired(LeaderElectionConfiguration configuration, LeaseOperationResult result) {
        log.info(
                "Leader latch acquired leadership lockName={} ownerId={} result={}",
                configuration.lockName(),
                configuration.ownerId(),
                result
        );
    }

    void lost(LeaderElectionConfiguration configuration, LeaseOperationResult result) {
        log.info(
                "Leader latch lost leadership lockName={} ownerId={} result={}",
                configuration.lockName(),
                configuration.ownerId(),
                result
        );
    }

    void cycleFailed(LeaderElectionConfiguration configuration, boolean wasLeader, RuntimeException ex) {
        if (wasLeader) {
            log.info(
                    "Leader latch cycle failed; leadership lost lockName={} ownerId={}",
                    configuration.lockName(),
                    configuration.ownerId(),
                    ex
            );
            return;
        }

        log.debug(
                "Leader latch cycle failed lockName={} ownerId={}",
                configuration.lockName(),
                configuration.ownerId(),
                ex
        );
    }
}
