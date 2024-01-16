package com.capitalone.identity.identitybuilder;

import org.springframework.lang.NonNull;

/**
 * Represent environment where Config Store Client is running.
 *
 * @deprecated moving forward implementations and host libraries should use environment-agnostic abstractions and
 * not rely upon this flag.
 */
@Deprecated
public enum ClientEnvironment {
    DEV,
    QA,
    PROD;

    /**
     * @return representation of enum in property string format
     */
    public String toProperty() {
        return this.name().toLowerCase().replace("_", "-");
    }

    /**
     * Translates property and calls {@link #valueOf(String)} on the enum.
     *
     * @throws {@link IllegalArgumentException} per {@link #valueOf(String)}
     */
    public static ClientEnvironment fromProperty(@NonNull String property) {
        String replace = property.toUpperCase().replace("-", "_");
        return ClientEnvironment.valueOf(replace);
    }
}
