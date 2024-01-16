package com.capitalone.identity.identitybuilder.repository;

import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CommonItemStore implements ItemStore {

    private static final String GROUP_LABEL_POLICY_SHORT_NAME = "policyShortName";
    private static final String GROUP_LABEL_POLICY_FULL_NAME = "policyFullName";
    private static final String GROUP_LABEL_POLICY_MAJOR_VERSION = "policyMajorVersion";
    private static final String GROUP_LABEL_POLICY_MINOR_VERSION = "policyMinorVersion";
    private static final String GROUP_LABEL_POLICY_PATCH_VERSION = "policyPatchVersion";
    private final EntityFactory accessControlEntityFactory = new EntityFactory(
            Pattern.compile("^(?<locationPrefix>.*?(?<entityId>(?<policyFullName>(?:[-_a-zA-Z\\d]+/){2}(?<policyShortName>[-_a-zA-Z0-9]+))/(?<policyMajorVersion>\\d+)/access-control)/(?<versionNumber>\\d+)/policy-access\\.json)$"),
            (entityId, entityLocationPrefix, entityVersionNumber, matcher, items) -> new EntityInfo.Access(
                    entityId,
                    entityLocationPrefix,
                    entityVersionNumber,
                    matcher.group(GROUP_LABEL_POLICY_SHORT_NAME),
                    matcher.group(GROUP_LABEL_POLICY_FULL_NAME),
                    Integer.parseInt(matcher.group(GROUP_LABEL_POLICY_MAJOR_VERSION)),
                    items
            ));

    private final EntityFactory policyEntityFactoryVersionNumberNamespace = new EntityFactory(
            Pattern.compile("^(?<locationPrefix>.*?(?<entityId>(?<policyFullName>(?:[-_a-zA-Z\\d]+/){2}(?<policyShortName>[-_a-zA-Z0-9]+))/(?<policyMajorVersion>\\d+)\\.(?<policyMinorVersion>\\d+))(?:/(?<policyPatchVersion>\\d+))?)/(?:policy-metadata\\.json|process/.*|rules/.*|config/.*)$"),
            (entityId, entityLocationPrefix, entityVersionNumber, matcher, items) -> {
                PolicyDefinition policy = new PolicyDefinition(
                        entityLocationPrefix,
                        matcher.group(GROUP_LABEL_POLICY_FULL_NAME),
                        matcher.group(GROUP_LABEL_POLICY_SHORT_NAME),
                        Integer.parseInt(matcher.group(GROUP_LABEL_POLICY_MAJOR_VERSION)),
                        Integer.parseInt(matcher.group(GROUP_LABEL_POLICY_MINOR_VERSION)),
                        Optional.ofNullable(matcher.group(GROUP_LABEL_POLICY_PATCH_VERSION))
                                .map(Integer::parseInt)
                                .orElse(EntityInfo.DEFAULT_VERSION_NUMBER));

                return new EntityInfo.Policy(policy, items);
            });

    private final EntityFactory pipEntityFactory = new EntityFactory(
            Pattern.compile("^(?<locationPrefix>.*?(?<entityId>[-_a-zA-Z\\d]+/routes/*.*\\.xml))$"),
            (entityId, entityLocationPrefix, entityVersionNumber, matcher, items) -> new EntityInfo.Pip(
                    entityId, entityLocationPrefix, items
            ));

    @Override
    public EntityFactory getFactoryForEntityType(EntityType type) {
        if (type == EntityType.ACCESS) {
            return accessControlEntityFactory;
        } else if (type == EntityType.PIP) {
            return pipEntityFactory;
        } else if (type == EntityType.POLICY) {
            return policyEntityFactoryVersionNumberNamespace;
        } else {
            return null;
        }
    }

    interface EntityFunction {
        EntityInfo getInfo(String entityId,
                           String entityLocationPrefix,
                           int entityVersionNumber,
                           Matcher matcher,
                           Set<ConfigStoreItemInfo> items);
    }
}
