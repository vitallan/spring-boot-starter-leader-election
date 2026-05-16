package com.allanvital.leaderelection.sample;

import com.allanvital.leaderelection.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Allan Vital (https://allanvital.com)
 */
@Component
public class LeaderStatusLogger {

    private static final Logger log = LoggerFactory.getLogger(LeaderStatusLogger.class);

    private final LeaderLatch leaderLatch;
    private final String lockName;
    private final String ownerId;

    public LeaderStatusLogger(
            LeaderLatch leaderLatch,
            @Value("${leader.election.lock-name:default}") String lockName,
            @Value("${leader.election.owner-id:}") String ownerId
    ) {
        this.leaderLatch = leaderLatch;
        this.lockName = lockName;
        this.ownerId = ownerId;
    }

    @Scheduled(fixedDelayString = "PT2S")
    public void logLeaderStatus() {
        log.info("leader status lockName={} ownerId={} leader={}", lockName, ownerId, leaderLatch.isLeader());
    }

}
