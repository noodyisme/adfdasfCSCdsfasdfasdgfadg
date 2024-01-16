package com.capitalone.identity.identitybuilder.model.parsing;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Represent a manifest.json object stored in s3.
 */
public class StoredPolicyManifest {

    @JsonProperty(value = "Versions_Supported")
    @Nullable
    public List<StoredPolicyVersion> policyVersions;

    public static class StoredPolicyVersion {

        @JsonProperty(value = "Version")
        @Nullable
        public String shortVersionName;

        @JsonProperty(value = "Status")
        @Nullable
        public PolicyStatus status;

        @Override
        public String toString() {
            return "StoredPolicyVersion{" +
                    "shortVersionName='" + shortVersionName + '\'' +
                    ", status=" + status +
                    '}';
        }
    }
}
