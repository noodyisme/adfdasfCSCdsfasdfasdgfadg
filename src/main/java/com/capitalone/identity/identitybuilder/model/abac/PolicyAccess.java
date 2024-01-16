package com.capitalone.identity.identitybuilder.model.abac;

import com.capitalone.identity.identitybuilder.model.EntityInfo;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PolicyAccess {

    private final String accessId;

    private final String policyName;

    private final int policyMajorVersion;

    private final Map<String, AccessGrant> clientGrants = new HashMap<>();

    public PolicyAccess(@NonNull EntityInfo.Access entity, @NonNull StoredPolicyAccess policyAccess) {
        this(entity.getId(), entity.getPolicyShortName(), entity.getPolicyMajorVersion(),
                policyAccess.clients.stream()
                        .map(storedClient -> new Client(storedClient.id, storedClient.effect))
                        .collect(Collectors.toList()));
        if (!entity.getId().startsWith(policyAccess.policyNamespace)
                || entity.getPolicyMajorVersion() != policyAccess.policyMajorVersion) {
            String msg = String.format("Invalid/mismatched fields in policy access document [entity:=%s, access:=%s]",
                    entity, policyAccess);
            throw new IllegalArgumentException(msg);
        }
    }

    public PolicyAccess(@NonNull String accessId, @NonNull String policyName, int policyMajorVersion,
                        @NonNull List<Client> clients) {
        this.accessId = Objects.requireNonNull(accessId);
        this.policyName = Objects.requireNonNull(policyName);
        this.policyMajorVersion = policyMajorVersion;
        clients.forEach(client -> clientGrants.put(client.id, client.effect));
    }

    @NonNull
    public String getAccessId() {
        return accessId;
    }

    @NonNull
    public String getPolicyShortName() {
        return policyName;
    }

    public int getPolicyMajorVersion() {
        return policyMajorVersion;
    }

    public AccessGrant getAccess(String clientId) {
        return clientGrants.getOrDefault(clientId, AccessGrant.UNDEFINED);
    }
}
