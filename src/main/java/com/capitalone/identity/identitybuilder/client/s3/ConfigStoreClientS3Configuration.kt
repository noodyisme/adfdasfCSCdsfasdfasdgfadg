package com.capitalone.identity.identitybuilder.client.s3

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.capitalone.identity.identitybuilder.util.AWSUtil


data class ConfigStoreClientS3Configuration @JvmOverloads constructor(
    val bucketName: String,
    /**
     * Will restrict to this S3 prefix when scanning and retrieving entities
     */
    val rootPrefix: String = "",
    /**
     * Used with [com.amazonaws.client.builder.AwsClientBuilder.withRegion]
     */
    var region: Regions = Regions.US_EAST_1,
    /**
     * Used with [com.amazonaws.client.builder.AwsClientBuilder.withCredentials]
     */
    var awsCredentialProfileName: String? = null,
    /**
     * Used with [com.amazonaws.ClientConfiguration]
     */
    var awsProxyEnabled: Boolean = false,
) {
    /**
     * This field is lazy-initialized and is set with param values from the time of first call
     */
    val awsClient: AmazonS3 by lazy {
        AWSUtil.createAmazonS3Client(region, awsCredentialProfileName, awsProxyEnabled)
    }
}
