package com.allanvital.leaderelection.sample;

import com.allanvital.leaderelection.LeaderLatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Allan Vital (https://allanvital.com)
 */
@RestController
public class LeaderController {

    private final LeaderLatch leaderLatch;
    private final String lockName;
    private final String ownerId;

    public LeaderController(
            LeaderLatch leaderLatch,
            @Value("${leader.election.lock-name:default}") String lockName,
            @Value("${leader.election.owner-id:}") String ownerId
    ) {
        this.leaderLatch = leaderLatch;
        this.lockName = lockName;
        this.ownerId = ownerId;
    }

    @GetMapping("/leader")
    public LeaderStatus leader() {
        return new LeaderStatus(leaderLatch.isLeader(), lockName, ownerId);
    }
}
