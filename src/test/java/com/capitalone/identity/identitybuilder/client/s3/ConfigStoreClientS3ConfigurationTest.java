package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreClientS3ConfigurationTest {

    @Test
    void testBuild_nullPointer() {
        assertThrows(NullPointerException.class,
                () -> new ConfigStoreClientS3Configuration(null));
    }

    @Test
    void testBuild_defaults() {
        ConfigStoreClientS3Configuration config = new ConfigStoreClientS3Configuration("test");

        assertEquals("test", config.getBucketName());
        assertEquals(Regions.US_EAST_1, config.getRegion());
        assertFalse(config.getAwsProxyEnabled());
        assertNotNull(config.getAwsClient());
        assertNull(config.getAwsCredentialProfileName());
    }

    @Test
    void testBuild_nonDefaults() {
        ConfigStoreClientS3Configuration config = new ConfigStoreClientS3Configuration("test");
        config.setRegion(Regions.US_WEST_2);
        config.setAwsProxyEnabled(true);
        config.setAwsCredentialProfileName("CI_PROFILE");

        assertEquals("test", config.getBucketName());
        assertEquals(Regions.US_WEST_2, config.getRegion());
        assertTrue(config.getAwsProxyEnabled());
        assertEquals("CI_PROFILE", config.getAwsCredentialProfileName());
        assertNotNull(config.getAwsClient());
    }

    @Test
    void lateInitS3Client() {
        ConfigStoreClientS3Configuration s3Config = new ConfigStoreClientS3Configuration("s3");
        s3Config.setRegion(Regions.US_WEST_2);

        AmazonS3 awsClientA = s3Config.getAwsClient();
        s3Config.setRegion(Regions.US_EAST_1);
        AmazonS3 awsClientB = s3Config.getAwsClient();

        // check that the client is initialized lazily
        assertNotNull(awsClientA);
        assertEquals(awsClientA, awsClientB);
        assertEquals(Regions.US_WEST_2.getName(), s3Config.getAwsClient().getRegionName());
    }
}
