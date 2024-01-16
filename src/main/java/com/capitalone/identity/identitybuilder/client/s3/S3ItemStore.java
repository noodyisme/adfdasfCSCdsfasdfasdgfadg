package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.repository.CommonItemStore;
import com.capitalone.identity.identitybuilder.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class S3ItemStore extends CommonItemStore {
    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);

    private final String bucketName;
    private final String rootPrefix;
    private final AmazonS3 s3Client;
    private final int maxPageSize = 1000;

    public S3ItemStore(@NonNull AmazonS3 s3Client, @NonNull String bucketName, @NonNull String rootPrefix) {
        this.bucketName = StringUtils.requireNotNullOrBlank(bucketName);
        this.s3Client = Objects.requireNonNull(s3Client);
        this.rootPrefix = Objects.requireNonNull(rootPrefix);
    }

    public S3ItemStore(@NonNull AmazonS3 s3Client, @NonNull String bucketName) {
        this(s3Client, bucketName, "");
    }

    public S3ItemStore(@NonNull ConfigStoreClientS3Configuration configuration) {
        this(configuration.getAwsClient(), configuration.getBucketName(), configuration.getRootPrefix());
    }

    @Override
    public Set<ConfigStoreItemInfo> getAllItemInfo() {
        return getConfigStoreItemInfoStream(rootPrefix, maxPageSize).collect(Collectors.toSet());
    }

    @Override
    public ConfigStoreItem getItem(ConfigStoreItemInfo info) throws IOException {

        final String key = rootPrefixBoundedLocation(info.getName());
        final String tag = info.getTag();
        final GetObjectRequest request = new GetObjectRequest(bucketName, key);
        request.setMatchingETagConstraints(Collections.singletonList(tag));

        // Get item and look for item in version history if not found
        S3Object object = Optional.ofNullable(s3Client.getObject(request))
                .orElseThrow(() -> {
                    String msg = String.format("Item not found: key: %s, tag: %s", key, tag);
                    return new IllegalArgumentException(msg);
                });

        try (S3ObjectInputStream objectContent = object.getObjectContent()) {
            String result = IOUtils.toString(objectContent);
            LOGGER.debug("Downloaded: {}:={}", key, result);
            return new ConfigStoreItem(info, result);
        }

    }

    @Override
    public Flux<ConfigStoreItemInfo> getStoredItemInfo() {
        Stream<ConfigStoreItemInfo> configStoreItemInfoStream = getConfigStoreItemInfoStream(rootPrefix, maxPageSize);
        return Flux.fromStream(configStoreItemInfoStream)
                .sort(Comparator.comparing(ConfigStoreItemInfo::getName));
    }

    @Override
    public Optional<ConfigStoreItemInfo> getSingleStoredItemInfo(String key) {
        String boundedKey = rootPrefixBoundedLocation(key);
        ObjectMetadata objectMetadata = s3Client.doesObjectExist(bucketName, boundedKey)
                ? s3Client.getObjectMetadata(bucketName, boundedKey)
                : null;

        return Optional.ofNullable(objectMetadata)
                .map(objectData -> new ConfigStoreItemInfo(boundedKey, objectData.getETag()));

    }

    Stream<ConfigStoreItemInfo> getConfigStoreItemInfoStream(String prefix, int maxPageSize) {
        ListObjectsRequest listObjectsRequest = Strings.isBlank(prefix)
                ? new ListObjectsRequest().withBucketName(bucketName).withMaxKeys(maxPageSize)
                : new ListObjectsRequest().withBucketName(bucketName).withMaxKeys(maxPageSize).withPrefix(prefix);

        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        Set<ConfigStoreItemInfo> resultConfigStore = processObjectListingItems(objectListing);

        // if it reaches the default limit of 1000, the if block code will be executed
        while (objectListing.isTruncated()) {
            objectListing = s3Client.listNextBatchOfObjects(objectListing);
            resultConfigStore.addAll(processObjectListingItems(objectListing));
        }

        return resultConfigStore.stream();
    }

    private Set<ConfigStoreItemInfo> processObjectListingItems(ObjectListing objectListing) {

        List<S3ObjectSummary> objectSummaries = new ArrayList<>(objectListing.getObjectSummaries());
        return objectSummaries.parallelStream()
                .map(objectSummary -> {
                    String key = objectSummary.getKey();
                    return new ConfigStoreItemInfo(key, objectSummary.getETag());
                }).collect(Collectors.toSet());
    }

    private String rootPrefixBoundedLocation(String location) {
        if (location != null && location.startsWith(rootPrefix)) {
            return location;
        } else {
            throw new UnsupportedOperationException(
                    String.format("Requested location outside root context [rootPrefix='%s', location='%s']",
                            rootPrefix, location));

        }
    }

}
