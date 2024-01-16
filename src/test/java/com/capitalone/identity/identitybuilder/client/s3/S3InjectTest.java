package com.capitalone.identity.identitybuilder.client.s3;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ClientInjectTest;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClientInjectTest.SpringConfig.class)
@TestPropertySource(properties = {
        "csc.client-environment=prod",
        "csc.dynamic-updates.enabled=false",
        "csc.s3.bucket-name.east-region=bucket-east",
        "csc.s3.bucket-name.west-region=bucket-west"
})
public class S3InjectTest {

    @Autowired
    ConfigStoreClient client;

    @Autowired
    ClientEnvironment environment;

    @Autowired
    S3ConfigurationProperties properties;

    /**
     * Proves that S3 client implementation can be injected by library users without conflicts.
     */
    @Test
    void testInjectedS3() {
        assertNotNull(client);
        assertEquals(ClientEnvironment.PROD, environment);
        assertFalse(properties.getIsProxyEnabled());
    }
}
