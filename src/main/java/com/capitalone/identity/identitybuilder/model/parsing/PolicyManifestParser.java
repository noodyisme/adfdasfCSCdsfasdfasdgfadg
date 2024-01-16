package com.capitalone.identity.identitybuilder.model.parsing;

import java.io.IOException;
import java.util.Set;

public interface PolicyManifestParser {

    Set<PolicyDefinition> parseVersions(String filePath, String content) throws ManifestProcessingException;

    class ManifestProcessingException extends IOException {

        ManifestProcessingException(String message) {
            super(message);
        }

        ManifestProcessingException(String fileName, Throwable wrapped) {
            super(fileName, wrapped);
        }

    }
}
