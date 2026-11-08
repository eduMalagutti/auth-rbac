package com.esmalagutti.authrbac;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * A full-application integration test: the whole Spring context against a real PostgreSQL in Testcontainers.
 *
 * <p>Runs with {@code ./gradlew integrationTest}. See docs/architecture/testing-strategy.md.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {}
