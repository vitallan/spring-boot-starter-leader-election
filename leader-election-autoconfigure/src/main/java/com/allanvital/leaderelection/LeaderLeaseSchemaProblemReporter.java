package com.allanvital.leaderelection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class LeaderLeaseSchemaProblemReporter {

    private static final Logger log = LoggerFactory.getLogger(LeaderLeaseSchemaProblemReporter.class);
    private static final String TABLE_NAME = "leader_lock";

    private final AtomicBoolean logged = new AtomicBoolean(false);

    public void logProblemOnce(Throwable ex, String lockName, String ownerId) {
        if (!logged.compareAndSet(false, true)) {
            return;
        }

        log.error(
                "Leader election table '{}' is missing or not accessible. This instance will remain follower (leader election disabled). " +
                        "Create the table via migrations, or allow Hibernate to create it (spring.jpa.hibernate.ddl-auto=create|update). " +
                        "lockName={} ownerId={}",
                TABLE_NAME,
                lockName,
                ownerId,
                ex
        );
    }

    public boolean isSchemaProblem(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains(TABLE_NAME)) {
                    //trying to get the possible errors related to the table
                    if (lower.contains("does not exist")
                            || lower.contains("doesn't exist")
                            || lower.contains("unknown table")
                            || lower.contains("invalid object name")
                            || lower.contains("permission denied")
                            || lower.contains("not authorized")
                            || lower.contains("access denied")) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
