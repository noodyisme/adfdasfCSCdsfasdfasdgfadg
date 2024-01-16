package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3BucketResolverTest {

    S3BucketResolver s3BucketResolver;

    @Mock
    S3ConfigurationProperties s3ConfigurationProperties;

    @Test
    public void testGetBucketNameEast() {
        when(s3ConfigurationProperties.getRegionsOverride()).thenReturn(Regions.US_EAST_1);
        when(s3ConfigurationProperties.getBucketNameForEastRegion()).thenReturn("testEast");
        s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        assertEquals("testEast", s3BucketResolver.getBucketName());
    }

    @Test
    public void testGetBucketNameWest() {
        when(s3ConfigurationProperties.getRegionsOverride()).thenReturn(Regions.US_WEST_2);
        when(s3ConfigurationProperties.getBucketNameForWestRegion()).thenReturn("testWest");
        s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        assertEquals("testWest", s3BucketResolver.getBucketName());
    }

    @Test
    public void testGetBucketNameCanada() {
        when(s3ConfigurationProperties.getRegionsOverride()).thenReturn(Regions.CA_CENTRAL_1);
        when(s3ConfigurationProperties.getBucketNameForWestRegion()).thenReturn("testWest");
        s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        assertEquals("testWest", s3BucketResolver.getBucketName());
    }

    @Test
    public void testGetRegion() {
        when(s3ConfigurationProperties.getRegionsOverride()).thenReturn(Regions.US_EAST_1);
        s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        assertEquals(Regions.US_EAST_1, s3BucketResolver.getRegions());
    }


    @Test
    public void testBucketConfigurations_OverrideEast() {
        String testBucket = "testBucket";
        when(s3ConfigurationProperties.getBucketNameForEastRegion()).thenReturn(testBucket);
        Region region = RegionUtils.getRegion("us-east-1");

        try (MockedStatic<Regions> mocked = mockStatic(Regions.class)) {
            when(Regions.getCurrentRegion()).thenReturn(region);
            when(Regions.fromName(anyString())).thenReturn(Regions.US_EAST_1);

            S3BucketResolver s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
            assertEquals(testBucket, s3BucketResolver.getBucketName());

        }

    }

    @Test
    public void testBucketConfigurations_OverrideWest() {
        String testBucket = "testBucket";
        when(s3ConfigurationProperties.getBucketNameForWestRegion()).thenReturn(testBucket);

        try (MockedStatic<Regions> mocked = mockStatic(Regions.class)) {
            Region region = RegionUtils.getRegion("us-west-2");
            when(Regions.getCurrentRegion()).thenReturn(region);
            when(Regions.fromName(anyString())).thenReturn(Regions.US_WEST_2);

            S3BucketResolver s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
            assertEquals(testBucket, s3BucketResolver.getBucketName());
        }

    }

}
