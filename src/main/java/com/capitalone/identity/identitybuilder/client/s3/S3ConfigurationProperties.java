package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.regions.Regions;
import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ClientProperties;
import com.capitalone.identity.identitybuilder.client.DevLocalProperties;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.function.Supplier;

@Lazy
@Component
public class S3ConfigurationProperties {

    public static final String S3_PROPERTY_BASE = "csc.s3";

    private static final String BUCKET_NAME_IN_EAST = S3_PROPERTY_BASE + ".bucket-name.east-region";
    private static final String BUCKET_NAME_IN_WEST = S3_PROPERTY_BASE + ".bucket-name.west-region";

    private static final String OVERRIDE_REGION = S3_PROPERTY_BASE + ".override-region";

    private final String eastBucketName;
    private final String westBucketName;

    private final String credentialProfileKey;
    private final Regions regions;
    private final ClientEnvironment clientEnvironment;

    private final boolean proxyEnabled;

    /**
     * @param bucketNameInEastRegion bucket name to use when service is deployed in east region (us-east-1)
     * @param bucketNameInWestRegion bucket name to use when service is deployed in west region (not us-east-1)
     * @param proxyEnabled
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public S3ConfigurationProperties(
            ClientProperties clientProperties,
            Optional<DevLocalProperties> localDebugConfiguration,
            @Value("${" + BUCKET_NAME_IN_EAST + ":#{null}}") String bucketNameInEastRegion,
            @Value("${" + BUCKET_NAME_IN_WEST + ":#{null}}") String bucketNameInWestRegion,
            @Value("${" + OVERRIDE_REGION + ":#{null}}") String overrideRegion,
            @Value("${csc.aws.proxy.enabled:false}") boolean proxyEnabled) {

        clientEnvironment = clientProperties.getClientEnvironment();

        final Supplier<RuntimeException> s3ConfigErrorSupplier = () -> {
            String msg = String.format("Missing required properties: '%s' [found: '%s'] and/or '%s' [found: '%s']",
                    BUCKET_NAME_IN_EAST, bucketNameInEastRegion, BUCKET_NAME_IN_WEST, bucketNameInWestRegion);
            throw new IllegalArgumentException(msg);
        };

        eastBucketName = Optional.ofNullable(bucketNameInEastRegion).orElseThrow(s3ConfigErrorSupplier);
        westBucketName = Optional.ofNullable(bucketNameInWestRegion).orElseThrow(s3ConfigErrorSupplier);

        credentialProfileKey = localDebugConfiguration
                .map(DevLocalProperties::getAwsCredentialProfileName)
                .orElse(null);

        regions = Strings.isBlank(overrideRegion) ? null : Regions.fromName(overrideRegion);
        this.proxyEnabled = proxyEnabled;

    }

    @Nullable
    public Regions getRegionsOverride() {
        return regions;
    }

    @Nullable
    public String getCredentialProfileName() {
        return credentialProfileKey;
    }

    @NotNull
    public String getBucketNameForEastRegion() {
        return eastBucketName;
    }

    @NotNull
    public String getBucketNameForWestRegion() {
        return westBucketName;
    }

    @NotNull
    public ClientEnvironment getClientEnvironment() {
        return clientEnvironment;
    }

    public boolean getIsProxyEnabled() {
        return proxyEnabled;
    }

}
