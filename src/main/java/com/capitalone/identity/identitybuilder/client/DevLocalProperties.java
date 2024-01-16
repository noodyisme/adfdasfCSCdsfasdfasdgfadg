package com.capitalone.identity.identitybuilder.client;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Conditional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Conditional(DevLocalProperties.DevLocalEnabled.class)
public class DevLocalProperties {

    public static final String DEV_LOCAL_ROOT_DIR = "csc.dev-local.debug-root-directory";
    private static final String DEV_LOCAL_ENABLED = "csc.dev-local.enabled";
    private static final String DEV_LOCAL_AWS_PROFILE_NAME = "csc.dev-local.aws-credential-profile-name";

    private final String credentialProfileName;
    private final String rootDirectory;

    /**
     * @param devLocalAwsCredentialProfileName dev-local only, needed to connect to S3 from local dev environment
     *                                         {@link com.amazonaws.auth.profile.ProfileCredentialsProvider#ProfileCredentialsProvider(String)}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    DevLocalProperties(@Value("${" + DEV_LOCAL_AWS_PROFILE_NAME + ":#{null}}")
                               String devLocalAwsCredentialProfileName,
                       @Value("${" + DEV_LOCAL_ROOT_DIR + ":#{null}}")
                               String devLocalDebugRootDirectory) {

        if (!Strings.isBlank(devLocalDebugRootDirectory)) {
            rootDirectory = devLocalDebugRootDirectory;
            credentialProfileName = null;
        } else {
            rootDirectory = null;
            if (Strings.isBlank(devLocalAwsCredentialProfileName)) {
                String msg = String.format("Missing required property when dev-local is enabled '%s'",
                        DEV_LOCAL_AWS_PROFILE_NAME);
                throw new IllegalArgumentException(msg);
            } else {
                credentialProfileName = devLocalAwsCredentialProfileName;
            }

        }

    }

    @NonNull
    public String getAwsCredentialProfileName() {
        return credentialProfileName;
    }

    @NonNull
    public String getRootDirectory() {
        return rootDirectory;
    }

    public static class DevLocalEnabled extends AllNestedConditions {

        DevLocalEnabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(ClientProperties.LibraryEnabled.class)
        public static class LibraryIsEnabled {
        }

        @ConditionalOnProperty(value = DEV_LOCAL_ENABLED)
        public static class CscDevLocalEnabled {
        }
    }

    public static class DevLocalDisabled extends NoneNestedConditions {

        DevLocalDisabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(DevLocalEnabled.class)
        public static class DevEnabled {
        }
    }

    public static class DevLocalDebugRootDirectoryEnabled extends NoneNestedConditions {

        DevLocalDebugRootDirectoryEnabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(DevLocalDebugRootDirectoryDisabled.class)
        public static class DevDebugDisabled {
        }
    }

    public static class DevLocalDebugRootDirectoryDisabled extends AnyNestedCondition {

        DevLocalDebugRootDirectoryDisabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(DevLocalDisabled.class)
        public static class DevIsDisabledLocally {
        }

        @ConditionalOnProperty(value = DEV_LOCAL_ROOT_DIR, havingValue = "WONTMATCH", matchIfMissing = true)
        public static class MissingDebugRootDirectory {
        }

    }

}
