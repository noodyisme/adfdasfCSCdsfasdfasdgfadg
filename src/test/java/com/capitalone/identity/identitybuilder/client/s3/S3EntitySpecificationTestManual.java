package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.capitalone.identity.identitybuilder.client.ClientInjectTest;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.util.AWSTestUtil;
import com.capitalone.identity.identitybuilder.util.AWSUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.*;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test will upload the files under "resources/manual-test-policies" to the bucket specified in property
 * csc.s3.bucket-name.east-region. The configStoreClient will load the policies in the bucket based off of the
 * environment set in csc.client-environment. I.E. to change the test environment to dev, set csc.client-environment=dev
 * on line 42. The loaded policy IDs are then cross referenced against the list of expected IDs in expectedIDsMap. If
 * this is failing, debug and set a break point at line 115 to see what ID is failing the test. Requires token to be
 * fetched for profile.
 */
@ContextConfiguration(classes = ClientInjectTest.SpringConfig.class)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "csc.client-environment=prod",
        "csc.dynamic-updates.enabled=false",
        "csc.s3.bucket-name.east-region=identitybuilder-core-testbed-configstore-dev-e1",
        "csc.s3.bucket-name.west-region=identitybuilder-core-testbed-configstore-dev-e1",
        "csc.dev-local.aws-credential-profile-name=GR_GG_COF_AWS_SharedTech_DigiTech_QA_Developer",
        "csc.dev-local.enabled=true"
})
@Disabled
class S3EntitySpecificationTestManual {
    private static final String bucketName = "identitybuilder-core-testbed-configstore-dev-e1";

    AmazonS3 s3Client;
    TransferManager transferManager;
    String keyPrefix;
    Map<String, ArrayList<String>> expectedIDsMap;

    @Value("${csc.client-environment}")
    String environment;

    @Value("${csc.dev-local.aws-credential-profile-name}")
    String profileName;

    @Autowired
    ConfigStoreClient configStoreClient;

    @BeforeEach
    public void setup(){
        String srcDirectory = Objects.requireNonNull(getClassLoader().getResource("manual-test-policies")).getPath() + "/";
        s3Client = AWSTestUtil.getTestClient();

        keyPrefix = "DeleteMe/" + UUID.randomUUID().toString() + "/ManualTests";

        transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        try {
            MultipleFileUpload upload = transferManager.uploadDirectory(bucketName, keyPrefix,
                    new File(srcDirectory), true);
            upload.waitForCompletion();
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ArrayList<String> expectedIDsProd = new ArrayList<>();
        expectedIDsProd.add(keyPrefix+"/PROD/status_Active/1.0");
        expectedIDsProd.add(keyPrefix+"/PROD/status_INACTIVE/1.0");
        expectedIDsProd.add(keyPrefix+"/PROD/status_READY_FOR_PROD/1.0");

        ArrayList<String> expectedIDsQA = new ArrayList<>(expectedIDsProd);
        expectedIDsQA.add(keyPrefix+"/QA/status_READY_FOR_QA/1.0");
        expectedIDsQA.add(keyPrefix+"/QA/status_QA/1.0");
        expectedIDsQA.add(keyPrefix+"/QA/status_APPROVED/1.0");

        ArrayList<String> expectedIDsDev = new ArrayList<>(expectedIDsProd);
        expectedIDsDev.addAll(expectedIDsQA);
        expectedIDsDev.add(keyPrefix+"/DEV/status_IN_PROGRESS/1.0");
        expectedIDsDev.add(keyPrefix+"/DEV/status_IN_REVIEW/1.0");

        expectedIDsMap = new HashMap<>();
        expectedIDsMap.put("prod", expectedIDsProd);
        expectedIDsMap.put("qa", expectedIDsQA);
        expectedIDsMap.put("dev", expectedIDsDev);
    }

    @Test
    public void processS3Specifications() {
        List<Entity> startEntities = configStoreClient.getEntityInfo(EntityType.POLICY)
                .map(configStoreClient::getEntity)
                .collectList()
                .block();

        for (Entity entity: startEntities){
            String id = entity.getId();
            assertTrue(expectedIDsMap.get(environment).contains(id));
            expectedIDsMap.get(environment).remove(id);
        }

        // ensures all IDs are loaded into the expected environment
        assertEquals(0, expectedIDsMap.get(environment).size());
    }

    @AfterEach
    public void removeFile() {
        for (S3ObjectSummary s3ObjectSummary : s3Client.listObjects(bucketName, keyPrefix).getObjectSummaries()){
            s3Client.deleteObject(bucketName, s3ObjectSummary.getKey());
        }
        transferManager.shutdownNow();
    }
}