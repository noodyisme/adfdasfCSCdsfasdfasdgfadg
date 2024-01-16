package com.capitalone.identity.identitybuilder.repository;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Allows for construction of an immutable {@link EntityInfo} object during a scan operation.
 * Component items of an entity can be added via {@link #addItem(ConfigStoreItemInfo)} and
 * the final entity constructed with {@link #build()}
 */
class EntityBuilder {

    final String id;
    final String locationPrefix;
    final int versionNumber;
    final Matcher objectMatcher;
    final CommonItemStore.EntityFunction generator;
    final Set<ConfigStoreItemInfo> items = new HashSet<>();

    EntityBuilder(String id, String prefix, int versionNumber, Matcher objectMatcher,
                  CommonItemStore.EntityFunction generator) {
        this.id = id;
        this.locationPrefix = prefix;
        this.versionNumber = versionNumber;
        this.objectMatcher = objectMatcher;
        this.generator = generator;
    }

    /**
     * Adds a {@link ConfigStoreItem} to the set of individual items that define this entity. This operation
     * is conditional and depends on whether the provided item should be classified as part of the entity based
     * on item name and location prefix of the entity.
     *
     * @param item the item to add
     * @return true if the item is classified as part of the entity, false if the item does not belong to this entity
     */
    boolean addItem(ConfigStoreItemInfo item) {
        if (item.getName().equals(locationPrefix) || item.getName().startsWith(locationPrefix + "/")) {
            items.add(item);
            return true;
        } else {
            return false;
        }
    }

    EntityInfo build() {
        return generator.getInfo(id, locationPrefix, versionNumber, objectMatcher, items);
    }
}
