package com.capitalone.identity.identitybuilder.client.local;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.repository.EntityProvider;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.junit.jupiter.api.Assertions.*;

class LocalFileDataProviderTest {

    ItemStore itemStore;

    EntityProvider entityProvider;

    @BeforeEach
    public void setup() {
        String srcDirectory = Objects.requireNonNull(
                getClassLoader().getResource("repository/repository-basic")).getPath() + "/";
        itemStore = new LocalDebugItemStore(srcDirectory);
        entityProvider = new EntityProvider(itemStore, Flux::never, ConfigStoreClient_ApplicationEventPublisher.EMPTY);
    }

    @Test
    void getLocalItemData() throws IOException {
        assertFalse(itemStore.getAllItemInfo().isEmpty());
    }

    @Test
    void processLocalItemData() {
        List<EntityInfo> entityInfo = entityProvider.getEntities(EntityType.POLICY, EntityType.PIP)
                .collectList()
                .block();
        assertEquals(3, entityInfo.size());

        List<EntityInfo> allEntityInfo = entityProvider.getEntities(EntityType.POLICY, EntityType.PIP)
                .collectList()
                .block();

        allEntityInfo.removeAll(entityInfo);
        assertTrue(allEntityInfo.isEmpty());

    }

}
