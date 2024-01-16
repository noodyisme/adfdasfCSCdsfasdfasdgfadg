package com.capitalone.identity.identitybuilder.model.parsing;

import com.capitalone.identity.identitybuilder.util.ParseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;

import java.util.*;

public final class PolicyManifestJsonFileParser implements PolicyManifestParser {

    /**
     * @param policyLocation      parent directory of manifest file, e.g. '.../us_consumers/lob/policy_name/metadata.json' ... all
     *                            characters up to and including the last forward-slash will be used as source-of-truth
     *                            prefix for locating policy versions.
     * @param manifestFileContent contents of the manifest file
     * @return set of {@link PolicyDefinition} that contains policy specifications parsed from the file
     * @throws ManifestProcessingException for any formatting error from input file, including json and null fields.
     */
    @Override
    public Set<PolicyDefinition> parseVersions(@NonNull String policyLocation, String manifestFileContent) throws ManifestProcessingException {

        if (manifestFileContent == null) {
            throw new ManifestProcessingException(String.format("content null at path '%s'", policyLocation));
        }

        try {
            // read stored version of metadata.json (parsed object may contain null fields)
            StoredPolicyManifest storedManifest = ParseUtils.MAPPER.readValue(manifestFileContent, StoredPolicyManifest.class);
            if (storedManifest.policyVersions == null) {
                throw new ManifestProcessingException(String.format("File or policy version content null at filepath '%s'", policyLocation));
            }

            Set<PolicyDefinition> specifications = new HashSet<>();
            Set<String> specifiedVersions = new HashSet<>();
            for (StoredPolicyManifest.StoredPolicyVersion version : storedManifest.policyVersions) {
                if (version.status == null || Strings.isBlank(version.shortVersionName)) {
                    String m = String.format("manifest.json file at '%s' contains invalid version specification: %s", policyLocation, version);
                    throw new ManifestProcessingException(m);
                } else if (!specifiedVersions.add(version.shortVersionName)) {
                    String m = String.format("manifest.json file at '%s' has multiple specifications for a single policy version", policyLocation);
                    throw new ManifestProcessingException(m);
                } else {
                    specifications.add(new PolicyDefinition(policyLocation, version.shortVersionName));
                }
            }
            return specifications;
        } catch (JsonProcessingException e) {
            throw new ManifestProcessingException(policyLocation + "/manifest.json", e);
        }
    }

    public Optional<PolicyStatus> parseVersionStatusFromLegacyMetadata(String manifestFileContent, String version) throws ManifestProcessingException {

        if (manifestFileContent == null) {
            return Optional.empty();
        }

        try {
            // read stored version of metadata.json (parsed object may contain null fields)
            StoredPolicyManifest storedManifest = ParseUtils.MAPPER.readValue(manifestFileContent, StoredPolicyManifest.class);
            return Optional.ofNullable(storedManifest.policyVersions).orElse(Collections.emptyList()).stream()
                    .filter(versionEntry -> StringUtils.equals(version, versionEntry.shortVersionName))
                    .filter(versionEntry -> !Objects.isNull(versionEntry.status))
                    .map(entry -> entry.status)
                    .findAny();

        } catch (JsonProcessingException e) {
            throw new ManifestProcessingException("manifest.json", e);
        }
    }

    public Optional<PolicyMetadata> parsePolicyMetadata(String metadataFileContent) throws ManifestProcessingException {
        if (metadataFileContent == null) {
            return Optional.empty();
        }

        try {
            // read stored version of metadata.json (parsed object may contain null fields)
            StoredSparsePolicyManifest storedManifest = ParseUtils.MAPPER.readValue(metadataFileContent, StoredSparsePolicyManifest.class);
            if (storedManifest.status == null) {
                return Optional.empty();
            } else {
                PolicyMetadata value = new PolicyMetadata(storedManifest.status);
                if (Strings.isNotBlank(storedManifest.type)) value.setType(storedManifest.type);
                if (storedManifest.compileVersion!=null) value.setCompileVersion(storedManifest.compileVersion);
                return Optional.of(value);
            }
        } catch (JsonProcessingException e) {
            throw new ManifestProcessingException("policy-metadata.json", e);
        }
    }

}
