package com.capitalone.identity.identitybuilder.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import org.apache.commons.lang3.StringUtils;

public class AWSUtil {

    private AWSUtil() {
    }

    public static AmazonS3 createAmazonS3Client(Regions region, String profileName, boolean isProxyEnabled) {
        AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder.standard()
                .withClientConfiguration(getClientConfiguration(isProxyEnabled))
                .withRegion(region);
        if (StringUtils.isNotEmpty(profileName)) {
            s3Builder.withCredentials(new ProfileCredentialsProvider(profileName));
        }

        return s3Builder.build();
    }

    private static ClientConfiguration getClientConfiguration(boolean isProxyEnabled) {
        RetryPolicy retryPolicy = PredefinedRetryPolicies
                .getDefaultRetryPolicyWithCustomMaxRetries(ConfigStoreConstants.MAX_RETRY_COUNT);

        if (isProxyEnabled) {
            return new ClientConfiguration()
                    .withProtocol(Protocol.HTTPS)
                    .withProxyHost(ConfigStoreConstants.PROXY_HOST)
                    .withRetryPolicy(retryPolicy)
                    .withProxyPort(ConfigStoreConstants.PROXY_PORT);
        } else {
            return new ClientConfiguration()
                    .withProtocol(Protocol.HTTPS)
                    .withRetryPolicy(retryPolicy);
        }
    }
}
