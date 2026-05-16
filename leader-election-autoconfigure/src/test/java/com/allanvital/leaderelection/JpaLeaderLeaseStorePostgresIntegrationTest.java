package com.allanvital.leaderelection;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


/**
 * @author Allan Vital (https://allanvital.com)
 */
@SpringBootTest(classes = AbstractJpaLeaderLeaseStoreIntegrationTest.TestApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
        "spring.data.jpa.repositories.enabled=true",
        "spring.main.register-shutdown-hook=false",
        "leader.election.enabled=false" //we'll manipulate the calls directly
})
@Testcontainers(disabledWithoutDocker = true)
class JpaLeaderLeaseStorePostgresIntegrationTest extends AbstractJpaLeaderLeaseStoreIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }
}
