package com.capitalone.identity.identitybuilder.configmanagement;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Exceptions;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigManagementModelParserTest {
    private static final String EMPTY_STRING = "";
    private static Map<String, String> useCases;
    @Mock
    Entity.Policy entity;

    private String getJsonFile(String name) {
        try {
            URL resource = this.getClass().getClassLoader().getResource("configmanagement/parsing/" + name);
            return IOUtils.toString(Objects.requireNonNull(resource), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    String getUseCaseCustomerCode(String useCase) {
        String customerCode;
        switch (useCase) {
            case "useCase_A_valid":
                customerCode = "xyz";
                break;
            case EMPTY_STRING:
                customerCode = "abc";
                break;
            case "LOB.DIV.CHANNEL.APP.USECASE_B_VALID":
                customerCode = "klm";
                break;
            case "LOB.DIV.CHANNEL.APP":
                customerCode = "app";
                break;
            default:
                customerCode = null;
                break;
        }

        return customerCode;
    }

    String getUseCaseClearanceCodesString(String useCase) {
        String clearanceCodes;
        switch (useCase) {
            case "useCase_A_valid":
                clearanceCodes = "[X]";
                break;
            case EMPTY_STRING:
                clearanceCodes = "[AA, BB, CC]";
                break;
            case "LOB.DIV.CHANNEL.APP.USECASE_B_VALID":
                clearanceCodes = "[P, Q, R]";
                break;
            case "LOB.DIV.CHANNEL.APP":
                clearanceCodes = "[A, P, P]";
                break;
            default:
                clearanceCodes = null;
                break;
        }
        return clearanceCodes;
    }

    @BeforeEach
    void configureConfigStore() {
        useCases = new HashMap<>();
    }

    @ParameterizedTest
    @ValueSource(strings = {"useCase_A_valid", "LOB.DIV.CHANNEL.APP.USECASE_B_VALID", "LOB.DIV.CHANNEL.APP"})
    void parse_ok(String useCaseName) {
        String schema = getJsonFile("schema_valid.json");
        String defaults = getJsonFile("defaults_valid.json");

        useCases.put(useCaseName, getJsonFile(useCaseName + ".json"));
        ConfigManagementModel config = ConfigManagementModelParser.parseV1("1", schema, defaults, useCases);
        assertEquals(getUseCaseCustomerCode(useCaseName), config.getValueOrThrow("customer.code", useCaseName));
        Object useCaseClearanceCodes = config.getValueOrThrow("user.clearanceCodes", useCaseName);
        assertEquals(getUseCaseClearanceCodesString(useCaseName), useCaseClearanceCodes.toString());

    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void parse_nullArgs() {
        String schema = getJsonFile("schema_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        String name = "useCase_A_valid";
        String useCaseA = getJsonFile(name + ".json");
        useCases.put(name, useCaseA);

        assertThrows(NullPointerException.class,
                () -> ConfigManagementModelParser.parseV1(null, schema, defaults, useCases));

        String policyId = "abc-123";
        RuntimeException e;

        e = assertThrows(NullPointerException.class,
                () -> ConfigManagementModelParser.parseV1(policyId, null, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));

        e = assertThrows(NullPointerException.class,
                () -> ConfigManagementModelParser.parseV1(policyId, schema, null, useCases));
        assertTrue(e.getMessage().contains(policyId));

        e = assertThrows(NullPointerException.class,
                () -> ConfigManagementModelParser.parseV1(policyId, schema, defaults, (Map<String, String>) null));
        assertTrue(e.getMessage().contains(policyId));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid_extra_param.json",
            "invalid_missing_param.json",
            "invalid_null_param.json",
            "invalid_type.json",
            "invalid_format.json",
    })
    void parse_invalid_defaults(String filename) {
        String schema = getJsonFile("schema_valid.json");
        String defaults = getJsonFile(filename);
        String policyId = "abc123";
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parseV1(policyId, schema, defaults, useCases));
        assertTrue(exception.getCause().getMessage().contains("default"));
        assertTrue(exception.getMessage().contains(policyId));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid_extra_param.json",
            "invalid_null_param.json",
            "invalid_type.json",
            "invalid_format.json",
            "invalid_max.json"
    })
    void parse_invalid_usecase(String useCase) {
        String schema = getJsonFile("schema_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        useCases.put(useCase, getJsonFile(useCase));

        final String policyId = "abc123";
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parseV1(policyId, schema, defaults, useCases));
        assertTrue(exception.getCause().getMessage().contains(useCase), "Failed use case " + useCase);
        assertTrue(exception.getMessage().contains(policyId));
    }

    @Test
    void parse_invalid_business_event_chars_format() {
        String schema = getJsonFile("schema_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        useCases.put("invalid business event name.json", getJsonFile("invalid business event name.json"));
        final String policyId = "fish";
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parseV1(policyId, schema, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));
        assertTrue(e.getCause().getMessage().contains("A config management use case contains invalid characters."));
    }

        @Test
    void parse_entity() {

        HashSet<ConfigStoreItem> items = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/policy-metadata.json","{\"Status\": \"AVAILABLE\"}"),
                new ConfigStoreItem("a/b/c/1.1/config/schema.json","{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"type\": \"object\", \"required\": [], \"additionalProperties\": false, \"properties\": {}}"),
                new ConfigStoreItem("a/b/c/1.1/config/defaults.json","{}"),
                new ConfigStoreItem("a/b/c/1.1/config/useCase_A_valid.json","{}"),
                new ConfigStoreItem("a/b/c/1.1/config/LOB.DIV.CHANNEL.APP.USECASE_B_VALID.json","{}"),
                new ConfigStoreItem("a/b/c/1.1/config/LOB.DIV.CHANNEL.APP.json","{}")
        ));

        EntityInfo.Policy mockInfo = Mockito.mock(EntityInfo.Policy.class);
        when(mockInfo.getLocationPrefix()).thenReturn("abc123");

        Entity.Policy policy = assertDoesNotThrow(() -> new Entity.Policy(mockInfo, items));

        items.removeIf(item -> item.getName().startsWith("useCase_"));
        assertDoesNotThrow(policy::getConfigManagementModel);

    }

    @Test
    void parse_multiple_use_case_invalid_extra_param() {
        String schema = getJsonFile("schema_valid.json");
        String defaults = getJsonFile("defaults_valid.json");

        String invalidUseCase = "invalid_extra_param.json";
        useCases.put("useCase_A_valid.json", getJsonFile("useCase_A_valid.json"));
        useCases.put(invalidUseCase, getJsonFile(invalidUseCase));

        final String policyId = "abc123";
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parseV1(policyId, schema, defaults, useCases));
        assertTrue(exception.getCause().getMessage().contains(invalidUseCase), "Failed use case " + invalidUseCase);
        assertTrue(exception.getMessage().contains(policyId));
    }

    @Test
    void parse_entity_empty() {
        ConfigManagementModel model = ConfigManagementModelParser.parse("location", null, null, Collections.emptySet());
        assertNull(model);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "}", "[]", "",})
    @NullSource
    void parse_entity_noSchema_throws(String schema) {

        String location = "a/b/c/1.1/";
        String defaultvalid = "{}";
        Set<ConfigStoreItem> useCase = Collections.singleton(
                new ConfigStoreItem("a/b/c/1.1/config/usecaseA.json", "{}")
        );

        RuntimeException e = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parse(location, defaultvalid, schema, useCase));
        assertTrue(e.getMessage().contains(location));

    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "}", "[]", "",})
    @NullSource
    void parse_entity_noDefaults_throws(String defaultContent) {
        String policyFullLocation = "a/b/c/1.1/";
        String schemaValid = "{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"type\": \"object\", \"required\": [], \"additionalProperties\": false, \"properties\": {}}";
        Set<ConfigStoreItem> useCase = Collections.singleton(
                new ConfigStoreItem("a/b/c/1.1/config/usecaseA.json", "{}")
        );

        RuntimeException e = assertThrows(ConfigurationManagementValidationError.class,
                () -> ConfigManagementModelParser.parse(policyFullLocation,
                        defaultContent, schemaValid, useCase));
        assertTrue(e.getMessage().contains(policyFullLocation));

    }

    @Test
    void parse_entity_useCaseOnly_throws() {

        String policyFullLocation = "a/b/c/1.1/";
        Set<ConfigStoreItem> useCase = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/usecaseA.json", "{}"),
                new ConfigStoreItem("a/b/c/1.1/config/usecaseB.json", "{}")
        ));

        RuntimeException e = assertThrows(ConfigurationManagementValidationError.class,
                () -> ConfigManagementModelParser.parse(policyFullLocation,
                        null, null, useCase));
        assertTrue(e.getMessage().contains(policyFullLocation));
    }

    @Test
    void parse_invalid_schema_structure() {
        String schema = "[]";
        String defaults = getJsonFile("defaults_valid.json");
        final String policyId = "fox";
        Throwable e = assertThrows(ConfigurationManagementValidationError.SchemaError.class,
                () -> ConfigManagementModelParser.parseV1(policyId, schema, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "question?mark",
            "customer code",
    })
    void parse_invalid_schema_param_chars_format(String invalid) {
        final String validPropertyName = "customer.code";
        final String schemaFormat = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"required\": [ \"%s\"],\n" +
                "  \"additionalProperties\": false,\n" +
                "  \"properties\": {\n" +
                "    \"%s\": {\n" +
                "      \"type\": \"string\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // success case
        final String policyId = "fox";
        final String defaults = "{\"customer.code\": \"test\"}";
        String validSchema = String.format(schemaFormat, validPropertyName, validPropertyName);
        assertDoesNotThrow(() -> ConfigManagementModelParser.parseV1(policyId, validSchema, defaults, useCases));

        // error case
        String invalidSchema = String.format(schemaFormat, invalid, invalid);
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parseV1(policyId, invalidSchema, "{ }", useCases));
        assertTrue(e.getMessage().contains(policyId));
        assertTrue(e.getMessage().contains("A config management parameter contains invalid characters."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  \"$schema\": \"https://json-schema.org/draft/2012-12/schema\",\n",
            "",
    })
    void parse_invalid_schema_enforceVersion(String invalid) {

        // Prevent use of schema version other than 2019-09 (supported by this library which has no vulns)

        final String validProperty = "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n";
        final String schemaFormat = "{\n" +
                "  %s " +
                "  \"type\": \"object\",\n" +
                "  \"required\": [ ],\n" +
                "  \"additionalProperties\": false,\n" +
                "  \"properties\": { }\n" +
                "}";

        final String defaults = "{}";
        final String policyId = "fox";
        String validSchema = String.format(schemaFormat, validProperty);
        assertDoesNotThrow(() -> ConfigManagementModelParser.parseV1(policyId, validSchema, defaults, useCases));

        String invalidSchema = String.format(schemaFormat, invalid);
        Exception e = assertThrows(ConfigurationManagementValidationError.SchemaError.class,
                () -> ConfigManagementModelParser.parseV1(policyId, invalidSchema, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  \"type\": \"string\",\n",
            "  \"type\": \"array\",\n",
            "  \"type\": \"null\",\n",
            "  \"type\": \"custom\",\n",
            "  \"type\": \"integer\",\n",
            "  \"type\": \"number\",\n",
            "  \"type\": \"boolean\",\n",
            "",
    })
    void parse_invalid_schema_enforceBaseType(String invalid) {

        // Ensure the schema 'type' field is set to 'object'. This would be caught during
        // parsing business data from the default file, but flags the issue earlier.

        final String validProperty = "  \"type\": \"object\",\n";
        final String schemaFormat = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                "  %s " +
                "  \"required\": [ ],\n" +
                "  \"additionalProperties\": false,\n" +
                "  \"properties\": { }\n" +
                "}";

        final String defaults = "{}";
        final String policyId = "fox";
        String validSchema = String.format(schemaFormat, validProperty);
        assertDoesNotThrow(() -> ConfigManagementModelParser.parseV1(policyId, validSchema, defaults, useCases));

        String invalidSchema = String.format(schemaFormat, invalid);
        Exception e = assertThrows(ConfigurationManagementValidationError.SchemaError.class,
                () -> ConfigManagementModelParser.parseV1(policyId, invalidSchema, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  \"additionalProperties\": true,\n",
            "",
    })
    void parse_invalid_schema_enforceAdditionalPropertiesNotAllowed(String invalid) {

        /*
        additionalProperties must always be set to false. Otherwise, default file could
        contain unspecified properties of an unsupported type like null or object. Additional
        properties could also change type from one version to another, which is not permitted.
         */

        final String validProperty = "  \"additionalProperties\": false,\n";
        final String schemaFormat = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"required\": [ ],\n" +
                "%s" +
                "  \"properties\": { }\n" +
                "  }\n" +
                "}";

        final String defaults = "{ }";
        final String policyId = "fox";

        String validSchema = String.format(schemaFormat, validProperty);
        assertDoesNotThrow(() -> ConfigManagementModelParser.parseV1(policyId, validSchema, defaults, useCases));

        String invalidSchema = String.format(schemaFormat, invalid);
        Exception e = assertThrows(ConfigurationManagementValidationError.SchemaError.class,
                () -> ConfigManagementModelParser.parseV1(policyId, invalidSchema, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"required\": [\"customer.extra\"],", // extra case
            "\"required\": [ ],", // missing case
            "", // not defined / missing case
    })
    void parse_invalid_schema_enforcedRequiredListMatchesPropertyKeys(String invalid) {

        /*
        All properties under the 'properties' key must be set in the 'required' array, and vice versa.
        1. If a required property isn't specified, then default could set it to any type
        2. If a defined property isn't required, then default value could be added without a schema change.
         */

        final String validProperty = "  \"required\": [\"customer.code\"],\n";
        final String schemaFormat = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                "  \"type\": \"object\",\n" +
                " %s\n" +
                "  \"additionalProperties\": false,\n" +
                "  \"properties\": {\n" +
                "    \"customer.code\": {\n" +
                "      \"type\": \"string\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        final String policyId = "fox";

        final String defaults = "{\"customer.code\": \"test\"}";
        String validSchema = String.format(schemaFormat, validProperty);
        assertDoesNotThrow(() -> ConfigManagementModelParser.parseV1(policyId, validSchema, defaults, useCases));

        String invalidSchema = String.format(schemaFormat, invalid);
        Exception e = assertThrows(ConfigurationManagementValidationError.SchemaError.class,
                () -> ConfigManagementModelParser.parseV1(policyId, invalidSchema, defaults, useCases));
        assertTrue(e.getMessage().contains(policyId));

    }


    @ParameterizedTest
    @ValueSource(strings = {
            "", // type missing case
            "\"type\": \"null\"", // null case
            "\"type\": \"object\"", // object case
            "\"type\": [\"object\"]", // object array
            "\"type\": [\"null\"]", // null array
            "\"type\": [ ]", // empty array case
            "\"type\": [\"string\", \"integer\"]", // multiple types case
            "\"type\": \"custom\"", // custom case
            "\"type\": [\"custom\"]", // custom multiple case
            "\"type\": \"array\", \"items\": { \"type\": \"null\" }", // enforce inside arrays, too
            "\"type\": \"array\", \"items\": { \"type\": \"object\" }",
            "\"type\": \"array\", \"items\": { \"type\": \"array\" }",
            "\"type\": \"array\", \"items\": { \"type\": \"custom\" }",
            "\"type\": \"array\", \"items\": { \"type\": [ ] }",
            "\"type\": \"array\", \"items\": { \"type\": [\"null\"] }",
            "\"type\": \"array\", \"items\": { \"type\": [\"object\"] }",
            "\"type\": \"array\", \"items\": { \"type\": [\"array\"] }",
            "\"type\": \"array\", \"items\": { \"type\": [\"custom\"] }",
            "\"type\": \"array\", \"items\": { \"type\": [\"string\", \"integer\"] }",
    })
    void parse_invalid_schema_enforcePropertyTypes(String invalid) {

        /*
         Properties under the 'properties' key are restricted to integer, number, string, and boolean types.
         Arrays of those types are allowed by the specification, too, but only one level deep.
         Object, null, and custom types are not allowed.
        */

        final String schemaFormat = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"required\": [\"customer.code\"],\n" +
                "  \"additionalProperties\": false,\n" +
                "  \"properties\": {\n" +
                "    \"customer.code\": {\n" +
                " %s " +
                "    }\n" +
                "  }\n" +
                "}";

        final String policyId = "fox";
        /* Check Single Property */

        // Success case to ensure failure is caused by invalid arg
        final String validSchema = String.format(schemaFormat, "\"type\": \"string\"\n");
        final String defaults = "{\"customer.code\": \"test\"}";
        assertDoesNotThrow(() -> ConfigManagementModelParser.parseV1(policyId, validSchema, defaults, useCases));

        // Error Case
        String invalidSchema = String.format(schemaFormat, invalid);
        // check for a schema error, specifically, not caused by the bad default arg is wrong.
        Exception e = assertThrows(ConfigurationManagementValidationError.SchemaError.class,
                () -> ConfigManagementModelParser.parseV1(policyId, invalidSchema, "{ }", useCases),
                "This schema should have failed but didn't " + invalidSchema);
        assertTrue(e.getMessage().contains(policyId));

    }

    @Test
    void parse_v2_schema_success() {
        String schema = getJsonFile("schema_v2_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> useCases = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/useCase_a.json", "{\"user.clearanceCodes\": [ \"X\" ],\"dash-incredible_\": \"test\"}")
        ));
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json","{\"global.property\":\"xyz\",\"non-overrideable-bool\":false}")
        ));
        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null));
        ConfigManagementModel config = ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null);
        //value from defaults is present
        assertEquals("abc", config.getValueOrThrow("customer.code", "useCase_a"));
        //New property from overrides
        assertEquals("xyz", config.getValueOrThrow("global.property", "useCase_a"));
        //Usecase value as expected
        assertEquals("test", config.getValueOrThrow("dash-incredible_", "useCase_a"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "}", "[]", "",})
    @NullSource
    void parse_malformed_v2_schema(String schema) {
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> useCases = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/useCase_a.json", "{\"user.clearanceCodes\": [ \"X\" ],\"dash-incredible_\": \"test\"}")
        ));
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json","{\"global.property\":\"xyz\",\"non-overrideable-bool\":false}")
        ));
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null));
        assertTrue(exception.getMessage().contains("location"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "}", "[]", ""})
    void parse_malformed_v2_override_content(String nonOverridableString) {
        String schema = getJsonFile("schema_v2_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> useCases = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/useCase_a.json", "{\"user.clearanceCodes\": [ \"X\" ],\"dash-incredible_\": \"test\"}")
        ));
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json",nonOverridableString)
        ));
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null));
        assertTrue(exception.getMessage().contains("location"));
    }

    @Test
    void parse_conflicting_v2_schemas() {
        //An existing property is redefined
        String schema = "{\"$id\":\"/v2\",\"$defs\":{\"usecase\":{\"$schema\":\"https://json-schema.org/draft/2019-09/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"document.check\":{\"type\":\"boolean\"}}},\"usecase-required\":{\"allOf\":[{\"$ref\":\"usecase\"},{\"required\":[\"document.check\"]}]},\"non-overrideable\":{\"$schema\":\"https://json-schema.org/draft/2019-09/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"document.check\":{\"type\":\"string\"}}},\"non-overrideable-required\":{\"allOf\":[{\"$ref\":\"non-overrideable\"},{\"required\":[\"document.check\"]}]}}}";
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> useCases = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/useCase_a.json", "{\"user.clearanceCodes\": [ \"X\" ],\"dash-incredible_\": \"test\"}")
        ));
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json","{\"global.property\":\"xyz\",\"non-overrideable-bool\":false}")
        ));
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null));
        assertTrue(exception.getMessage().contains("location"));
    }

    @Test
    void parse_usecase_contains_override_fail() {
        String schema = getJsonFile("schema_v2_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> useCases = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/useCase_a.json", "{\"user.clearanceCodes\": [ \"X\" ],\"dash-incredible_\": \"test\", \"global.property\": \"abc\"}")
        ));
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json","{\"global.property\":\"xyz\",\"non-overrideable-bool\":false}")
        ));
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () ->
                ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null));
        assertTrue(exception.getCause().getCause().getMessage().contains("global.property"));
    }

    @Test
    void parse_v2_no_usecase() {
        String schema = getJsonFile("schema_v2_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json","{\"global.property\":\"xyz\",\"non-overrideable-bool\":false}")
        ));
        Set<ConfigStoreItem> useCases = new HashSet<>();

        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null));
        ConfigManagementModel config = ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, null);
        assertEquals("abc", config.getValueOrThrow("customer.code", "useCase_a"));
        assertEquals("xyz", config.getDefaults().get("global.property"));
    }

    @Test
    void parse_v2_schema_env_override_success() {
        String schema = getJsonFile("schema_v2_valid.json");
        String defaults = getJsonFile("defaults_valid.json");
        Set<ConfigStoreItem> useCases = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/useCase_a.json", "{\"user.clearanceCodes\": [ \"X\" ],\"dash-incredible_\": \"test\"}")
        ));
        Set<ConfigStoreItem> nonOverridable = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.1/config/features.json","{\"global.property\":\"xyz\",\"non-overrideable-bool\":false}"),
                new ConfigStoreItem("a/b/c/1.1/config/features-qa.json","{\"global.property\":\"abc\",\"non-overrideable-bool\":true}"),
                new ConfigStoreItem("a/b/c/1.1/config/features-123.json","{\"global.property\":\"abc\",\"non-overrideable-bool\":true}"),
                new ConfigStoreItem("a/b/c/1.1/config/features-dev.json","{\"global.property\":\"123\",\"non-overrideable-bool\":true}")
        ));
        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, "QA"));
        ConfigManagementModel config = ConfigManagementModelParser.parse("location", defaults, schema, useCases, nonOverridable, "QA");
        //value from defaults is present
        assertEquals("abc", config.getValueOrThrow("customer.code", "useCase_a"));
        //New property from overrides
        assertEquals("abc", config.getValueOrThrow("global.property", "useCase_a"));
        assertNotEquals("123", config.getValueOrThrow("global.property", "useCase_a"));
        //Usecase value as expected
        assertEquals("test", config.getValueOrThrow("dash-incredible_", "useCase_a"));
    }

    @Test
    void parse_v2_success() {
        String schema = getJsonFile("simple_schema_v2.json");
        Set<ConfigStoreItem> features = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"));
        String defaults = "{\"propertyAKey\":\"defaultA\" }";
        String usecase = "{\"propertyAKey\":\"usecaseA\" }";

        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));
        ConfigManagementModel result = ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null);

        HashMap<String, String> expectedFeatures = new HashMap<String, String>() {{
            put("featureXKey", "featureX");
        }};
        HashMap<String, String> expectedDefaults = new HashMap<String, String>() {{
            put("featureXKey", "featureX");
            put("propertyAKey", "defaultA");
        }};
        HashMap<String, String> expectedUsecaseA = new HashMap<String, String>() {{
            put("featureXKey", "featureX");
            put("propertyAKey", "usecaseA");
        }};
        assertEquals(expectedFeatures, result.getFeaturesMap());
        assertEquals(expectedDefaults, result.getDefaults());
        Map<String, Serializable> usecaseAResult = result.getConfiguration("usecase_a", MatchingStrategies.MATCH_EXACT_ONLY).orElse(null);
        assertEquals(expectedUsecaseA, usecaseAResult);

    }

    @Test
    void parse_v2_error_schema_conflict() {
        String schema = getJsonFile("simple_schema_v2_conflict.json");
        Set<ConfigStoreItem> features = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"));
        String defaults = "{\"propertyAKey\":\"defaultA\", \"featureXKey\":\"featureX\"}";
        String usecase = "{\"propertyAKey\":\"usecaseA\" }";

        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class, () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null));
        assertEquals("Policy Level config cannot be present in usecase or defaults: featureXKey", exception.getCause().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // missing/incorrect/invalid features
            "[ ]",
            "{ }",
            "{\"featureZKey\":\"featureX\"}",
            "{\"featureXKey\": true }",
            ""
    })
    void parse_v2_features_errors(String feature) {
        String schema = getJsonFile("simple_schema_v2.json");
        String defaults = "{\"propertyAKey\":\"defaultA\" }";
        String usecase = "{ }";
        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));
        Set<ConfigStoreItem> features = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", feature));

        assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null));

        Set<ConfigStoreItem> successCase = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"));
        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, successCase, null));

    }

    @Test
    void parse_v2_features_null() {
        String schema = getJsonFile("simple_schema_v2.json");
        String defaults = "{\"propertyAKey\":\"defaultA\" }";
        String usecase = "{ }";
        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));

        Set<ConfigStoreItem> nullFeatureArg = null;
        assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, nullFeatureArg, null));

        Set<ConfigStoreItem> emptyFeatureArg = Collections.emptySet();
        assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, emptyFeatureArg, null));

        Set<ConfigStoreItem> successCase = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"));
        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, successCase, null));

    }

    @ParameterizedTest
    @CsvSource(value = {
            "nil::featureX",
            "dev::featureX-dev",
            "DEV::featureX-dev",
            "qa::featureX-qa",
            "qa-w::featureX-qa-w",
            "w-qa-w::featureX-w-qa-w",
            "w-qa::featureX-w-qa",
            "devint-test::featureX-devint-test",
            "random::featureX",
            "::featureX",
    }, delimiterString = "::")
    void parse_v2_features_env_variants(String env, String expectedProperty) {
        String schema = getJsonFile("simple_schema_v2.json");
        String defaults = "{\"propertyAKey\":\"defaultA\" }";
        String usecase = "{ }";
        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));
        Set<ConfigStoreItem> features = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"),
                new ConfigStoreItem("abc/1.0/config/features-dev.json", "{\"featureXKey\":\"featureX-dev\"}"),
                new ConfigStoreItem("abc/1.0/config/features-qa.json", "{\"featureXKey\":\"featureX-qa\"}"),
                new ConfigStoreItem("abc/1.0/config/features-qa-w.json", "{\"featureXKey\":\"featureX-qa-w\"}"),
                new ConfigStoreItem("abc/1.0/config/features-w-qa-w.json", "{\"featureXKey\":\"featureX-w-qa-w\"}"),
                new ConfigStoreItem("abc/1.0/config/features-w-qa.json", "{\"featureXKey\":\"featureX-w-qa\"}"),
                new ConfigStoreItem("abc/1.0/config/features-devint-test.json", "{\"featureXKey\":\"featureX-devint-test\"}"),
                new ConfigStoreItem("abc/1.0/config/features-nil.json", "{ }")
        ));

        Map<String, Serializable> defaultResult = ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null).getFeaturesMap();
        assertEquals("featureX", defaultResult.get("featureXKey"));
        Map<String, Serializable> envResult = ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, env).getFeaturesMap();
        assertEquals(expectedProperty, envResult.get("featureXKey"));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "[]",
            "{\"propertyAKey\": false }",
            "{\"propertyZKey\":\"defaultA\" }",
            "{\"featureXKey\": \"invalidOverrid\", \"propertyAKey\": \"defaultA\" }",
    })
    @NullAndEmptySource
    void parse_v2_defaults_errors(String defaults) {
        String schema = getJsonFile("simple_schema_v2.json");
        Set<ConfigStoreItem> features = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"));
        String usecase = "{}";
        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));

        assertThrows(RuntimeException.class, () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null));

        String successCase = "{\"propertyAKey\":\"defaultA\" }";
        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("ParserTest", successCase, schema, useCases, features, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"propertyAKey\": true }",
            "{\"propertyZKey\":\"usecaseA\" }",
            "{\"featureXKey\":\"usecaseA\" }",
            "[]",
    })
    void parse_v2_usecase_errors(String usecase) {
        String schema = getJsonFile("simple_schema_v2.json");
        String defaults = "{\"propertyAKey\":\"defaultA\" }";
        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));
        Set<ConfigStoreItem> features = Collections.singleton(new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"));

        assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null));

        String successUsecase = "{\"propertyAKey\":\"usecaseA\" }";
        Set<ConfigStoreItem> useCaseSuccess = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", successUsecase));
        assertDoesNotThrow(() -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCaseSuccess, features, null));

    }

    @ParameterizedTest
    @CsvSource(value = {
            "qa::{\"featureYKey\":\"featureY-value\"}", // invalid key
            "qa::{\"featureXKey\": false }", // invalid value
    }, delimiterString = "::")
    void parse_v2_features_env_parse_errors(String filePostfix, String envContent) {
        String schema = getJsonFile("simple_schema_v2.json");
        String defaults = "{\"propertyAKey\":\"defaultA\" }";
        String usecase = "{ }";
        Set<ConfigStoreItem> useCases = Collections.singleton(new ConfigStoreItem("abc/1.0/config/usecase_a.json", usecase));
        Set<ConfigStoreItem> features = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("abc/1.0/config/features.json", "{\"featureXKey\":\"featureX\"}"),
                new ConfigStoreItem("abc/1.0/config/features-dev.json", "{\"featureXKey\":\"featureX-dev\"}"),
                new ConfigStoreItem("abc/1.0/config/features-" + filePostfix + ".json", envContent)
        ));

        assertThrows(RuntimeException.class,
                () -> ConfigManagementModelParser.parse("ParserTest", defaults, schema, useCases, features, null));

    }

}
