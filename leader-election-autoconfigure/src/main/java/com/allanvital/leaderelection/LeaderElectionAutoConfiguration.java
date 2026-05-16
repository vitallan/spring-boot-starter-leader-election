package com.allanvital.leaderelection;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.SmartLifecycle;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * @author Allan Vital (https://allanvital.com)
 */

@AutoConfiguration
@EnableConfigurationProperties(LeaderElectionProperties.class)
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "leader.election", name = "enabled", matchIfMissing = true)
public class LeaderElectionAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public LeaderLeaseStore leaderLeaseStore(EntityManagerFactory entityManagerFactory) {
        log.debug("Configuring JpaLeaderLeaseStore");
        return new JpaLeaderLeaseStore(org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityManagerFactoryBuilderCustomizer leaderElectionEntityManagerFactoryBuilderCustomizer() {
        return builder -> builder.setPersistenceUnitPostProcessors(new LeaderElectionPersistenceUnitPostProcessor());
    }

    @Bean
    @ConditionalOnMissingBean(LeaderLatch.class)
    public DefaultLeaderLatch leaderLatch(LeaderElectionProperties properties, LeaderLeaseStore leaderLeaseStore) {
        String ownerId = resolveOwnerId(properties);
        log.debug(
                "Configuring DefaultLeaderLatch lockName={} ownerId={} leaseDuration={} renewInterval={} acquireInterval={}",
                properties.getLockName(),
                ownerId,
                properties.getLeaseDuration(),
                properties.getRenewInterval(),
                properties.getAcquireInterval()
        );
        LeaderElectionConfiguration configuration = new LeaderElectionConfiguration(
                properties.getLockName(),
                ownerId,
                properties.getLeaseDuration(),
                properties.getRenewInterval(),
                properties.getAcquireInterval()
        );
        return new DefaultLeaderLatch(configuration, leaderLeaseStore);
    }

    @Bean
    @ConditionalOnMissingBean(LeaderLatchLifecycle.class)
    public SmartLifecycle leaderLatchLifecycle(DefaultLeaderLatch leaderLatch) {
        return new LeaderLatchLifecycle(leaderLatch);
    }

    private static String resolveOwnerId(LeaderElectionProperties properties) {
        if (properties.getOwnerId() != null && !properties.getOwnerId().isBlank()) {
            return properties.getOwnerId();
        }

        String hostname = "unknown-host";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
        }

        String process = ManagementFactory.getRuntimeMXBean().getName();
        return hostname + "-" + process + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

}
