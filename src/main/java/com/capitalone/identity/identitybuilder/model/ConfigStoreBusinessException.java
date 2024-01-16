package com.capitalone.identity.identitybuilder.model;

/**
 * Exception related to business logic of config store client. Includes issues like error in policy specification.
 * Does not include issues related to problems like I/O or connectivity.
 */
public class ConfigStoreBusinessException extends RuntimeException {

    public ConfigStoreBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
