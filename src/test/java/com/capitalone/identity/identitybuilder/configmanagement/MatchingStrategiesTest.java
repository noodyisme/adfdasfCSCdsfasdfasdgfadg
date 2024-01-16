package com.capitalone.identity.identitybuilder.configmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatchingStrategiesTest {

    @ParameterizedTest
    @ValueSource(strings = "testUseCase")
    @NullAndEmptySource
    void testMatchDefault(String useCase) {
        Map<String, Serializable> defaults = Collections.singletonMap("property-A", "Default");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        useCases.put("a", new HashMap<>());

        assertEquals(defaults, MatchingStrategies.MATCH_DEFAULT_ONLY.getConfig(useCase, useCases, defaults, null));

    }

    @Test
    void testMatchExact() {
        Map<String, Serializable> defaults = Collections.singletonMap("property-A", "Default");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();

        Map<String, Serializable> useCaseAProperties = Collections.singletonMap("property-A", "Use Case A");
        useCases.put("a.b.c.d.e", useCaseAProperties);

        final ConfigMatchingStrategy strategy = MatchingStrategies.MATCH_EXACT_ONLY;
        assertEquals(useCaseAProperties, strategy.getConfig("a.b.c.d.e", useCases, defaults, null));
        assertNull(strategy.getConfig(null, useCases, defaults, null));
        assertNull(strategy.getConfig("b", useCases, defaults, null));
        assertNull(strategy.getConfig("a.b.c", useCases, defaults, null));
        assertNull(strategy.getConfig("a.b.c.d", useCases, defaults, null));
        assertNull(strategy.getConfig("a.b.c.d.z", useCases, defaults, null));

    }

    @Test
    void testAppLevelMatchExact() {
        Map<String, Serializable> defaults = Collections.singletonMap("property-A", "Default");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();

        Map<String, Serializable> useCaseAProperties = Collections.singletonMap("property-A", "Use Case A");
        useCases.put("a.b.c.d", useCaseAProperties);

        final ConfigMatchingStrategy strategy = MatchingStrategies.MATCH_APP_LEVEL_ONLY;
        assertEquals(useCaseAProperties, strategy.getConfig("a.b.c.d.e", useCases, defaults, null));
        assertEquals(useCaseAProperties, strategy.getConfig("a.b.c.d", useCases, defaults, null));
        assertNull(strategy.getConfig("a.b.c", useCases, defaults, null));
        assertNull(strategy.getConfig(null, useCases, defaults, null));
        assertNull(strategy.getConfig("b", useCases, defaults, null));

    }

    @Test
    void testAllNonNullMatchInPriorityOrder() {
        Map<String, Serializable> defaults = Collections.singletonMap("property-A", "Default");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        Map<String, Serializable> appLevelProperties = Collections.singletonMap("property-A", "AppLevel");
        Map<String, Serializable> useCaseAProperties = Collections.singletonMap("property-A", "Use Case A");
        useCases.put("a.b.c.d", appLevelProperties);
        useCases.put("a.b.c.d.e", useCaseAProperties);

        final ConfigMatchingStrategy strategy = MatchingStrategies.MATCH_ALL_NON_NULL;
        assertEquals(useCaseAProperties, strategy.getConfig("a.b.c.d.e", useCases, defaults, null));
        assertEquals(appLevelProperties, strategy.getConfig("a.b.c.d.z", useCases, defaults, null));
        assertEquals(appLevelProperties, strategy.getConfig("a.b.c.d", useCases, defaults, null));
        assertEquals(defaults, strategy.getConfig("xyz", useCases, defaults, null));
        assertEquals(defaults, strategy.getConfig(null, useCases, defaults, null));
    }

    @ParameterizedTest
    @ValueSource(strings = "testUseCase")
    @NullAndEmptySource
    void testMatchFeatures(String useCase) {
        Map<String, Serializable> defaults = Collections.singletonMap("property-A", "Default");
        Map<String, Serializable> features = Collections.singletonMap("policy-level", "abc");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        useCases.put("a", new HashMap<>());

        assertEquals(features, MatchingStrategies.MATCH_FEATURES_ONLY.getConfig(useCase, useCases, defaults, features));

    }

}
