package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.client.ClientInjectTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClientInjectTest.SpringConfig.class)
@TestPropertySource(properties = {
        "csc.client-environment=prod",
        "identitybuilder.policycore.feature.version-forwarder.enabled=false",
        "csc.dynamic-updates.polling-interval=PT24H",
        "csc.dynamic-updates.enabled=true",
        "csc.s3.bucket-name.east-region=bucket-east",
        "csc.s3.bucket-name.west-region=bucket-west",
})
class ArchaiusInjectTest {

    @Autowired
    ArchaiusPollingConfigurationStreamProvider archaius;


    @Test
    void testInjectedS3() {
        assertNotNull(archaius);
    }
}
