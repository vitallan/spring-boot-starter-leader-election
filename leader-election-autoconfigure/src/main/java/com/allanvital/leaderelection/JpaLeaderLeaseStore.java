package com.allanvital.leaderelection;


import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class JpaLeaderLeaseStore implements LeaderLeaseStore {

    private static final Logger log = LoggerFactory.getLogger(JpaLeaderLeaseStore.class);

    private final EntityManager entityManager;
    private final LeaderLeaseSchemaProblemReporter schemaProblemReporter = new LeaderLeaseSchemaProblemReporter();

    public JpaLeaderLeaseStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public LeaseOperationResult acquire(LeaseIdentity leaseIdentity) {
        String lockName = leaseIdentity.lockName();
        String ownerId = leaseIdentity.ownerId();
        try {
            Instant dbNow = currentDbTime();
            LeaderLockEntity lock = entityManager.find(LeaderLockEntity.class, lockName, LockModeType.PESSIMISTIC_WRITE);
            if (lock == null) {
                LeaseOperationResult result = tryInsert(lockName, ownerId, dbNow.plus(leaseIdentity.leaseDuration()), dbNow);
                if (log.isDebugEnabled() && result != LeaseOperationResult.RENEWED) {
                    log.debug("leader lease acquire result={} lockName={} ownerId={}", result, lockName, ownerId);
                }
                return result;
            }

            String previousOwnerId = lock.getOwnerId();
            if (ownerId.equals(lock.getOwnerId()) || !lock.getLeaseUntil().isAfter(dbNow)) {
                lock.setOwnerId(ownerId);
                lock.setLeaseUntil(dbNow.plus(leaseIdentity.leaseDuration()));
                lock.setUpdatedAt(dbNow);
                LeaseOperationResult result = ownerId.equals(previousOwnerId) ? LeaseOperationResult.RENEWED : LeaseOperationResult.ACQUIRED;
                if (result == LeaseOperationResult.ACQUIRED) {
                    log.debug("leader lease acquired lockName={} ownerId={}", lockName, ownerId);
                } else {
                    log.debug("leader lease renewed via acquire lockName={} ownerId={}", lockName, ownerId);
                }
                return result;
            }

            log.debug("leader lease held by other lockName={} ownerId={} currentOwner={}", lockName, ownerId, lock.getOwnerId());
            return LeaseOperationResult.HELD_BY_OTHER;
        } catch (RuntimeException ex) {
            if (schemaProblemReporter.isSchemaProblem(ex)) {
                schemaProblemReporter.logProblemOnce(ex, lockName, ownerId);
            }
            log.debug("leader lease acquire failed lockName={} ownerId={}", lockName, ownerId, ex);
            return LeaseOperationResult.FAILED;
        }
    }

    @Override
    @Transactional
    public LeaseOperationResult renew(LeaseIdentity leaseIdentity) {
        String lockName = leaseIdentity.lockName();
        String ownerId = leaseIdentity.ownerId();
        try {
            Instant dbNow = currentDbTime();
            LeaderLockEntity lock = entityManager.find(LeaderLockEntity.class, lockName, LockModeType.PESSIMISTIC_WRITE);
            if (lock == null) {
                log.debug("leader lease renew not-owner (no row) lockName={} ownerId={}", lockName, ownerId);
                return LeaseOperationResult.NOT_OWNER;
            }

            if (!ownerId.equals(lock.getOwnerId())) {
                log.debug("leader lease renew not-owner lockName={} ownerId={} currentOwner={}", lockName, ownerId, lock.getOwnerId());
                return LeaseOperationResult.NOT_OWNER;
            }

            lock.setLeaseUntil(dbNow.plus(leaseIdentity.leaseDuration()));
            lock.setUpdatedAt(dbNow);
            log.debug("leader lease renewed lockName={} ownerId={}", lockName, ownerId);
            return LeaseOperationResult.RENEWED;
        } catch (RuntimeException ex) {
            if (schemaProblemReporter.isSchemaProblem(ex)) {
                schemaProblemReporter.logProblemOnce(ex, lockName, ownerId);
            }
            log.debug("leader lease renew failed lockName={} ownerId={}", lockName, ownerId, ex);
            return LeaseOperationResult.FAILED;
        }
    }

    private LeaseOperationResult tryInsert(String lockName, String ownerId, Instant leaseUntil, Instant dbNow) {
        try {
            LeaderLockEntity lock = new LeaderLockEntity();
            lock.setLockName(lockName);
            lock.setOwnerId(ownerId);
            lock.setLeaseUntil(leaseUntil);
            lock.setUpdatedAt(dbNow);
            entityManager.persist(lock);
            entityManager.flush();
            log.debug("leader lease inserted lockName={} ownerId={}", lockName, ownerId);
            return LeaseOperationResult.ACQUIRED;
        } catch (DataIntegrityViolationException | JpaSystemException | PersistenceException ex) {
            log.debug("leader lease insert contention lockName={} ownerId={}", lockName, ownerId, ex);
            return LeaseOperationResult.HELD_BY_OTHER;
        }
    }

    private Instant currentDbTime() {
        Object value = entityManager.createQuery("select current_timestamp").getSingleResult();
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return Instant.parse(value.toString());
    }

}
