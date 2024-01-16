package com.capitalone.identity.identitybuilder.client.local;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalDebugConfigStoreClientTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "file:target/test-classes/test-items",
            "classpath:test-items",
            "test-items",
    })
    void construct_directory_file(String directory) {
        ConfigStoreClient client = new LocalDebugConfigStoreClient(directory, ClientEnvironment.DEV);
        List<Entity> startEntities = client.getEntityInfo(EntityType.POLICY)
                .map(client::getEntity)
                .collectList().block();
        EntityInfo info = startEntities.stream().findFirst().map(Entity::getInfo).orElse(null);
        assertEquals(1, startEntities.size());
        assertNotNull(info);
        Entity entityStored = client.getEntity(info);
        assertEquals(info, entityStored.getInfo());
        assertEquals(4, info.getItemInfo().size());
        assertEquals(1, info.getFilteredItemNames().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:non-existing",
            "classpath:non-existing",
            "non-existing",
    })
    @NullAndEmptySource
    void construct_directory_file_error(String directory) {
        assertThrows(RuntimeException.class, () -> new LocalDebugConfigStoreClient(directory, ClientEnvironment.DEV));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:target/test-classes/test-items-pip",
            "classpath:test-items-pip",
            "test-items-pip",
    })
    void construct_directory_file_stream_pip(String directory) {
        ConfigStoreClient client = new LocalDebugConfigStoreClient(directory, ClientEnvironment.DEV);
        StepVerifier.withVirtualTime(() -> client.getEntityInfo(EntityType.PIP))
                .expectSubscription()
                .expectNextCount(1)
                .verifyComplete();

    }

}
