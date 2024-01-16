package com.capitalone.identity.identitybuilder.configmanagement;

import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigManagementModelParser {
    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String REQUIRED_SCHEMA_PROPERTY_KEY = "required";
    private static final String PROPERTIES_SCHEMA_PROPERTY_KEY = "properties";
    private static final String JSON_EXTENSION = ".json";
    private static final String ENTITY_NAMESPACE_SEPARATOR = "/";
    private static final String V2 = "v2";
    private static final String DEFAULTS_SCHEMA_KEY = "defaults";
    private static final String USECASE_SCHEMA_KEY = "usecase";

    private static final String FEATURES_SCHEMA_KEY = "features";
    private static final String FEATURES_REQUIRED_SCHEMA_KEY = "features-required";
    private static final Pattern CONFIG_MANAGEMENT_USECASE_AND_PARAMETER_PATTERN = Pattern
            .compile("^[a-zA-Z0-9._-]+$");

    @SuppressWarnings("squid:S1192")
    private static final String META_SCHEMA = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"required\": [\n" +
            "    \"$schema\",\n" +
            "    \"type\",\n" +
            "    \"required\",\n" +
            "    \"additionalProperties\",\n" +
            "    \"properties\"\n" +
            "  ],\n" +
            "  \"additionalProperties\": false,\n" +
            "  \"properties\": {\n" +
            "    \"$schema\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"https://json-schema.org/draft/2019-09/schema\"]\n" +
            "    },\n" +
            "    \"type\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"object\"]\n" +
            "    },\n" +
            "    \"required\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"string\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"additionalProperties\": {\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"enum\": [\n" +
            "        false\n" +
            "      ]\n" +
            "    },\n" +
            "    \"properties\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"patternProperties\": {\n" +
            "        \".\": {\n" +
            "          \"$ref\": \"#/$defs/config-property\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"$defs\": {\n" +
            "    \"config-property\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"required\": [\n" +
            "        \"type\"\n" +
            "      ],\n" +
            "      \"properties\": {\n" +
            "        \"type\": {\n" +
            "          \"type\": \"string\",\n" +
            "          \"pattern\": \"(string|boolean|integer|number|array)\",\n" +
            "          \"description\": \"Type of config property\"\n" +
            "        },\n" +
            "        \"items\": {\n" +
            "          \"type\": \"object\",\n" +
            "          \"required\": [\n" +
            "            \"type\"\n" +
            "          ],\n" +
            "          \"properties\": {\n" +
            "            \"type\": {\n" +
            "              \"type\": \"string\",\n" +
            "              \"pattern\": \"(string|boolean|integer|number)\",\n" +
            "              \"description\": \"Type of config property\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String META_SCHEMA_V2 = "{\n" +
            "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"required\": [\n" +
            "    \"$id\",\n" +
            "    \"$defs\"\n" +
            "  ],\n" +
            "  \"additionalProperties\": false,\n" +
            "  \"properties\": {\n" +
            "    \"$id\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\"v2\"]\n" +
            "    },\n" +
            "    \"$defs\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"required\": [\n" +
            "        \"usecase\",\n" +
            "        \"defaults\"\n" +
            "      ],\n" +
            "      \"properties\": {\n" +
            "        \"usecase\": {\n" +
            "          \"type\": \"object\",\n" +
            "          \"required\": [\n" +
            "            \"type\"\n" +
            "          ]\n" +
            "        },\n" +
            "        \"features\": {\n" +
            "          \"type\": \"object\",\n" +
            "          \"required\": [\n" +
            "            \"type\"\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final JsonSchema META_JSON_SCHEMA = SCHEMA_FACTORY.getSchema(META_SCHEMA);
    private static final JsonSchema META_JSON_SCHEMA_V2 = SCHEMA_FACTORY.getSchema(META_SCHEMA_V2);

    private ConfigManagementModelParser() {
        LOGGER.debug("Schema for schemas:=" + META_SCHEMA);
    }

    public static ConfigManagementModel parse(@NonNull String location, @Nullable String defaultsContent,
                                              @Nullable String schemaContent, @NonNull Set<ConfigStoreItem> items) {
        return parse(location, defaultsContent, schemaContent, items, new HashSet<>(), null);
    }

    /**
     * This method validates the config files against the schema.json file and returns a ConfigManagementModel
     * @param location where the content is coming from
     * @param defaultsContent content of defaults file
     * @param schemaContent content of schema file
     * @param items set of use-case items that are related to default and schema
     * @param nonOverridableProperties contents of non-overridable file, properties that cannot be overwritten
     *                                 by usecases. Multiple files possible for env specific
     * @param environment environment
     * @return {@link ConfigManagementModel} if one can be created from the provided entity, or else {@code null}
     * @throws RuntimeException if config items {@link ConfigStoreItem} are malformed or cannot be parsed
     */
    public static ConfigManagementModel parse(@NonNull String location, @Nullable String defaultsContent,
                                              @Nullable String schemaContent, @NonNull Set<ConfigStoreItem> items,
                                              @Nullable Set<ConfigStoreItem> nonOverridableProperties, @Nullable String environment) {
        Optional<String> defaults = Optional.ofNullable(defaultsContent);
        Optional<String> schema = Optional.ofNullable(schemaContent);
        Map<String, String> useCases = ConfigStoreItem.getItems(items, ConfigStoreItem.Type.CONFIG_USECASE).stream()
                .collect(Collectors.toMap(item -> {
                    String[] split = item.getName().split(ENTITY_NAMESPACE_SEPARATOR);
                    return split[split.length - 1].replace(JSON_EXTENSION, Strings.EMPTY);
                }, item -> item.content));
        if(!defaults.isPresent() && useCases.isEmpty() && !schema.isPresent()) {
            return null;
        }
        try {
            if(schema.isPresent()) {
                JsonNode node = OBJECT_MAPPER.readTree(schema.get());
                String id = null != node.get("$id") ? node.get("$id").toString() : null;
                if(null != id && id.toLowerCase().contains(V2)) {
                    return defaults.map(defaultItem -> ConfigManagementModelParser.parseV2(location, schema.get(), node, defaultItem, useCases, nonOverridableProperties, environment))
                            .orElseThrow(() -> new ConfigurationManagementValidationError(
                                    String.format("Invalid configuration for entity '%s' [schema=%s, defaults=%s, usecases=%s]",
                                            location, schema.isPresent(), defaults.isPresent(), useCases.size())));
                } else {
                    return defaults.map(defaultItem -> ConfigManagementModelParser.parseV1(location, schema.get(), defaultItem, useCases))
                            .orElseThrow(() -> new ConfigurationManagementValidationError(
                                    String.format("Invalid configuration for entity '%s' [schema=%s, defaults=%s, usecases=%s]",
                                            location, schema.isPresent(), defaults.isPresent(), useCases.size())));
                }
            } else {
                throw new ConfigurationManagementValidationError(String.format("Unable to parse schema, no valid schema present for entity '%s'. ", location));
            }
        } catch (JsonProcessingException e) {
            throw new ConfigurationManagementValidationError(String.format("Unable to parse schema, invalid format for entity '%s'.", location), e);
        }
    }

    /**
     * Uses V1 of schema.json file to validate defaults file and usecases, then returns a ConfigManagementModel with both the defaults and use cases map
     * @param id descriptive identifier for the model that can be used to populate exception/error messages
     * @param schema content of schema file
     * @param defaults content of defaults file
     * @param useCases set of use-case items that are related to default and schema
     * @return {@link ConfigManagementModel} if one can be created from the provided entity, or else {@code null}
     * @throws RuntimeException if config items {@link ConfigStoreItem} are malformed or cannot be parsed
     */
    static ConfigManagementModel parseV1(@NonNull String id,
                                       @NonNull String schema,
                                       @NonNull String defaults,
                                       @NonNull Map<String, String> useCases) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(schema, () -> String.format("null 'schema' for id '%s'", id));
        Objects.requireNonNull(defaults, () -> String.format("null 'defaults' for id '%s'", id));
        Objects.requireNonNull(useCases, () -> String.format("null 'content' for id '%s'", id));

        // validate provided schema against our schema
        Map<String, Serializable> schemaObject;
        try {
            schemaObject = parseConfig(META_JSON_SCHEMA, schema, "schema");
        } catch (RuntimeException r) {
            // add ID information to any exception thrown
            throw new ConfigurationManagementValidationError.SchemaError(id, r);
        }

        // validate content of the provided schema
        @SuppressWarnings("unchecked")
        Set<String> required = new HashSet<>((List<String>) schemaObject.get(REQUIRED_SCHEMA_PROPERTY_KEY));
        @SuppressWarnings("unchecked")
        Set<String> props = ((Map<String, Object>) schemaObject.get(PROPERTIES_SCHEMA_PROPERTY_KEY)).keySet();
        if (props.containsAll(required) && required.containsAll(props)) {
            props.forEach(key -> validateKeyAgainstRegex(key, id,
                    CONFIG_MANAGEMENT_USECASE_AND_PARAMETER_PATTERN, "parameter"));
        } else {
            String msg = String.format("Schema file for id '%s' malformed. All keys under 'properties' key must " +
                    "be in 'required' list and vice versa", id);
            throw new ConfigurationManagementValidationError.SchemaError(msg);
        }

        try {
            // parse default
            JsonSchema defaultSchema = SCHEMA_FACTORY.getSchema(schema);
            ObjectNode schemaNode = (ObjectNode) defaultSchema.getSchemaNode();
            Map<String, Serializable> defaultsMap = parseConfig(defaultSchema, defaults, "defaults");

            // parse usecases
            schemaNode.remove(REQUIRED_SCHEMA_PROPERTY_KEY);
            JsonSchema useCaseSchema = SCHEMA_FACTORY.getSchema(schemaNode);
            Map<String, Map<String, Serializable>> useCaseMap = validateUseCases(useCaseSchema, useCases, id);
            return ConfigManagementModel.newInstance(defaultsMap, useCaseMap, null);
        } catch (RuntimeException r) {
            // add ID information to any exception thrown
            throw new ConfigurationManagementValidationError(
                    String.format("Exception parsing configuration for id '%s'", id), r);
        }
    }

    /**
     * Uses V2 schema to validate and parse the defaults, usecases, and non-overrideable properties. Returns a ConfigManagementModel that contains fully resolved usecases
     * @param id descriptive identifier for the model that can be used to populate exception/error messages
     * @param jsonString content of schema file
     * @param jsonNode schema file as json node
     * @param defaults content of defaults file
     * @param useCases set of use-case items that are related to default and schema
     * @param nonOverridableProperties Can be null, String version of file containing all Policy level properties
     * @param environment environment to be invoked
     * @return {@link ConfigManagementModel} if one can be created from the provided entity, or else {@code null}
     * @throws RuntimeException if config items {@link ConfigStoreItem} are malformed or cannot be parsed
     */
    private static ConfigManagementModel parseV2(@NonNull String id,
                                         @NonNull String jsonString,
                                         @NonNull JsonNode jsonNode,
                                         @NonNull String defaults,
                                         @NonNull Map<String, String> useCases,
                                         @Nullable Set<ConfigStoreItem> nonOverridableProperties,
                                         String environment) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(defaults, () -> String.format("null 'defaults' for id '%s'", id));
        Objects.requireNonNull(useCases, () -> String.format("null 'content' for id '%s'", id));

        try{
            // validate provided schema against our schema
            parseConfig(META_JSON_SCHEMA_V2, jsonString, "schema-v2");
            JsonNode fullResolved = resolveReferences(jsonNode);
            Map<String, Serializable> subSchemas = OBJECT_MAPPER.convertValue(fullResolved.get("$defs"), new TypeReference<Map<String, Serializable>>() {
            });
            //parse defaults
            String defaultsString = OBJECT_MAPPER.writeValueAsString(subSchemas.get(DEFAULTS_SCHEMA_KEY));
            JsonSchema defaultSchema = SCHEMA_FACTORY.getSchema(defaultsString);
            Map<String, Serializable> defaultsMap = parseConfig(defaultSchema, defaults, DEFAULTS_SCHEMA_KEY);

            //parse useCases
            String usecaseString = OBJECT_MAPPER.writeValueAsString(subSchemas.get(USECASE_SCHEMA_KEY));
            JsonSchema useCaseDefaultsSchemas = SCHEMA_FACTORY.getSchema(usecaseString);
            Map<String, Map<String, Serializable>> useCaseMap = validateUseCases(useCaseDefaultsSchemas, useCases, id);

            //parse non overridable properties
            Map<String, Serializable> featuresResultMap = new HashMap<>();
            nonOverridableProperties = Optional.ofNullable(nonOverridableProperties).orElse(Collections.emptySet());
            if (subSchemas.get(FEATURES_REQUIRED_SCHEMA_KEY) != null || !nonOverridableProperties.isEmpty()) {
                Map<String, String> nonOverridableConfigs = nonOverridableProperties.stream()
                        .collect(Collectors.toMap(item -> {
                    String[] split = item.getName().split(ENTITY_NAMESPACE_SEPARATOR);
                    return split[split.length - 1].replace(JSON_EXTENSION, Strings.EMPTY);
                }, item -> item.content));
                //Get the features schema to validate
                String nonOverridableString = OBJECT_MAPPER.writeValueAsString(subSchemas.get(FEATURES_REQUIRED_SCHEMA_KEY));
                JsonSchema nonOverridableSchema = SCHEMA_FACTORY.getSchema(nonOverridableString);

                //Get the main features config
                String policyLevelProperties = nonOverridableConfigs.get(FEATURES_SCHEMA_KEY);
                nonOverridableConfigs.remove(FEATURES_SCHEMA_KEY);
                featuresResultMap = parseConfig(nonOverridableSchema, policyLevelProperties, FEATURES_REQUIRED_SCHEMA_KEY);

                //get the env specific schema
                String envSpecificPolicySchemaString = OBJECT_MAPPER.writeValueAsString(subSchemas.get(FEATURES_SCHEMA_KEY));
                JsonSchema envSpecificPolicySchema = SCHEMA_FACTORY.getSchema(envSpecificPolicySchemaString);
                Map<String, Serializable> envSpecificConfig = new HashMap<>();
                //iterate through all environment-specific features.json configurations and validate
                for (Map.Entry<String, String> featuresConfig : nonOverridableConfigs.entrySet()) {
                    if (Strings.isNotBlank(environment) && featuresConfig.getKey().replaceFirst("features-", "")
                            .equals(environment.toLowerCase())) {
                        envSpecificConfig = parseConfig(envSpecificPolicySchema, featuresConfig.getValue(), FEATURES_SCHEMA_KEY);
                    } else {
                        // validate that the file is well-formed, even if it's not a match
                        parseConfig(envSpecificPolicySchema, featuresConfig.getValue(), FEATURES_SCHEMA_KEY);
                    }
                }
                //apply env specific overwrites
                featuresResultMap = mergeWithOverwriteValues(featuresResultMap, envSpecificConfig);
            }

            return ConfigManagementModel.newInstance(defaultsMap, useCaseMap, featuresResultMap);

        } catch (Exception e) {
            throw new ConfigurationManagementValidationError(
                    String.format("Exception parsing configuration for id '%s'", id), e);
        }
    }

    /**
     * Explicitly apply the schema to the content being passed. Return any errors if present
     * @param schema Explicitly apply the schema to the content being passed. Throw runtime exception for any errors if present
     * @param content content the json schema will validate
     * @param label label for error handling, only used in event of an error
     * @return the content with the schema applied
     */
    private static Map<String, Serializable> parseConfig(JsonSchema schema, String content,
                                                         @NonNull String label) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(content);
            Set<ValidationMessage> errorMessages = schema.validate(node);
            if (errorMessages.isEmpty()) {
                return OBJECT_MAPPER.convertValue(node, new TypeReference<Map<String, Serializable>>() {
                });
            } else {
                throw new IllegalArgumentException(StringUtils.join(errorMessages, ", "));
            }
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ConfigurationManagementValidationError(
                    String.format("Invalid file content '%s' does not obey provided schema.", label), e);
        }
    }

    private static void validateKeyAgainstRegex(String key, String id, Pattern pattern, String validationType) {
        if (!pattern.matcher(key).matches()) {
            throw new ConfigurationManagementValidationError(
                    String.format("Exception parsing configuration for id '%s'. A config management " + validationType +
                            " contains invalid characters. Alphanumeric characters as well as '.', '_', and '-' are " +
                            "valid characters", id));
        }
    }

    /**
     * Returns a JsonNode with all $ref fully resolved, meaning the reference has been inserted into the individual
     * node. This means sub schemas can be used independently
     * @param node JsonNode to Resolve
     * @return
     * @throws JsonProcessingException
     */
    private static JsonNode resolveReferences(JsonNode node) throws JsonProcessingException {
        JsonNode defNode = node.get("$defs");
        Iterator<JsonNode> schemas = defNode.elements();
        while(schemas.hasNext()) {
            JsonNode currentSchema = schemas.next();
            Iterator<Map.Entry<String, JsonNode>> schemaConditions =  currentSchema.fields();
            while(schemaConditions.hasNext()) {
                Map.Entry<String, JsonNode> currentSchemaCondition = schemaConditions.next();
                if(null != currentSchemaCondition.getKey() && currentSchemaCondition.getKey().equalsIgnoreCase("allOf")) {
                    currentSchemaCondition.setValue(referenceResolution(defNode, currentSchemaCondition));
                }
            }
        }

        return node;
    }

    /**
     * Resolve references within a tree node. Only used by the above private method
     * @param defNode Parent node of schemas that will be referenced to resolve
     * @param schemaCondition
     * @return
     * @throws JsonProcessingException
     */
    private static JsonNode referenceResolution(JsonNode defNode, Map.Entry<String, JsonNode> schemaCondition) throws JsonProcessingException {
        JsonNode currentCondition = schemaCondition.getValue();
        Iterator<JsonNode> properties = currentCondition.elements();
        if(null != properties && properties.hasNext()) {
            List<JsonNode> nodeList = new ArrayList<>();
            properties.forEachRemaining(nodeList::add);
            ListIterator<JsonNode> propsListIterator = nodeList.listIterator();
            StringBuilder propsStringBuilder= new StringBuilder();
            while(propsListIterator.hasNext()) {
                JsonNode refProp = propsListIterator.next();
                JsonNode val = refProp.get("$ref");
                if(null != val) {
                    String refId = val.asText();
                    JsonNode referencedSchema = defNode.get(refId);
                    if(null == referencedSchema) {
                        throw new ConfigurationManagementValidationError(
                                String.format("Unable to parse schema, invalid reference '%s'.", refId));
                    }
                    propsStringBuilder.append(referencedSchema).append(",");
                    propsListIterator.set(referencedSchema);
                } else {
                    propsStringBuilder.append(refProp).append(",");
                }
            }
            String propsString = propsStringBuilder.toString();
            return OBJECT_MAPPER.readTree("[" + propsString.substring(0,propsString.length()-1) + "]");
            }
        return currentCondition;
    }

    private static Map<String, Map<String, Serializable>> validateUseCases(JsonSchema schema, Map<String, String> useCases, String id) {
        Map<String, Map<String, Serializable>> useCaseMap = useCases.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        parseConfig(schema, entry.getValue(), entry.getKey())));

        useCaseMap.forEach((key, value) -> validateKeyAgainstRegex(key, id,
                CONFIG_MANAGEMENT_USECASE_AND_PARAMETER_PATTERN, "use case"));
        return useCaseMap;
    }

    private static Map<String, Serializable> mergeWithOverwriteValues(Map<String, Serializable> baselineValues,
                                                                      Map<String, Serializable> overrideValues) {
        Map<String, Serializable> combinedSource = new HashMap<>(baselineValues);
        combinedSource.putAll(Objects.requireNonNull(overrideValues));
        return combinedSource;
    }

}
