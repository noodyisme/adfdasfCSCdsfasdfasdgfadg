package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Conditional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Conditional(ClientProperties.LibraryEnabled.class)
public class ClientProperties {

    private static final String LIBRARY_ENABLED = "csc.enabled";
    private static final String ENVIRONMENT = "csc.client-environment";

    private final ClientEnvironment clientEnvironment;

    public ClientProperties(@Value("${" + ENVIRONMENT + ":#{null}}") String name) {

        try {
            clientEnvironment = Optional.ofNullable(name)
                    .map(ClientEnvironment::fromProperty)
                    .orElseThrow(() -> new IllegalArgumentException("Missing property"));

        } catch (IllegalArgumentException e) {
            String validList = Arrays.stream(ClientEnvironment.values())
                    .map(ClientEnvironment::toProperty)
                    .collect(Collectors.joining(", "));

            String message = String.format("Missing or Invalid Property '%s' must be one of: %s",
                    ENVIRONMENT, validList);
            IllegalArgumentException exception = new IllegalArgumentException(message);
            exception.addSuppressed(e);
            throw exception;
        }
    }

    @NonNull
    public ClientEnvironment getClientEnvironment() {
        return clientEnvironment;
    }

    public static final class LibraryEnabled extends AllNestedConditions {

        LibraryEnabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(value = LIBRARY_ENABLED, matchIfMissing = true)
        public static class CscEnabled {
        }
    }

    public static final class LibraryDisabled extends NoneNestedConditions {

        LibraryDisabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(LibraryEnabled.class)
        public static class CscNotEnabled {
        }
    }

}
