package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.util.AWSTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Requires token to be fetched for profile.
 */
class S3ItemStoreTestIT {

    private static final String key = "it_test_file.txt";
    private static final String pagination_prefix = "page_test";
    private static final String bucketName = "identitybuilder-core-testbed-configstore-dev-e1-gen3";
    private static final String prefixToTestPolicy = "us_consumers/ep2_identity_builder_test/test_policy";
    private static final int bucketSize = 18;
    private static final int maxPageSize = 5;

    final String contentV1 = "Test File Version 1";
    final String dynamicContentVersion = "pagination test version";
    String eTagV1;
    String eTagV2;
    AmazonS3 s3Client;
    S3ItemStore store;
    PutObjectResult putObjectResult;

    @BeforeEach
    public void setUp() {
        s3Client = AWSTestUtil.getTestClient();
        boolean isPipeline = System.getenv("BUILD_ID") != null;
        ConfigStoreClientS3Configuration configuration = new ConfigStoreClientS3Configuration(
                "identitybuilder-core-testbed-configstore-dev-e1-gen3",
                "",
                Regions.US_EAST_1,
                isPipeline ? null : "GR_GG_COF_AWS_668484372489_Developer",
                !isPipeline
        );
        s3Client = configuration.getAwsClient();
        this.store = new S3ItemStore(s3Client, bucketName);
        putObjectResult = s3Client.putObject(bucketName, key, contentV1);
        eTagV1 = putObjectResult.getETag();

    }

    @Test
    void get_all_items() {
        assertNotNull(store.getAllItemInfo());
    }

    @Test
    void test_get_content() throws IOException {
        ConfigStoreItemInfo info = new ConfigStoreItemInfo(key, eTagV1);
        String content = store.getItem(info).content;
        assertEquals(contentV1, content);
    }


    @Test
    void test_get_content_fail_tag_mismatch() {
        ConfigStoreItemInfo info = new ConfigStoreItemInfo(key, "bad-tag");
        assertThrows(RuntimeException.class, () -> store.getItem(info));
    }

    @Test
    void test_get_content_fail_missing_key() {
        ConfigStoreItemInfo info = new ConfigStoreItemInfo("missing_key.txt", eTagV1);
        assertThrows(RuntimeException.class, () -> store.getItem(info));
    }

    @Test
    void test_get_object_data_found() {
        Optional<ConfigStoreItemInfo> itemInfo = store.getSingleStoredItemInfo(key);
        assertTrue(itemInfo.isPresent());
        ConfigStoreItemInfo expectedInfo = new ConfigStoreItemInfo(key, eTagV1);
        assertEquals(expectedInfo, itemInfo.get());
    }

    @Test
    void test_get_object_data_missing() {
        Optional<ConfigStoreItemInfo> itemInfo = store.getSingleStoredItemInfo("missing_key.txt");
        assertFalse(itemInfo.isPresent());
    }

    @Test
    void testConfigStoreItemsBasedOnBucketSize() throws IOException {
        // uploading the objects based on bucket size
        for (int i = 0; i < bucketSize; i++) {

            putObjectResult = s3Client.putObject(bucketName, pagination_prefix + i + ".txt", dynamicContentVersion + i);
            eTagV2 = putObjectResult.getETag();

            // verifiying the Objects are successfully uploading into s3 or not
            ConfigStoreItemInfo info = new ConfigStoreItemInfo(pagination_prefix + i + ".txt", eTagV2);
            String content = store.getItem(info).content;
            //checking the content
            assertEquals(dynamicContentVersion + i, content);

        }

        //checking the assertions for all the items got uploaded or not
        assertNotNull(store.getConfigStoreItemInfoStream(pagination_prefix, maxPageSize));
        assertEquals(bucketSize, store.getConfigStoreItemInfoStream(pagination_prefix, maxPageSize).count());

        // deleting all the uploaded objects
        for (int i = 0; i < bucketSize; i++) {
            s3Client.deleteObject(bucketName, pagination_prefix + i + ".txt");
        }

    }

    @Test
    void testConfigStoreItemsWithPrefixDefined() {
        S3ItemStore prefixedItemStore = new S3ItemStore(s3Client, bucketName, prefixToTestPolicy);
        Set<ConfigStoreItemInfo> itemInfo = prefixedItemStore.getAllItemInfo();
        for (ConfigStoreItemInfo item : itemInfo) {
            assertTrue(item.getName().startsWith(prefixToTestPolicy));
        }
    }

    @AfterEach
    public void removeFile() {
        s3Client.deleteObject(bucketName, key);
    }

}
