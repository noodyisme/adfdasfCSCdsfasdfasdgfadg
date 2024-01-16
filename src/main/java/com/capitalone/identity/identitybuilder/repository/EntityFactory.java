package com.capitalone.identity.identitybuilder.repository;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows for a declarative declaration of how a logical entity is identified and constructed based
 * on pattern-matching of component items that are observed in the config store.
 */
final class EntityFactory {

    private final Pattern objectPattern;
    private final CommonItemStore.EntityFunction entityGenerator;
    private final boolean doLookForVersion;

    public EntityFactory(Pattern objectPattern, CommonItemStore.EntityFunction entityGenerator) {
        this.objectPattern = Objects.requireNonNull(objectPattern);
        this.entityGenerator = Objects.requireNonNull(entityGenerator);
        this.doLookForVersion = objectPattern.pattern().contains("(?<versionNumber>");
    }

    EntityBuilder newBuilder(String objectName) {
        final Matcher objectMatcher = objectPattern.matcher(objectName);
        if (!objectMatcher.matches()) return null;

        String entityPrefix = Objects.requireNonNull(objectMatcher.group("locationPrefix"),
                "Regex must have a named group of 'locationPrefix'");
        String entityId = Objects.requireNonNull(objectMatcher.group("entityId"),
                "Regex must have a named group of 'entityId'");
        if (!entityPrefix.contains(entityId)) {
            String msg = String.format("entityPrefix must contain entityId in regex [entityId=%s, entityPrefix=%s]",
                    entityId, entityPrefix);
            throw new IllegalStateException(msg);
        }

        int versionNumber = doLookForVersion ? Integer.parseInt(objectMatcher.group("versionNumber")) : 0;

        return new EntityBuilder(entityId, entityPrefix, versionNumber, objectMatcher, entityGenerator);
    }
}
