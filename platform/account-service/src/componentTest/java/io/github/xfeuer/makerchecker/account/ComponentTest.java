package io.github.xfeuer.makerchecker.account;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

/**
 * A component test: the whole service on a real port, with its database in a container. See ADR 0017.
 *
 * <p>The test is a client of the service. It gets a {@code RestTestClient} that calls the running server, and it
 * sets up and reads through the API alone.
 *
 * <p>Every class with this annotation shares one application and one database, so no test may count on finding
 * either one empty.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(DatabaseContainer.class)
@interface ComponentTest {}
