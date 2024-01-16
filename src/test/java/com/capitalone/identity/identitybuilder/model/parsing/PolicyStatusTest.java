package com.capitalone.identity.identitybuilder.model.parsing;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyStatusTest {

    private static final List<PolicyStatus> PROD_POLICIES = Arrays.asList(
            PolicyStatus.READY_FOR_PROD,
            PolicyStatus.ACTIVE,
            PolicyStatus.DISABLED
    );
    private static final List<PolicyStatus> QA_POLICIES = Arrays.asList(
            PolicyStatus.READY_FOR_QA,
            PolicyStatus.QA,
            PolicyStatus.ACTIVE_QA,
            PolicyStatus.INACTIVE,
            PolicyStatus.APPROVED
    );

    private static final List<PolicyStatus> DEV_POLICIES = Arrays.asList(
            PolicyStatus.IN_PROGRESS,
            PolicyStatus.READY_FOR_REVIEW,
            PolicyStatus.IN_REVIEW,
            PolicyStatus.REVIEWED
    );

    @ParameterizedTest()
    @EnumSource(value = PolicyStatus.class)
    void statusesSpecifiedForAllEnvironments(PolicyStatus status) {
        for (ClientEnvironment environment : ClientEnvironment.values()) {
            try {
                status.shouldLoad(environment);
            } catch (Exception e) {
                fail(String.format("Status: %s, not specified for environment: %s", status, environment));
            }
        }
    }

    @ParameterizedTest()
    @EnumSource(value = PolicyStatus.class)
    void statusesLoadInAllLowerEnvironments(PolicyStatus status) {
        if (status.shouldLoad(ClientEnvironment.PROD)) {
            assertTrue(status.shouldLoad(ClientEnvironment.DEV));
            assertTrue(status.shouldLoad(ClientEnvironment.QA));
            assertTrue(status.shouldLoad(ClientEnvironment.PROD));
        } else if (status.shouldLoad(ClientEnvironment.QA)) {
            assertTrue(status.shouldLoad(ClientEnvironment.DEV));
            assertTrue(status.shouldLoad(ClientEnvironment.QA));
            assertFalse(status.shouldLoad(ClientEnvironment.PROD));
        } else if (status.shouldLoad(ClientEnvironment.DEV)) {
            assertTrue(status.shouldLoad(ClientEnvironment.DEV));
            assertFalse(status.shouldLoad(ClientEnvironment.QA));
            assertFalse(status.shouldLoad(ClientEnvironment.PROD));
        } else {
            assertFalse(status.shouldLoad(ClientEnvironment.DEV));
            assertFalse(status.shouldLoad(ClientEnvironment.QA));
            assertFalse(status.shouldLoad(ClientEnvironment.PROD));
        }

    }

    @Test
    void statusesToLoadInProd() {
        for (PolicyStatus status : PolicyStatus.values()) {
            if (PROD_POLICIES.contains(status)) {
                assertTrue(status.shouldLoad(ClientEnvironment.PROD));
            } else {
                assertFalse(status.shouldLoad(ClientEnvironment.PROD));
            }
        }
    }

    @Test
    void statusesToLoadInQA() {
        for (PolicyStatus status : PolicyStatus.values()) {
            if (PROD_POLICIES.contains(status) || QA_POLICIES.contains(status)) {
                assertTrue(status.shouldLoad(ClientEnvironment.QA));
            } else {
                assertFalse(status.shouldLoad(ClientEnvironment.QA));
            }
        }
    }

    @Test
    void statusesToLoadInDev() {
        for (PolicyStatus status : PolicyStatus.values()) {
            if (PROD_POLICIES.contains(status) || QA_POLICIES.contains(status) || DEV_POLICIES.contains(status)) {
                assertTrue(status.shouldLoad(ClientEnvironment.DEV));
            } else {
                assertFalse(status.shouldLoad(ClientEnvironment.DEV));
            }
        }
    }

    @Test
    void prodActivationStatuses() {
        assertEquals(EntityActivationStatus.AVAILABLE, PolicyStatus.READY_FOR_PROD.toActivationStatus(ClientEnvironment.PROD));
        assertEquals(EntityActivationStatus.ACTIVE, PolicyStatus.ACTIVE.toActivationStatus(ClientEnvironment.PROD));
        assertEquals(EntityActivationStatus.DISABLED, PolicyStatus.ARCHIVE.toActivationStatus(ClientEnvironment.PROD));
    }
}
