package com.capitalone.identity.identitybuilder.model.parsing;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * As of 12/10/2020: Possible "lifecycle" values of a config store Policy See
 * <a href="https://confluence.kdc.capitalone.com/pages/viewpage.action?pageId=959896253#PolicyLifeCycle-policy-status">
 * Policy Status Specifications</a>
 */
public enum PolicyStatus {

    /**
     * Received assurances this is temporary (1/8/21)
     * Ticket below (or associated epic) will:
     * A. fix admin services to write the correct status flag
     * B. fix the flag in metadata.json files deployed to buckets
     * https://jira.kdc.capitalone.com/browse/AUTH-104358
     */
    @JsonAlias("IN-PROGRESS")
    IN_PROGRESS(ClientEnvironment.DEV),
    READY_FOR_REVIEW(ClientEnvironment.DEV),
    IN_REVIEW(ClientEnvironment.DEV),
    REVIEWED(ClientEnvironment.DEV),
    READY_FOR_QA(ClientEnvironment.QA),
    ACTIVE_QA(ClientEnvironment.QA),
    QA(ClientEnvironment.QA),
    TESTING_COMPLETED,
    APPROVED(ClientEnvironment.QA),
    INACTIVE(ClientEnvironment.QA),
    READY_FOR_PROD(ClientEnvironment.PROD),
    ACTIVE(ClientEnvironment.PROD),
    DISABLED(ClientEnvironment.PROD),

    /**
     * Convenience to prevent confusion around past/present tense of this status.
     */
    @JsonAlias("ARCHIVED")
    ARCHIVE;

    final List<ClientEnvironment> environments;

    PolicyStatus(ClientEnvironment... deployedEnvironments) {
        environments = Arrays.asList(deployedEnvironments);
    }

    /**
     * @return {@link true} if config store entities with this status should be loaded in a given environment.
     */
    public boolean shouldLoad(@NonNull ClientEnvironment environment) {
        switch (environment) {
            case PROD:
                return environments.contains(ClientEnvironment.PROD);
            case QA:
                return environments.contains(ClientEnvironment.QA) || shouldLoad(ClientEnvironment.PROD);
            case DEV:
                return environments.contains(ClientEnvironment.DEV) || shouldLoad(ClientEnvironment.QA);
            default:
                throw new IllegalArgumentException("unrecognized environment: " + environment);
        }
    }


    public boolean isDisabled() {
        return this == PolicyStatus.DISABLED;
    }

    public boolean isActive() {
        return this == PolicyStatus.ACTIVE ||
                this == PolicyStatus.ACTIVE_QA;
    }

    public EntityActivationStatus toActivationStatus(ClientEnvironment environment) {
        if (this == PolicyStatus.ACTIVE || this == PolicyStatus.ACTIVE_QA) {
            return EntityActivationStatus.ACTIVE;
        } else if (this == PolicyStatus.DISABLED) {
            return EntityActivationStatus.DISABLED;
        } else if (this.shouldLoad(environment)) {
            return EntityActivationStatus.AVAILABLE;
        } else {
            return EntityActivationStatus.DISABLED;
        }
    }

}
