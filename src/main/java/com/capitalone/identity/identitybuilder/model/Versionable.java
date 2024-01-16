package com.capitalone.identity.identitybuilder.model;

public interface Versionable {

    /**
     * @return a {@link String} that represents an object across versions.
     */
    String getId();

    /**
     * @return a {@link String} that represents a version of an object.
     */
    String getVersion();

    /**
     * @return a {@link String} that represents the root path prefix of a Versionable object's id.*
     * default return value is an empty String {@code ""}.
     */
    default String getIdPrefix(){
        return "";
    }
}
