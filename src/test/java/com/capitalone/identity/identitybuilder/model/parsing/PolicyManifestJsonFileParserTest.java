package com.capitalone.identity.identitybuilder.model.parsing;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PolicyManifestJsonFileParserTest {

    PolicyManifestJsonFileParser parser = new PolicyManifestJsonFileParser();

    @Test
    void parseOK() throws PolicyManifestJsonFileParser.ManifestProcessingException {
        String validMultiple = "{\"unknown_property\": \"abc\", \"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": \"ACTIVE\"}, {\"Version\": \"3.0\",\"Status\": \"INACTIVE\"}, {\"Version\": \"4.0\",\"Status\": \"Active\"}]}";
        String prefix = "/us_consumers/";
        Set<PolicyDefinition> specifications = parser.parseVersions(prefix + "manifest.json", validMultiple);
        assertEquals(3, specifications.size());
        specifications.forEach(spec -> assertEquals(prefix, spec.getPolicyLocation().substring(0, prefix.length())));

    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "test",
            "/test",
            "test/test-json",
    })
    void parsePolicyBasePrefix(String location) throws PolicyManifestJsonFileParser.ManifestProcessingException {
        String validMultiple = "{\"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": \"ACTIVE\"}, {\"Version\": \"3.0\",\"Status\": \"INACTIVE\"}]}";
        Set<PolicyDefinition> specifications = parser.parseVersions(location, validMultiple);
        specifications.forEach(spec -> {
            String prefix = spec.getPolicyLocation();
            assertTrue(prefix.startsWith(location));
        });
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            " ",
            "test/",
            "/",
            "   /",
            "\t",
            "\r/",
            "\n/",
            "/ / ",
            "/ ",
    })
    @EmptySource
    void parseBadFileName(String bad) {
        String validMultiple = "{\"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": \"ACTIVE\"}, {\"Version\": \"3.0\",\"Status\": \"INACTIVE\"}]}";
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseVersions(bad, validMultiple),
                String.format("expected invalid manifest exception for path: '%s'", bad));

    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "{\"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": ACTIVE}, %s]}",
            "{}",
            "[]",
            "text",
            "",
            " ",
    })
    @NullAndEmptySource
    void parseJsonError_rootObjectProblem(String spec) {
        String filePath = "/us_consumers/manifest.json";
        try {
            parser.parseVersions(filePath, spec);
            fail("Should have failed with manifest.json content:=" + spec);
        } catch (PolicyManifestJsonFileParser.ManifestProcessingException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains(filePath), String.format("Error message '%s' should contain filepath for the following error spec: %s", e.getMessage(), spec));
        }
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "{\"Version\": \" \",\"Status\": \"ACTIVE\"}",
            "{\"Version\": null,\"Status\": \"ACTIVE\"}",
            "{\"Version\": {},\"Status\": \"ACTIVE\"}",
            "{\"Version\": [],\"Status\": \"ACTIVE\"}",
            "{\"Status\": \"ACTIVE\"}",
    })
    void parseJsonError_versionFormatProblem(String specFragment) {
        String partialInvalidSpecFormat = "{\"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": \"ACTIVE\"}, %s]}";
        String spec = String.format(partialInvalidSpecFormat, specFragment);
        String filePath = "/us_consumers/manifest.json";

        try {
            parser.parseVersions(filePath, spec);
            fail("Should have failed with manifest.json content:=" + spec);
        } catch (PolicyManifestJsonFileParser.ManifestProcessingException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains(filePath));
        }

    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "{\"Version\": \" \",\"Status\": \"ACTIVE\"}",
            "{\"Version\": null,\"Status\": \"ACTIVE\"}",
            "{\"Version\": {},\"Status\": \"ACTIVE\"}",
            "{\"Version\": [],\"Status\": \"ACTIVE\"}",
            "{\"Status\": \"ACTIVE\"}",
    })
    void parseJsonError_statusFormatProblem(String specFragment) {
        String partialInvalidSpecFormat = "{\"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": \"ACTIVE\"}, %s]}";
        String spec = String.format(partialInvalidSpecFormat, specFragment);
        String filePath = "/us_consumers/manifest.json";

        try {
            parser.parseVersions(filePath, spec);
            fail("Should have failed with manifest.json content:=" + spec);
        } catch (PolicyManifestJsonFileParser.ManifestProcessingException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains(filePath));
        }
    }

    /**
     * See {@link PolicyStatus#IN_PROGRESS}
     */
    @ParameterizedTest()
    @ValueSource(strings = {
            "{\"Version\": \"2.2\",\"Status\": \"IN_PROGRESS\"}",
            "{\"Version\": \"2.3\",\"Status\": \"IN-PROGRESS\"}",
            "{\"Version\": \"2.3\",\"Status\": \"In-progress\"}",
            "{\"Version\": \"2.3\",\"Status\": \"In_progress\"}",
            "{\"Version\": \"2.3\",\"Status\": \"ARCHIVED\"}",
    })
    void parseJsonError_statusException(String specFragment) {
        String partialInvalidSpecFormat = "{\"Versions_Supported\": [%s]}";
        String spec = String.format(partialInvalidSpecFormat, specFragment);

        String filePath = "/us_consumers/manifest.json";
        assertDoesNotThrow(() -> parser.parseVersions(filePath, spec));

    }

    @Test
    void parseRedundantVersionSpecifications() {
        String redundantSpec = "{\"Versions_Supported\": [{\"Version\": \"2.0\",\"Status\": \"ACTIVE\"}, {\"Version\": \"2.0\",\"Status\": \"INACTIVE\"}]}";
        String filePath = "/us_consumers/manifest.json";
        try {
            parser.parseVersions(filePath, redundantSpec);
            fail("should have failed due to multiple versions specified");
        } catch (PolicyManifestJsonFileParser.ManifestProcessingException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains(filePath));
        }
    }

    @ParameterizedTest()
    @MethodSource("provideSource")
    void testParserManifestScenarios(String metadataFileContent, Optional<PolicyMetadata> expectedMetadata) throws PolicyManifestParser.ManifestProcessingException {
        //act
        Optional<PolicyMetadata> parseResult = parser.parsePolicyMetadata(metadataFileContent);

        //assert
        assertEquals(expectedMetadata, parseResult);
    }

    private static Stream<Arguments> provideSource() {
        return Stream.of(
                Arguments.of(null, Optional.empty()),
                Arguments.of("{\"Status\": \"ACTIVE\"}", Optional.ofNullable(new PolicyMetadata(EntityActivationStatus.ACTIVE, 1, "ORCHESTRATION_POLICY"))),
                Arguments.of("{\"Status\": \"ACTIVE\", \"Type\": \"testType\"}", Optional.ofNullable(new PolicyMetadata(EntityActivationStatus.ACTIVE, 1, "testType")))
        );
    }
}