package com.esmalagutti.authrbac;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * A Spring Modulith module test: bootstraps only the module the test class lives in, against a real PostgreSQL in
 * Testcontainers. Put the test class in the module's package (e.g. {@code com.esmalagutti.authrbac.rbac}).
 *
 * <p>Runs with {@code ./gradlew moduleTest}. See docs/architecture/testing-strategy.md.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("module")
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
public @interface ModuleTest {}
