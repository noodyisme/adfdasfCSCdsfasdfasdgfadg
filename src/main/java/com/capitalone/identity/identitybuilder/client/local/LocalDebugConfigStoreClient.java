package com.capitalone.identity.identitybuilder.client.local;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.DevLocalProperties;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import com.capitalone.identity.identitybuilder.repository.EntityProvider;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

public class LocalDebugConfigStoreClient implements ConfigStoreClient {

    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);

    private final EntityProvider entityProvider;

    public LocalDebugConfigStoreClient(@Nullable String rootDir,
                                       @NonNull ClientEnvironment environment) {
        this(rootDir, environment, Flux::never, ConfigStoreClient_ApplicationEventPublisher.EMPTY);
    }

    public LocalDebugConfigStoreClient(@Nullable String rootDir,
                                       @NonNull ClientEnvironment environment,
                                       @NonNull ScanRequester scanRequester,
                                       @NonNull ConfigStoreScanCompleted_Publisher scanPublisher) {

        LOGGER.debug("Config Store Client: LOCAL DEBUG ENABLED. Root Directory Used: {}", rootDir);
        try {
            ItemStore itemStore = new LocalDebugItemStore(rootDir);
            entityProvider = new EntityProvider(itemStore,
                    Objects.requireNonNull(scanRequester),
                    Objects.requireNonNull(scanPublisher),
                    environment);
        } catch (IllegalArgumentException e) {
            final String msg = String.format("Must set property '%s' before running in local debug mode.",
                    DevLocalProperties.DEV_LOCAL_ROOT_DIR);
            throw new IllegalArgumentException(msg, e);
        }

    }

    @Override
    public Entity getEntity(EntityInfo entityInfo) {
        return entityProvider.getEntity(entityInfo);
    }

    @Override
    public Flux<EntityInfo> getEntityInfo(EntityType type, EntityType... typeFilter) {
        return entityProvider.getEntities(type, typeFilter);
    }

    @Override
    public Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> startList, EntityType type, EntityType... typeFilter) {
        return entityProvider.getEntityUpdatesBatch(startList, type, typeFilter);
    }

}
