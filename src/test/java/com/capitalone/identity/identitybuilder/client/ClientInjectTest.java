package com.capitalone.identity.identitybuilder.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClientInjectTest.SpringConfig.class)
@TestPropertySource(properties = {
        "csc.enabled=false"
})
public class ClientInjectTest {

    @Configuration
    @ComponentScan("com.capitalone.identity.identitybuilder")
    public static class SpringConfig {

    }

    @Inject
    ConfigStoreClient client;

    /**
     * Proves that no-op client implementation is injected when library is disabled without conflicts.
     */
    @Test
    void testInjected_NoOp_version_when_disabled() {
        assertTrue(client instanceof ConfigStoreClientConfiguration.NoOpConfigStoreClient);
    }
}
