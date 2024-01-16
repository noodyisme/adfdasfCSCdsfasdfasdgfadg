package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigStoreItemTest {

    @Test
    void constructor_infoNull() {
        assertThrows(NullPointerException.class, () -> new ConfigStoreItem((ConfigStoreItemInfo) null, "content"));
    }

    @Test
    void constructor_nameNull() {
        assertThrows(NullPointerException.class, () -> new ConfigStoreItem((String) null, "content"));
    }

    @Test
    void constructor_contentNull() {
        ConfigStoreItemInfo info = new ConfigStoreItemInfo("name", "1");
        assertThrows(NullPointerException.class, () -> new ConfigStoreItem(info, null));
        assertThrows(NullPointerException.class, () -> new ConfigStoreItem("name", null));
    }

    @Test
    void constructor_contentEquivalence() {
        ConfigStoreItem stringConstruction = new ConfigStoreItem("name", "abc");
        ConfigStoreItem infoConstruction = new ConfigStoreItem(
                new ConfigStoreItemInfo("name", "900150983cd24fb0d6963f7d28e17f72"),
                "abc");
        assertEquals(stringConstruction, infoConstruction);
    }

    @Test
    void testNullType() {
        assertEquals(ConfigStoreItem.Type.PROCESS, ConfigStoreItem.getTypeFromPath("test/1.0/process/name.xml"));
        assertEquals(ConfigStoreItem.Type.PROCESS, ConfigStoreItem.getTypeFromPath("test/1.0/34/process/name.xml"));
        assertEquals(ConfigStoreItem.Type.RULES, ConfigStoreItem.getTypeFromPath("test/1.0/rules/name.dmn"));
        assertEquals(ConfigStoreItem.Type.LIBRARY, ConfigStoreItem.getTypeFromPath("test/1.0/routes/name.xml"));
        assertEquals(ConfigStoreItem.Type.POLICY_STATUS, ConfigStoreItem.getTypeFromPath("test/1.0/metadata.json"));
        assertEquals(ConfigStoreItem.Type.UNRECOGNIZED, ConfigStoreItem.getTypeFromPath(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test/3.4/config/defaults.json",
            "test/3.4/55/config/defaults.json",
            "policy/1.0/config/defaults.json",
            "policy/1.11/config/defaults.json",
            "policy/11.11/config/defaults.json",
            "policy/1.0/55/config/defaults.json",
            "policy/1.11/55/config/defaults.json",
            "policy/11.11/55/config/defaults.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/config/defaults.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/defaults.json",
    })
    void testUseCaseDefaultType(String key) {
        assertEquals(ConfigStoreItem.Type.CONFIG_DEFAULT, ConfigStoreItem.getTypeFromPath(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test/3.4/config/features.json",
            "test/3.4/55/config/features.json",
            "policy/1.0/config/features.json",
            "policy/1.11/config/features.json",
            "policy/11.11/config/features.json",
            "policy/1.0/55/config/features.json",
            "policy/1.11/55/config/features.json",
            "policy/11.11/55/config/features.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/config/features.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/features.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/features-dev.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/features-qa.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/features-perf-e.json",
    })
    void testFeaturesConfigType(String key) {
        assertEquals(ConfigStoreItem.Type.CONFIG_FEATURES, ConfigStoreItem.getTypeFromPath(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test/0.0/config/schema.json",
            "test/0.0/55/config/schema.json",
            "policy/1.0/config/schema.json",
            "policy/1.11/config/schema.json",
            "policy/14.11/config/schema.json",
            "policy/1.0/55/config/schema.json",
            "policy/1.11/55/config/schema.json",
            "policy/14.0/55/config/schema.json",
            "/User/policies/repo/us_consumers/lob/policy_a/1.0/config/schema.json",
            "/User/policies/repo/us_consumers/lob/policy_a/1.0/55/config/schema.json",
    })
    void testUseCaseSchemaType(String key) {
        assertEquals(ConfigStoreItem.Type.CONFIG_SCHEMA, ConfigStoreItem.getTypeFromPath(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test/0.0/config/test.json",
            "test/0.0/55/config/test.json",
            "policy/1.0/config/a.b.c.json",
            "policy/1.11/config/a.b.c.json",
            "policy/14.11/config/a.b.c.json",
            "policy/1.0/55/config/a.b.c.json",
            "policy/1.11/55/config/a.b.c.json",
            "policy/14.11/55/config/a.b.c.json",
            "/User/policies/repo/us_consumers/lob/policy_a/1.0/config/hello-there.json",
            "/User/policies/repo/us_consumers/lob/policy_a/1.0/55/config/hello-there.json",
    })
    void testUseCaseType(String key) {
        assertEquals(ConfigStoreItem.Type.CONFIG_USECASE, ConfigStoreItem.getTypeFromPath(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name",
            "",
            "name.xml",
            "name.dmn",
            "meta.json",
            "policy/1.0/default.json",
            "policy/1.0/55/default.json",
            "policy/1.0/schema.json",
            "policy/1.0/55/schema.json",
            "policy/1.0/abc.json",
            "policy/1.0/55/abc.json",
            "policy/1.0/dir/default.json",
            "policy/1.0/55/dir/default.json",
            "policy/1.0/dir/default.xml",
            "policy/1.0/55/dir/default.xml",
            "policy/1.0/dir/default.dmn",
            "policy/1.0/55/dir/default.dmn",
            "policy/1.0/dir/schema.json",
            "policy/1.0/55/dir/schema.json",
            "policy/1.0/dir/abc.json",
            "policy/1.0/55/dir/abc.json",
            "policy_a/1.0/55/config/defaults",
            "policy_a/1.0/55/config/defaults.xml",
            "policy_a/1.0/55/config/schema",
            "policy_a/1.0/55/config/schema.xml",
            "policy_a/1.0/55/config/schema.txt",
            "policy_a/1.0/55/config/abc",
            "policy_a/1.0/55/config/abc.xml",
            // unrecognized location (must be under major.minor version)
            "policy_a/1.0-123/process/abc.xml",
            "policy_a/1.0-123/55/process/abc.xml",
            "policy_a/1.0-123-8bfbf63/process/abc.xml",
            "policy_a/1.0-123/8bfbf63/process/abc.xml",
            "policy_a/1.0-123/55/8bfbf63/process/abc.xml",
            "policy_a/1.0.0/process/abc.xml",
            "policy_a/1.0/34-8bfbf63/process/abc.xml",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/featuresqa.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/features.qa.json",
            "/User/abc123/Dev/policies/repo/us_consumers/lob/policy_a/1.0/55/config/features_qa.json",
    })
    void testUnrecognizedType(String key) {
        assertEquals(ConfigStoreItem.Type.UNRECOGNIZED, ConfigStoreItem.getTypeFromPath(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name1",
            "a/b/c"
    })
    void testGetters(String expectName) {
        ConfigStoreItemInfo info = new ConfigStoreItemInfo(expectName, "1");
        ConfigStoreItem item = new ConfigStoreItem(info, "content");
        assertEquals(expectName, item.getName());
    }

    @Test
    void testEquals() {
        ConfigStoreItemInfo infoA = new ConfigStoreItemInfo("name", "1");
        ConfigStoreItemInfo infoB = new ConfigStoreItemInfo("name", "1");

        ConfigStoreItem item1 = new ConfigStoreItem(infoA, "content");
        ConfigStoreItem item2 = new ConfigStoreItem(infoB, "content");
        assertNotNull(item1);
        assertNotNull(item2);
        assertEquals(item1, item1);
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

    }

    @Test
    void testNotEquals() {
        ConfigStoreItemInfo infoA = new ConfigStoreItemInfo("name1", "1");
        ConfigStoreItemInfo infoB = new ConfigStoreItemInfo("name2", "1");
        assertNotEquals(infoA, infoB);

        ConfigStoreItem item1 = new ConfigStoreItem(infoA, "content");
        ConfigStoreItem item2 = new ConfigStoreItem(infoB, "content");

        assertNotEquals(item1, null);
        assertNotEquals(item1, "DifferentObjectType");

        assertNotEquals(item1, item2);
        assertNotEquals(item1.hashCode(), item2.hashCode());

        ConfigStoreItem itemA = new ConfigStoreItem(infoA, "contentA");
        ConfigStoreItem itemB = new ConfigStoreItem(infoA, "contentB");
        assertNotEquals(itemA, itemB);
        assertNotEquals(itemA.hashCode(), itemB.hashCode());

    }

}
