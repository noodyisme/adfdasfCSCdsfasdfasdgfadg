package com.capitalone.identity.identitybuilder.client.local;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ClientInjectTest;
import com.capitalone.identity.identitybuilder.client.ClientProperties;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClientInjectTest.SpringConfig.class)
@TestPropertySource(properties = {
        "csc.client-environment=dev",
        "csc.dynamic-updates.polling-interval=PT5S",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=manual-test-policies/DEV"
})
public class LocalDebugInjectTest {
    @Autowired
    ConfigStoreClient client;

    @Autowired
    ClientEnvironment environment;

    /**
     * Proves that local debug client implementation is injected when library is disabled without conflicts.
     */
    @Test
    void testInjectedLocalClient() {
        assertTrue(client instanceof LocalDebugConfigStoreClient);
        assertEquals(ClientEnvironment.DEV, environment);
    }
}
