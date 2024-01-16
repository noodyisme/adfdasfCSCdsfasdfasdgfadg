package com.capitalone.identity.identitybuilder.client.test;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import reactor.core.publisher.Flux;

import java.util.List;

public class InMemoryConfigStoreClient implements ConfigStoreClient {

    @Override
    public Entity getEntity(EntityInfo entityInfo) {
        return null;
    }

    @Override
    public Flux<EntityInfo> getEntityInfo(EntityType type, EntityType... typeFilter) {
        return Flux.empty();
    }

    @Override
    public Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> startList, EntityType type, EntityType... typeFilter) {
        return Flux.empty();
    }

}
