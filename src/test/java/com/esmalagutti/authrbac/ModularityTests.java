package com.esmalagutti.authrbac;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails the build if any module reaches into another module's internals. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(AuthRbacApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
