package com.allanvital.leaderelection.sample;

import com.allanvital.leaderelection.LeaderLatch;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class SampleAppInstance implements AutoCloseable {

    private final ConfigurableApplicationContext context;

    private SampleAppInstance(ConfigurableApplicationContext context) {
        this.context = context;
    }

    static SampleAppInstance start(MySQLContainer<?> mysql, String ownerId, String lockName) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(LeaderElectionSampleApplication.class)
                .registerShutdownHook(false)
                .properties(
                        "server.port=0",
                        "spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "spring.datasource.driver-class-name=" + mysql.getDriverClassName(),
                        "spring.jpa.hibernate.ddl-auto=update",
                        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                        "leader.election.lock-name=" + lockName,
                        "leader.election.owner-id=" + ownerId,
                        "leader.election.lease-duration=5s",
                        "leader.election.renew-interval=1s",
                        "leader.election.acquire-interval=250ms"
                )
                .run();
        return new SampleAppInstance(context);
    }

    LeaderLatch leaderLatch() {
        return context.getBean(LeaderLatch.class);
    }

    @Override
    public void close() {
        context.close();
    }

}
