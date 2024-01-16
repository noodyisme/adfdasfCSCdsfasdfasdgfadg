package com.capitalone.identity.identitybuilder.model.parsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
class PolicyDefinitionTest {

    @Test
    void testConstruct_ok() {
        PolicyDefinition definition = new PolicyDefinition("lob/test", "2.0");
        assertEquals("lob/test/2.0", definition.getPolicyLocation());
        assertEquals("/lob/test/2.0", definition.getPolicyFullNameWithVersion());
        assertEquals("test", definition.getPolicyShortName());
        assertEquals("2.0", definition.getPolicyVersion());
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "policy_a:1.0",
            "/policy_a:2.4",
            "test/policy_a:0.0",
            "/t/test/policy_a:0.0",
    })
    void testConstruct_name_version_parse(String vals) {
        String[] args = vals.split(":");
        String path = args[0];
        String version = args[1];
        int major = Integer.parseInt(version.split("\\.")[0]);
        int minor = Integer.parseInt(version.split("\\.")[1]);

        PolicyDefinition test = new PolicyDefinition(path, version);

        assertEquals(String.format("%s/%s", path, version), test.getPolicyLocation());
        assertEquals(version, test.getPolicyVersion());
        assertEquals("policy_a", test.getPolicyShortName());
        assertEquals(minor, test.getPolicyMinorVersion());
        assertEquals(major, test.getPolicyMajorVersion());
    }

    @Test
    void testConstructor_nullArgs() {
        assertThrows(NullPointerException.class, () -> new PolicyDefinition(null, "0.0"));
        assertThrows(NullPointerException.class, () -> new PolicyDefinition("test", null));
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "/1/2/3/4/a/b/c/d",
            "/a/b/c/d",
            "a/b/c/d",
            "//b/c/d",
            "/b/c/d",
            "b/c/d",
            "///c/d",
            "//c/d",
            "/c/d",
            "c/d",
            "////d",
            "///d",
            "//d",
            "/d",
            "d",
    })
    void testConstructor_namespace_parsing(String policyLocation) {

        PolicyDefinition policy = new PolicyDefinition(policyLocation, "1.0");

        assertEquals("d", policy.getPolicyShortName());
        assertEquals(policyLocation + "/1.0", policy.getPolicyLocation());
        assertEquals("1.0", policy.getPolicyVersion());
        String policyFullName = policy.getPolicyFullName();
        String[] fullName = policyFullName.split("/");

        assertEquals(3, fullName.length);
        assertEquals("d", fullName[2]);
        assertTrue(fullName[1].isEmpty() || fullName[1].equals("c"));
        assertTrue(fullName[0].isEmpty() || fullName[0].equals("b"));

        String policyNamespace = policy.getPolicyFullNameWithVersion();
        assertEquals(policyFullName + "/1.0", policyNamespace);

    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "policy_a/",
            "/policy_a/",
            "abc/policy_a/",
            " ",
            "/ / ",
    })
    @EmptySource
    void testConstructor_invalidLocation(String location) {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDefinition(location, "1.0"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "0",
            "1",
            "1.0.0",
            "A",
            "-1.0",
            "1.-9",
    })
    @EmptySource
    void testConstructor_invalidVersions(String version) {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDefinition("/test", version));
    }

    @Test
    void testEquals() {
        PolicyDefinition specA = new PolicyDefinition("test/abc", "0.0");
        PolicyDefinition specB = new PolicyDefinition("test/abc", "0.0");
        PolicyDefinition specC = new PolicyDefinition("test/abc/0.0");
        assertEquals(specA, specB);
        assertEquals(specA, specC);
        assertEquals(specA.hashCode(), specB.hashCode());
        assertEquals(specA.hashCode(), specC.hashCode());
    }

    @Test
    void testNotEquals() {
        PolicyDefinition specA = new PolicyDefinition("test", "0.0");
        PolicyDefinition specB = new PolicyDefinition("testB", "0.0");
        PolicyDefinition specC = new PolicyDefinition("test", "0.1");
        assertNotEquals(specA, specB);
        assertNotEquals(specA, specC);
        assertNotEquals(specA.hashCode(), specB.hashCode());
        assertNotEquals(specA.hashCode(), specC.hashCode());
    }

}
