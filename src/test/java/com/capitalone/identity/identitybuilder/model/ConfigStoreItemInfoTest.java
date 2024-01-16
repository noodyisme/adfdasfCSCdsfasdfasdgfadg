package com.capitalone.identity.identitybuilder.model;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ConfigStoreItemInfoTest {

    @Test
    public void constructor_nameNull() {
        assertThrows(NullPointerException.class, () -> new ConfigStoreItemInfo(null, "1"));
    }

    @Test
    public void constructor_nameEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigStoreItemInfo("", "1"));
    }

    @Test
    public void constructor_versionNull() {
        assertThrows(NullPointerException.class, () -> new ConfigStoreItemInfo("a", null));
    }

    @Test
    public void testEquals() {
        ConfigStoreItemInfo a = new ConfigStoreItemInfo("a", "1");
        ConfigStoreItemInfo b = new ConfigStoreItemInfo("a", "1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testHashcodeConsistencyAcrossRuntimes() {
        /**
         * Needed for cross-runtime comparison of items
         */
        ConfigStoreItemInfo a = new ConfigStoreItemInfo("a", "1");
        assertEquals(3056, a.hashCode());
    }

    @Test
    public void testNotEquals_name() {
        ConfigStoreItemInfo a = new ConfigStoreItemInfo("a", "1");
        ConfigStoreItemInfo b = new ConfigStoreItemInfo("b", "1");
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEquals_version() {
        ConfigStoreItemInfo a = new ConfigStoreItemInfo("a", "1");
        ConfigStoreItemInfo b = new ConfigStoreItemInfo("a", "2");
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

}
