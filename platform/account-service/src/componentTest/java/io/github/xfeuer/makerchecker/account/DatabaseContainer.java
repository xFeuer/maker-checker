package io.github.xfeuer.makerchecker.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The database of the component tests: a real PostgreSQL in a container, from the image that the stack runs.
 *
 * <p>The build reads the image from the Compose file of the stack and passes it as a system property. Spring starts
 * the container before the service and points the service at it. The container lives as long as the application
 * of the tests.
 */
@TestConfiguration(proxyBeanMethods = false)
class DatabaseContainer {

    @Bean
    @ServiceConnection
    PostgreSQLContainer database(@Value("${makerchecker.database.image}") String image) {
        return new PostgreSQLContainer(image);
    }
}
