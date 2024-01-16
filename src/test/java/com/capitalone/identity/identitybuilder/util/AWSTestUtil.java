package com.capitalone.identity.identitybuilder.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.capitalone.identity.identitybuilder.client.s3.ConfigStoreClientS3Configuration;

public class AWSTestUtil {

    public static AmazonS3 getTestClient() {
        boolean isPipeline = System.getenv("BUILD_ID") != null;
        if (isPipeline) System.out.printf("SystemEnv:=%s%n", System.getenv());
        ConfigStoreClientS3Configuration configuration = new ConfigStoreClientS3Configuration(
                "identitybuilder-core-testbed-configstore-dev-e1-gen3",
                "",
                Regions.US_EAST_1,
                isPipeline ? null : "GR_GG_COF_AWS_668484372489_Developer",
                !isPipeline
        );
        return configuration.getAwsClient();
    }

    private AWSTestUtil() {
    }

}
