package com.capitalone.identity.identitybuilder.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DevLocalPropertiesTest {

    @Test
    void construct_dev_local_fails_without_profile() {
        assertThrows(IllegalArgumentException.class, () -> new DevLocalProperties(null,null));
    }

    @Test
    void construct_dev_local_profileSet() {
        String credentials = "awsProfileName";
        DevLocalProperties properties = new DevLocalProperties(credentials, null);
        assertEquals(credentials, properties.getAwsCredentialProfileName());
    }

    @Test
    void construct_dev_local_debugRootDirectorySet() {
        String directory = "/test";
        DevLocalProperties properties = new DevLocalProperties(null, directory);
        assertEquals(directory, properties.getRootDirectory());
    }
}
