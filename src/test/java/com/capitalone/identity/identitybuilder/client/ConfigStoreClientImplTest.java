package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.client.local.LocalDebugItemStore;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigStoreClientImplTest {

    @Test
    void checkGeneralConfigStoreClientImplementationAgainstLocalStore() {
        LocalDebugItemStore itemStore = new LocalDebugItemStore("test-items");
        ConfigStoreClientImpl client = new ConfigStoreClientImpl(
                itemStore,
                Flux::never,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY);

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

        assertNotNull(client.getEntityUpdatesBatch(Collections.emptyList(), EntityType.POLICY));

    }
}
