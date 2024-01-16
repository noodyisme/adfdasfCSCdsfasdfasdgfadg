package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import com.capitalone.identity.identitybuilder.client.s3.ConfigStoreClientS3Configuration;
import com.capitalone.identity.identitybuilder.client.test.InMemoryConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.ConfigStoreBusinessException;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <p>
 * It is a requirement to test the default methods in ConfigStoreClient
 * ({@link ConfigStoreClient#getEntityUpdates(List, EntityType, EntityType...)}). We use a Partial Mock via the
 * {@link Spy} annotation to invoke the actual interface methods, which allows us to verify that
 * {@link ConfigStoreClient#getEntityUpdatesBatch(List, EntityType, EntityType...)} is called as well.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(PER_CLASS)
public class ConfigStoreClientTest {

    @Spy
    InMemoryConfigStoreClient client = new InMemoryConfigStoreClient();

    List<EntityType> provideEntityParameters() {
        return Arrays.asList(EntityType.POLICY, EntityType.PIP, EntityType.ACCESS, EntityType.UNDEFINED);
    }

    @ParameterizedTest
    @MethodSource("provideEntityParameters")
    void getEntityUpdates_EmptyStartList_Test(EntityType entityType) {
        assertDoesNotThrow(() -> client.getEntityUpdates(Collections.emptyList(), entityType));
        verify(client, times(1)).getEntityUpdatesBatch(Collections.emptyList(), entityType);

    }

    @ParameterizedTest
    @MethodSource("provideEntityParameters")
    void getEntityUpdates_NullStartList_Test(EntityType entityType) {
        assertDoesNotThrow(() -> client.getEntityUpdates(null, entityType));
        verify(client, times(1)).getEntityUpdatesBatch(null, entityType);
    }

    @ParameterizedTest
    @MethodSource("provideEntityParameters")
    void getEntityUpdates_DefinedStartList_Test(EntityType entityType) {
        final EntityInfo.Access expectedResult1 = new EntityInfo.Access(
                "us_consumers/b/c/1/access-control",
                "x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json",
                10, "c", "us_consumers/b/c",
                1,
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"))
        );
        final EntityInfo.Pip expectedResult2 = new EntityInfo.Pip(
                "z/routes/a/b/c/d/routefile.xml",
                "x/y/z/routes/a/b/c/d/routefile.xml",
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/routes/a/b/c/d/routefile.xml", "b"))
        );

        List<EntityInfo> startState = Arrays.asList(expectedResult1, expectedResult2);
        assertDoesNotThrow(() -> client.getEntityUpdates(startState, entityType));
        verify(client, times(1)).getEntityUpdatesBatch(startState, entityType);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:target/test-classes/test-items",
            "file:target/test-classes/test-items/",
            "classpath:test-items",
            "classpath:test-items/",
            "test-items",
            "test-items/",
    })
    void localClientDirectory(String directory) {
        PollingConfiguration pollingConfiguration = new PollingConfiguration(Duration.ofDays(365));
        ConfigStoreClient client = ConfigStoreClient.newLocalClient(directory, pollingConfiguration, null);
        assertNotNull(client);
    }

    @Test
    void s3ClientConfig() {
        PollingConfiguration pollingConfiguration = new PollingConfiguration(Duration.ofDays(365));
        ConfigStoreClientS3Configuration s3Config = new ConfigStoreClientS3Configuration("s3");

        ConfigStoreClient client = ConfigStoreClient.newS3Client(s3Config, pollingConfiguration, null);
        assertNotNull(client);

    }

    @Test
    void s3ClientConfigWithPrefixSet() {
        PollingConfiguration pollingConfiguration = new PollingConfiguration(Duration.ofDays(365));
        ConfigStoreClientS3Configuration s3Config = new ConfigStoreClientS3Configuration("s3", "us_consumers");

        ConfigStoreClient client = ConfigStoreClient.newS3Client(s3Config, pollingConfiguration, null);
        assertNotNull(client);
    }

    @Test
    void s3ClientConfig_throwsWhenMissingRequiredExternalPollingProperties() {
        PollingConfiguration pollingConfiguration = new PollingConfiguration(Duration.ofSeconds(4));
        pollingConfiguration.setExternalPollingPropertiesObjectKey("polling.properties");

        ConfigStoreClientS3Configuration s3Config = new ConfigStoreClientS3Configuration("s3");

        String msg = assertThrows(ConfigStoreBusinessException.class,
                () -> ConfigStoreClient.newS3Client(s3Config, pollingConfiguration, null)).getMessage();
        assertTrue(msg.contains("polling.properties"));

    }

}
