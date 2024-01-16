package com.capitalone.identity.identitybuilder.model;

import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityInfoTest {

    @Test
    void testNotEquals_nameDifference() {
        EntityInfo a = new MockEntityInfo("a", EntityType.POLICY);
        EntityInfo b = new MockEntityInfo("b", EntityType.POLICY);

        assertEquals(a.getVersion(), b.getVersion());
        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(a, b);

    }

    @Test
    void testNotEquals_componentDifferences() {

        EntityInfo a = new MockEntityInfo("a", EntityType.POLICY,
                new ConfigStoreItemInfo("a/b/a/1.0/process/a.xml", "a"),
                new ConfigStoreItemInfo("a/b/a/1.0/process/c.xml", "c")
        );

        EntityInfo b = new MockEntityInfo("a", EntityType.POLICY,
                new ConfigStoreItemInfo("a/b/a/1.0/process/b.xml", "a"),
                new ConfigStoreItemInfo("a/b/a/1.0/process/c.xml", "c")
        );

        assertNotEquals(a, b);
        assertNotEquals(a.getVersion(), b.getVersion());
    }

    @Test
    void testNotEquals_logicalVersionDifference() {
        EntityInfo a = new MockEntityInfo("a", 1, 0, 0);
        EntityInfo b = new MockEntityInfo("a", 0, 1, 0);
        EntityInfo c = new MockEntityInfo("a", 0, 0, 1);

        EntityInfo aControl = new MockEntityInfo("a", 1, 0, 0);
        assertEquals(a, aControl);
        assertEquals(a.hashCode(), aControl.hashCode());

        assertNotEquals(a, b);
        assertNotEquals(b, c);
        assertNotEquals(a, c);

        assertNotEquals(a.hashCode(), b.hashCode());
        assertNotEquals(b.hashCode(), c.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());

    }

    @Test
    void testEquals_policy() {
        EntityInfo a = new EntityInfo.Policy(
                new PolicyDefinition("a", "a", "a", 1, 1, 0),
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("c", "c"),
                        new ConfigStoreItemInfo("a", "a"),
                        new ConfigStoreItemInfo("xx", "xx"),
                        new ConfigStoreItemInfo("yy", "yy")
                )));

        EntityInfo b = new EntityInfo.Policy(
                new PolicyDefinition("a", "a", "a", 1, 1, 0),
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("a", "a"),
                        new ConfigStoreItemInfo("c", "c"),
                        new ConfigStoreItemInfo("zz", "zz")
                )));

        assertEquals(a.getVersion(), b.getVersion());
        assertEquals(a, b);
    }

    @Test
    void testFiltered_policy() {
        EntityInfo b = new EntityInfo.Policy(
                new PolicyDefinition("a", "a", "a", 1, 0, 0),
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("a", "a"),
                        new ConfigStoreItemInfo("c", "c"),
                        new ConfigStoreItemInfo("xx", "xxxx"),
                        new ConfigStoreItemInfo("yy", "yyyy")
                )));
        Set<String> ignoredItems = b.getFilteredItemNames();
        assertFalse(ignoredItems.isEmpty());
        assertTrue(ignoredItems.contains("xx"));
        assertTrue(ignoredItems.contains("yy"));
    }

    @Test
    void testPolicyEntityEquals() {
        EntityInfo policyA = new EntityInfo.Policy(new PolicyDefinition("lob/policy_a/2.0"), Collections.emptySet());
        EntityInfo policyB = new EntityInfo.Policy(new PolicyDefinition("lob/policy_a/2.0"), Collections.emptySet());
        assertEquals(policyA, policyB);
        assertEquals(policyA.getVersion(), policyB.getVersion());
    }

    @Test
    void testPolicyEntityNotEquals() {
        EntityInfo policyA = new EntityInfo.Policy(new PolicyDefinition("lob/policy_a/2.0"), Collections.emptySet());
        EntityInfo policyB = new EntityInfo.Policy(new PolicyDefinition("lob/policy_b/2.0"), Collections.emptySet());
        assertNotEquals(policyA, policyB);
    }

    @Test
    void testPolicyEntityNotEquals_priorVersion() {
        EntityInfo.Access access1 = new EntityInfo.Access("a", "a", 0, "a", "a", 1, Collections.emptySet());
        EntityInfo.Access access2 = new EntityInfo.Access("a", "a", 1, "a", "a", 1, Collections.emptySet());
        EntityInfo returnedVersion = access2.setPriorVersion(access1);
        assertNotEquals(access1, access2);
        assertEquals(access2, returnedVersion);
        assertEquals(access1, access2.getPriorVersion());
        assertNull(access1.getPriorVersion());
    }

    @Test
    void testPolicyEntityCircularReference() {
        EntityInfo.Access access1 = new EntityInfo.Access("a", "a", 1, "a", "a", 1, Collections.emptySet());
        EntityInfo.Access access2 = new EntityInfo.Access("a", "a", 2, "a", "a", 1, Collections.emptySet());
        EntityInfo.Access access3 = new EntityInfo.Access("a", "a", 3, "a", "a", 1, Collections.emptySet());
        access2.setPriorVersion(access1);
        access3.setPriorVersion(access2);

        assertThrows(IllegalArgumentException.class, () -> access1.setPriorVersion(access3));


    }

    @Test
    void testEntityInfoIdPrefix() {
        EntityInfo a = new MockEntityInfo("a", "a", EntityType.POLICY);
        EntityInfo b = new MockEntityInfo("b", "path/to/b", EntityType.POLICY);
        EntityInfo c = new MockEntityInfo("", "", EntityType.POLICY);
        EntityInfo d = new MockEntityInfo("", "this/path/here/", EntityType.POLICY);
        EntityInfo e = new MockEntityInfo("Ee-i-ee-i-o",
                "Old MacDonald had a farm/Ee-i-ee-i-o/And on his farm he had some cows/Ee-i-ee-i-o",
                EntityType.POLICY);
        EntityInfo f = new MockEntityInfo("doesnt-exist-in-path", "path/to/fountain/of/youth", EntityType.ACCESS);
        assertEquals("", a.getIdPrefix());
        assertEquals("path/to/", b.getIdPrefix());
        assertEquals("", c.getIdPrefix());
        assertEquals("this/path/here/", d.getIdPrefix());
        assertEquals("Old MacDonald had a farm/Ee-i-ee-i-o/And on his farm he had some cows/", e.getIdPrefix());
        assertEquals("", f.getIdPrefix());

    }

    @Test
    void testLocationAgnostic_VersionHash() {
        EntityInfo a = new EntityInfo.Policy(
                new PolicyDefinition("d/c/b/test_policy/1.0", "c/b/test_policy", "test_policy", 1, 0, 0),
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("d/c/b/test_policy/1.0/process/test_policy.xml", "a"),
                        new ConfigStoreItemInfo("d/c/b/test_policy/1.0/policy-metadata.json", "b")
                )));

        EntityInfo b = new EntityInfo.Policy(
                new PolicyDefinition("c/d/e/f/g/test_policy/1.0/4", "f/g/test_policy", "test_policy", 1, 0, 4),
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("c/d/e/f/g/test_policy/1.0/4/process/test_policy.xml", "a"),
                        new ConfigStoreItemInfo("c/d/e/f/g/test_policy/1.0/4/policy-metadata.json", "b")
                )));

        EntityInfo c = new EntityInfo.Policy(
                new PolicyDefinition("e/f/g/test_policy/1.1", "e/f/g/test_policy", "test_policy", 1, 1, 0),
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("e/f/g/test_policy/1.1/process/test_policy.xml", "a"),
                        new ConfigStoreItemInfo("e/f/g/test_policy/1.1/policy-metadata.json", "b")
                )));


        assertEquals(a.getVersion(), b.getVersion());
        assertNotEquals(a.getId(), b.getId());
        assertEquals(a.getVersion(), c.getVersion());
        assertNotEquals(a.getId(), c.getId());

    }

    @Test
    void testPolicyEntity_getters() {
        EntityInfo.Policy policyA = new EntityInfo.Policy(new PolicyDefinition("lob/policy_a/2.0"), Collections.emptySet());
        assertEquals("policy_a", policyA.getPolicyShortName());
        assertEquals("/lob/policy_a", policyA.getPolicyFullName());
        assertEquals("2.0", policyA.getPolicyVersion());
        assertEquals(0, policyA.getPolicyMinorVersion());
        assertEquals(2, policyA.getPolicyMajorVersion());
    }

    public static class MockEntityInfo extends EntityInfo {

        final int minorVersion;
        final int majorVersion;

        MockEntityInfo(String entityId, EntityType entityType, ConfigStoreItemInfo... items) {
            super(entityId, entityId, entityType,
                    new HashSet<>(Arrays.asList(items)), EntityInfo.DEFAULT_VERSION_NUMBER, null);
            this.minorVersion = 0;
            this.majorVersion = 0;
        }

        MockEntityInfo(String entityId, String entityLocation, EntityType entityType, ConfigStoreItemInfo... items) {
            super(entityId, entityLocation, entityType,
                    new HashSet<>(Arrays.asList(items)), EntityInfo.DEFAULT_VERSION_NUMBER, null);
            this.minorVersion = 0;
            this.majorVersion = 0;
        }

        MockEntityInfo(String entityId, int major, int minor, int patch) {
            super(entityId, entityId, EntityType.UNDEFINED,
                    Collections.emptySet(), patch, null);
            this.majorVersion = major;
            this.minorVersion = minor;
        }

        @Override
        public String getName() {
            return getId();
        }

        @Override
        public int getMajorVersion() {
            return majorVersion;
        }

        @Override
        public int getMinorVersion() {
            return minorVersion;
        }
    }
}
