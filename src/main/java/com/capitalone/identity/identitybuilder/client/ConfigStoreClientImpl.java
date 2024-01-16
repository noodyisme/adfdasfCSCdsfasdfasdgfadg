package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import com.capitalone.identity.identitybuilder.repository.EntityProvider;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

public class ConfigStoreClientImpl implements ConfigStoreClient {

    private final EntityProvider provider;

    ConfigStoreClientImpl(@NonNull ItemStore itemStore,
                          @NonNull ScanRequester scanRequester,
                          @NonNull ConfigStoreClient_ApplicationEventPublisher scanPublisher) {
        this(new EntityProvider(itemStore, scanRequester, scanPublisher));
    }

    public ConfigStoreClientImpl(EntityProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public Entity getEntity(EntityInfo entityInfo) {
        return provider.getEntity(entityInfo);
    }

    @Override
    public Flux<EntityInfo> getEntityInfo(EntityType type, EntityType... typeFilter) {
        return provider.getEntities(type, typeFilter);
    }

    @Override
    public Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> startList, EntityType type, EntityType... typeFilter) {
        return provider.getEntityUpdatesBatch(startList, type, typeFilter);
    }
}
