package com.capitalone.identity.identitybuilder.model.abac;

import com.capitalone.identity.identitybuilder.model.EntityInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PolicyAccessTest {

    @Test
    void constructOk() {

        final String allowClientID = "client-abc";
        final String denyClientID = "client-xyz";

        Client allowClient = new Client(allowClientID, AccessGrant.ALLOW);
        Client denyClient = new Client(denyClientID, AccessGrant.DENY);

        PolicyAccess access = assertDoesNotThrow(() -> new PolicyAccess("a", "b", 1, Arrays.asList(allowClient, denyClient)));

        assertEquals(1, access.getPolicyMajorVersion());
        assertEquals("a", access.getAccessId());
        assertEquals("b", access.getPolicyShortName());
        assertEquals(AccessGrant.UNDEFINED, access.getAccess("random"));
        assertEquals(AccessGrant.ALLOW, access.getAccess(allowClientID));
        assertEquals(AccessGrant.DENY, access.getAccess(denyClientID));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void constructThrowsNPEs() {
        assertThrows(NullPointerException.class, () -> new PolicyAccess(null, "b", 0, Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> new PolicyAccess("a", null, 0, Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> new PolicyAccess("a", "b", 0, null));
    }

    @Test
    void constructWithEntityOk() {
        EntityInfo.Access access = new EntityInfo.Access(
                "a/b/c/1/access-control",
                "a/b/c/1/access-control/0/policy-access.json",
                0, "c", "a/b/c",
                1, Collections.emptySet());

        StoredPolicyAccess policyAccess = new StoredPolicyAccess();
        policyAccess.policyMajorVersion = 1;
        policyAccess.policyNamespace = "a/b/c";
        policyAccess.clients = Collections.emptyList();
        assertDoesNotThrow(() -> new PolicyAccess(access, policyAccess));
    }

    @Test
    void constructWithEntity_contentMismatchErrors() {
        EntityInfo.Access access = new EntityInfo.Access(
                "a/b/c/1/access-control",
                "a/b/c/1/access-control/0/policy-access.json",
                0, "c", "a/b/c",
                1, Collections.emptySet());

        StoredPolicyAccess policyAccess = new StoredPolicyAccess();
        policyAccess.policyMajorVersion = 1;
        policyAccess.policyNamespace = "a/b/c";
        policyAccess.clients = Collections.emptyList();
        assertDoesNotThrow(() -> new PolicyAccess(access, policyAccess));

        policyAccess.policyNamespace = "a/b/Z";
        assertThrows(IllegalArgumentException.class, () -> new PolicyAccess(access, policyAccess));

        policyAccess.policyNamespace = "a/b/c";
        assertDoesNotThrow(() -> new PolicyAccess(access, policyAccess));

        policyAccess.policyMajorVersion = 2;
        assertThrows(IllegalArgumentException.class, () -> new PolicyAccess(access, policyAccess));

    }

}
