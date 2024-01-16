package com.capitalone.identity.identitybuilder.model.abac;

import com.capitalone.identity.identitybuilder.model.ConfigStoreBusinessException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.Exceptions;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StoredPolicyAccessParserTest {

    private String getFileContent(String name) {
        try {
            URL resource = this.getClass().getClassLoader().getResource("accessControl/" + name);
            return IOUtils.toString(Objects.requireNonNull(resource), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    final StoredPolicyAccessParser parser = StoredPolicyAccessParser.getInstance();

    @Test
    void parsePolicyAccess() {
        String fileContent = getFileContent("policy-access-valid.json");
        StoredPolicyAccess storedPolicyAccess = parser.parsePolicyAccess(fileContent);
        assertNotNull(storedPolicyAccess);
        assertNotNull(storedPolicyAccess.clients);
    }

    @Test
    void parseInvalidMissingKeys() {
        List<String> parts = new ArrayList<>(Arrays.asList(
                "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"",
                "\"schemaVersion\": \"0\"",
                "\"policyNamespace\": \"tenant/lob_entry_1/policy_a\"",
                "\"policyMajorVersion\": 1",
                "\"clients\": [{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"ALLOW\"}]"
        ));
        String valid = parts.stream().collect(Collectors.joining(", ", "{", "}"));
        assertDoesNotThrow(() -> parser.parsePolicyAccess(valid));

        for (int i = 0; i < parts.size(); i++) {
            String removed = parts.remove(i);
            String result = parts.stream().collect(Collectors.joining(", ", "{", "}"));
            assertThrows(ConfigStoreBusinessException.class, () -> parser.parsePolicyAccess(result));
            parts.add(i, removed);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"TEST\"",
            "[]",
            "{}",
            "1",
            "",

    })
    void parseInvalidSchemaProperty(String values) {
        String validArg = "\"https://json-schema.org/draft/2019-09/schema\"";
        String format = "{ \"$schema\": %s, \"schemaVersion\": \"0\", \"policyNamespace\": \"tenant/lob_entry_1/policy_a\", \"policyMajorVersion\": 1, \"clients\": [{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"ALLOW\"}]}";
        String valid = String.format(format, validArg);
        assertDoesNotThrow(() -> parser.parsePolicyAccess(valid));

        String invalid = String.format(format, values);
        assertThrows(ConfigStoreBusinessException.class, () -> parser.parsePolicyAccess(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"1\"",
            "1",
            "[]",
            "{}",
            "",
    })
    void parseInvalidSchemaVersionProperty(String values) {
        String validArg = "\"0\"";
        String format = "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"schemaVersion\": %s, \"policyNamespace\": \"tenant/lob_entry_1/policy_a\", \"policyMajorVersion\": 1, \"clients\": [{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"ALLOW\"}]}";
        String valid = String.format(format, validArg);
        assertDoesNotThrow(() -> parser.parsePolicyAccess(valid));

        String invalid = String.format(format, values);
        assertThrows(ConfigStoreBusinessException.class, () -> parser.parsePolicyAccess(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"a/b\"",
            "\"a/b/c/d\"",
            "\"/a/b/c\"",
            "\"a/b/c/\"",
            "\"a\"",
            "\"a.b\"",
            "\"a.b.c\"",
            "\"a.b.c.d\"",
            "1",
            "[]",
            "{}",
            "",
    })
    void parseInvalidPolicyNamespaceProperty(String values) {
        String validArg = "\"a/b/c\"";
        String format = "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"schemaVersion\": \"0\", \"policyNamespace\": %s, \"policyMajorVersion\": 1, \"clients\": [{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"ALLOW\"}]}";
        String valid = String.format(format, validArg);
        assertDoesNotThrow(() -> parser.parsePolicyAccess(valid));

        String invalid = String.format(format, values);
        assertThrows(ConfigStoreBusinessException.class, () -> parser.parsePolicyAccess(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            //id
            "{\"id\": null, \"environment\": \"production\", \"effect\": \"ALLOW\"}",
            "{\"environment\": \"production\", \"effect\": \"ALLOW\"}",
            //environment
            "{\"id\": \"ABC\", \"environment\": null, \"effect\": \"ALLOW\"}",
            "{\"id\": \"ABC\", \"effect\": \"ALLOW\"}",
            //effect
            "{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": null}",
            "{\"id\": \"ABC\", \"environment\": \"production\"}",
            "{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"ABC\"}",
            "{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"\"}",
            //object type
            "1",
            "[]",
            "{}",
            //malformed json
            "{",
            "}",
            "<script>alert(1)</script>"

    })
    void parseInvalidClientParamsProperty(String values) {
        String validArg = "{\"id\": \"ABC\", \"environment\": \"production\", \"effect\": \"ALLOW\"}";
        String format = "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"schemaVersion\": \"0\", \"policyNamespace\": \"a/b/c\", \"policyMajorVersion\": 1, \"clients\": [%s]}";
        String valid = String.format(format, validArg);
        assertDoesNotThrow(() -> parser.parsePolicyAccess(valid));

        String invalid = String.format(format, values);
        assertThrows(ConfigStoreBusinessException.class, () -> parser.parsePolicyAccess(invalid));
    }
}
