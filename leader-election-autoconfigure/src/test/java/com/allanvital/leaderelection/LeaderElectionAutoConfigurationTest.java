package com.allanvital.leaderelection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class LeaderElectionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LeaderElectionAutoConfiguration.class));

    @Test
    public void doesNotCreateBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("leader.election.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LeaderLatch.class));
    }

    @Test
    public void createsLeaderLatchWhenLeaseStoreAndEntityManagerFactoryExist() {
        contextRunner
                .withUserConfiguration(TestInfrastructureConfiguration.class)
                .withPropertyValues("spring.data.jpa.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(LeaderLatch.class);
                    assertThat(context).hasSingleBean(LeaderLeaseStore.class);
                });
    }

}
