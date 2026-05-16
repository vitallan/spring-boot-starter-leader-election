# Spring Boot Starter Leader Election

Database-backed leader election starter for Spring Boot applications.

The starter exposes a simple `LeaderLatch` API that application code can inject and query via `isLeader()`. Leadership is maintained asynchronously in the background; `isLeader()` is a fast in-memory read.

## Compatibility

- Spring Boot: 4.0.x 
- Java: 21

## Modules

- `leader-election-core`: public API and election loop (no Spring Boot dependency).
- `leader-election-autoconfigure`: Spring Boot auto-configuration and JPA implementation.
- `leader-election-spring-boot-starter`: dependency aggregator used by application projects.

## Installation

Add the starter:

```xml
<dependency>
    <groupId>com.allanvital</groupId>
    <artifactId>leader-election-spring-boot-starter</artifactId>
    <version>${leader-election.version}</version>
</dependency>
```

## Usage

Inject `LeaderLatch` and guard leader-only logic:

```java
import com.allanvital.leaderelection.LeaderLatch;
import org.springframework.stereotype.Service;

@Service
class LeaderOnlyJob {

    private final LeaderLatch leaderLatch;

    LeaderOnlyJob(LeaderLatch leaderLatch) {
        this.leaderLatch = leaderLatch;
    }

    void run() {
        if (!leaderLatch.isLeader()) {
            return;
        }
        // leader-only logic
    }
}
```

Example using Spring scheduling:

```java
import com.allanvital.leaderelection.LeaderLatch;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class LeaderScheduledTask {

    private final LeaderLatch leaderLatch;

    LeaderScheduledTask(LeaderLatch leaderLatch) {
        this.leaderLatch = leaderLatch;
    }

    @Scheduled(fixedDelayString = "PT10S")
    void tick() {
        if (leaderLatch.isLeader()) {
            // do the work
        }
    }
}
```

## Configuration

Prefix: `leader.election`

All properties (Spring `.properties` format):

- `leader.election.enabled` (default: `true`)
- `leader.election.lock-name` (default: `default`)
- `leader.election.owner-id` (default: generated from host/process/random suffix)
- `leader.election.lease-duration` (default: `15s`)
- `leader.election.renew-interval` (default: `5s`)
- `leader.election.acquire-interval` (default: `2s`)

## Schema Contract

Expected table contract:

- table: `leader_lock`
- columns:
  - `lock_name` varchar primary key
  - `owner_id` varchar not null
  - `lease_until` timestamp not null
  - `version` bigint not null
  - `updated_at` timestamp not null

`lock-name` maps to `leader_lock.lock_name` (one row per lock).

MySQL example schema (baseline):

```sql
create table leader_lock (
  lock_name varchar(128) not null,
  owner_id varchar(256) not null,
  lease_until timestamp(6) not null,
  version bigint not null,
  updated_at timestamp(6) not null,
  primary key (lock_name)
);
```

## Schema Management

This starter does not run migrations.

- If `spring.jpa.hibernate.ddl-auto=create|update|create-drop`, Hibernate can create/manage the table
- If `spring.jpa.hibernate.ddl-auto=validate|none`, the table must already exist (for example via Flyway/Liquibase)

If the table is missing or the DB user cannot read/write it, the application keeps running but stays follower. The JPA lease store logs an `ERROR` once with guidance.

## How It Works

- Lease-based leader election over a single DB row per lock name.
- `lock-name` is the logical election scope:
  - every distinct `leader.election.lock-name` maps to a different `leader_lock.lock_name` row
  - instances contending for the same `lock-name` elect exactly one leader
  - different lock names are independent elections (useful if you want multiple unrelated leader-only processes)
  - for practical purposes, the default usage is in a single row, so all "nodes of the same cluster" should have the same `lock-name`
- Time source for lease decisions is database time (`CURRENT_TIMESTAMP` via JPQL)
- Contention control uses pessimistic row locking (a single row is locked briefly during acquire/renew)
- Follower behavior: periodically attempts to acquire the lock row at `leader.election.acquire-interval`
- Leader behavior: periodically renews the lease at `leader.election.renew-interval`
- `LeaderLatch.isLeader()` is always an in-memory atomic read.

## Operational Notes

Tuning guidelines:

- `renew-interval` must be less than `lease-duration`.
- Larger `lease-duration` makes leadership more stable (fewer handoffs) but increases worst-case time to fail over when a leader dies
- Smaller `lease-duration` makes failover faster, but increases the chance of churn (leadership changing hands) during short DB pauses or GC pauses
- Smaller `renew-interval` renews more aggressively (more DB traffic) and can tolerate shorter `lease-duration`
- Larger `acquire-interval` reduces DB traffic for followers but makes takeover slower after lease expiry

Logging:

- Enable debug logs:
  - `logging.level.com.allanvital.leaderelection=DEBUG`
- More detail:
  - `logging.level.com.allanvital.leaderelection=TRACE`

## Testing

- Unit tests run with `mvn test`
- Integration tests use Testcontainers (PostgreSQL and MySQL) and require Docker
