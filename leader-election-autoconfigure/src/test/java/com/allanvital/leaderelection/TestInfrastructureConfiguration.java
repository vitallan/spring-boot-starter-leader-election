package com.allanvital.leaderelection;

import jakarta.persistence.EntityManagerFactory;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Allan Vital (https://allanvital.com)
 */

@Configuration
class TestInfrastructureConfiguration {

    @Bean
    EntityManagerFactory entityManagerFactory() {
        return Mockito.mock(EntityManagerFactory.class);
    }

    @Bean
    LeaderLeaseStore leaderLeaseStore() {
        return new LeaderLeaseStore() {
            @Override
            public LeaseOperationResult acquire(LeaseIdentity leaseIdentity) {
                return LeaseOperationResult.ACQUIRED;
            }

            @Override
            public LeaseOperationResult renew(LeaseIdentity leaseIdentity) {
                return LeaseOperationResult.RENEWED;
            }
        };
    }
}
