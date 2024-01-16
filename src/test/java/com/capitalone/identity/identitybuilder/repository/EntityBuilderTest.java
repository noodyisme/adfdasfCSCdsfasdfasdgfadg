package com.capitalone.identity.identitybuilder.repository;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityBuilderTest {

    @Test
    void addItem_Directory() {
        EntityBuilder builder = new EntityBuilder("a/b/c/1.0", "a/b/c/1.0/1", 1, null, null);
        assertTrue(builder.addItem(new ConfigStoreItemInfo("a/b/c/1.0/1/process/policy.xml", "a")));
        assertFalse(builder.addItem(new ConfigStoreItemInfo("a/b/c/1.0/10/process/policy.xml", "a")));
    }

    @Test
    void addItem_Single() {
        EntityBuilder builder = new EntityBuilder("routes/pip_a.xml", "routes/pip_a.xml", 0, null, null);
        assertTrue(builder.addItem(new ConfigStoreItemInfo("routes/pip_a.xml", "a")));
    }

}
