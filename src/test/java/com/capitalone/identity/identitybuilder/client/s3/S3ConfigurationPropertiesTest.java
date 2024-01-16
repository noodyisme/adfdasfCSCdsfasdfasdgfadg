package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.regions.Regions;
import com.capitalone.identity.identitybuilder.client.ClientProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class S3ConfigurationPropertiesTest {

    private final String prod = "prod";
    private final String qa = "qa";
    private final String dev = "dev";

    private final String bucketEast = "custom-bucket-east";
    private final String bucketWest = "custom-bucket-west";


    @ParameterizedTest
    @ValueSource(strings = {prod, qa, dev})
    void construct_ok(String environment) {
        ClientProperties clientProperties = new ClientProperties(environment);
        S3ConfigurationProperties properties = new S3ConfigurationProperties(clientProperties, Optional.empty(), bucketEast, bucketWest, null, false);
        assertNotNull(properties.getBucketNameForEastRegion());
        assertNotNull(properties.getBucketNameForWestRegion());
        // region override now allowed
        assertNull(properties.getRegionsOverride());
    }

    @ParameterizedTest
    @ValueSource(strings = {prod, qa, dev})
    void construct_ok_bucket_args(String environment) {

        ClientProperties clientProperties = new ClientProperties(environment);

        S3ConfigurationProperties properties = new S3ConfigurationProperties(clientProperties, Optional.empty(), bucketEast, bucketWest, null, false);

        assertEquals(bucketEast, properties.getBucketNameForEastRegion());
        assertEquals(bucketWest, properties.getBucketNameForWestRegion());

        // Error if null values for east or west or both
        assertThrows(IllegalArgumentException.class, () -> new S3ConfigurationProperties(clientProperties, Optional.empty(), null, null, null, false));
        assertThrows(IllegalArgumentException.class, () -> new S3ConfigurationProperties(clientProperties, Optional.empty(), bucketEast, null, null, false));
        assertThrows(IllegalArgumentException.class, () -> new S3ConfigurationProperties(clientProperties, Optional.empty(), null, bucketWest, null, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {prod, qa, dev})
    void construct_s3_regionOverrideTest(String environment) {
        String overrideRegion = "us-west-2";
        ClientProperties clientProperties = new ClientProperties(environment);
        S3ConfigurationProperties properties = new S3ConfigurationProperties(clientProperties, Optional.empty(), bucketEast, bucketWest, overrideRegion, false);
        assertEquals(Regions.US_WEST_2, properties.getRegionsOverride());
    }

    @ParameterizedTest
    @ValueSource(strings = {prod, qa, dev})
    void construct_s3_regionOverrideTest_default(String environment) {
        ClientProperties clientProperties = new ClientProperties(environment);
        S3ConfigurationProperties properties = new S3ConfigurationProperties(clientProperties, Optional.empty(), bucketEast, bucketWest, Regions.US_EAST_1.getName(), false);
        assertEquals(Regions.US_EAST_1, properties.getRegionsOverride());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void construct_proxyEnabled(boolean expected) {
        S3ConfigurationProperties properties = new S3ConfigurationProperties(new ClientProperties("prod"),
                Optional.empty(), bucketEast, bucketWest, Regions.US_EAST_1.getName(),
                expected);
        assertEquals(expected, properties.getIsProxyEnabled());
    }

}
