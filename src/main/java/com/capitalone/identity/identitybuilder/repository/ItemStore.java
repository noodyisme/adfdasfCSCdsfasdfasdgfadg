package com.capitalone.identity.identitybuilder.repository;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public interface ItemStore {

    Set<ConfigStoreItemInfo> getAllItemInfo() throws IOException;

    ConfigStoreItem getItem(ConfigStoreItemInfo info) throws IOException;

    /**
     * @return {@link Flux<ConfigStoreItemInfo>} of all object metadata in the store.
     */
    Flux<ConfigStoreItemInfo> getStoredItemInfo();

    Optional<ConfigStoreItemInfo> getSingleStoredItemInfo(String key);

    EntityFactory getFactoryForEntityType(EntityType type);

}
