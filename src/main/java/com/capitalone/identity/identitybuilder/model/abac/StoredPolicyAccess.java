package com.capitalone.identity.identitybuilder.model.abac;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StoredPolicyAccess {

    @JsonProperty(value = "schemaVersion")
    String schemaVersion;

    @JsonProperty(value = "policyNamespace")
    String policyNamespace;

    @JsonProperty(value = "policyMajorVersion")
    int policyMajorVersion;


    @JsonProperty(value = "clients")
    List<StoredClient> clients;

    @Override
    public String toString() {
        return "StoredPolicyAccess{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", policyNamespace='" + policyNamespace + '\'' +
                ", policyMajorVersion=" + policyMajorVersion +
                ", clients=" + clients +
                '}';
    }

    public static class StoredClient {
        @JsonProperty(value = "id")
        String id;

        @JsonProperty(value = "effect")
        AccessGrant effect;
    }
}
