package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class S3BucketResolver {

    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);

    private final String bucketName;
    private final Regions regions;

    public String getBucketName() {
        return bucketName;
    }

    public Regions getRegions() {
        return regions;
    }

    public S3BucketResolver(S3ConfigurationProperties properties) {

        if (properties.getRegionsOverride() != null) {
            regions = properties.getRegionsOverride();
        } else {
            regions = Optional.ofNullable(Regions.getCurrentRegion())
                    .map(Region::getName)
                    .map(Regions::fromName)
                    .orElse(Regions.US_EAST_1);
        }
        LOGGER.debug("Region: {}", regions.getName());
        bucketName = (regions == Regions.US_EAST_1)
                ? properties.getBucketNameForEastRegion()
                : properties.getBucketNameForWestRegion();
    }
}
