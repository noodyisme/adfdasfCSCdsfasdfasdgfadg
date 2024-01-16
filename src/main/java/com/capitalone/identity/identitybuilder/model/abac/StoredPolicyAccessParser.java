package com.capitalone.identity.identitybuilder.model.abac;

import com.capitalone.identity.identitybuilder.model.ConfigStoreBusinessException;
import com.capitalone.identity.identitybuilder.util.ParseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Exceptions;

import java.io.IOError;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class StoredPolicyAccessParser {

    private static final String ABAC_SCHEMA_FILE = "accessControl/access-control-schema.json";

    private static StoredPolicyAccessParser parser;

    private final JsonSchema jsonSchema;

    public static StoredPolicyAccessParser getInstance() {
        if (parser == null) {
            parser = new StoredPolicyAccessParser();
        }
        return parser;
    }

    private StoredPolicyAccessParser() {
        try {
            URL resource = this.getClass().getClassLoader().getResource(ABAC_SCHEMA_FILE);
            String schema = IOUtils.toString(Objects.requireNonNull(resource), StandardCharsets.UTF_8);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
            jsonSchema = factory.getSchema(schema);
        } catch (IOException e) {
            throw new IOError(e);
        }

    }

    public StoredPolicyAccess parsePolicyAccess(String content) {
        try {
            final ObjectMapper mapper = ParseUtils.MAPPER;
            JsonNode node = mapper.readTree(content);
            Set<ValidationMessage> errorMessages = jsonSchema.validate(node);
            if (errorMessages.isEmpty()) {
                return mapper.readValue(content, StoredPolicyAccess.class);
            } else {
                throw new IllegalArgumentException(StringUtils.join(errorMessages, ", "));
            }
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ConfigStoreBusinessException("Failed to parse policy access document. " +
                    "Invalid file content does not obey abac schema.", e);
        }
    }
}
