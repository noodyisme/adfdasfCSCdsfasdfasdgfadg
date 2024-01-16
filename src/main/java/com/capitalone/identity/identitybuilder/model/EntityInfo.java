package com.capitalone.identity.identitybuilder.model;

import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents metadata information for an {@link Entity} object. Includes {@link ConfigStoreItemInfo} metadata
 * for the logical group of {@link ConfigStoreItem}s represented by the {@link Entity}.
 * <p>
 * This should be enough information to determine if an {@link Entity} object has been updated, without
 * loading the full {@link Entity} object.
 */
public abstract class EntityInfo implements Versionable, LogicalVersion {

    public static final int DEFAULT_VERSION_NUMBER = 0;

    private final Set<ConfigStoreItemInfo> componentItems = new HashSet<>();
    private final String version;

    private final int versionNumber;
    private final String entityId;
    private final String entityLocation;

    private final String idPrefix;
    private final EntityType type;
    private EntityInfo priorVersion;

    /**
     * @param entityId
     * @param type
     * @param items         set of items that constitute the {@link Entity}
     * @param versionNumber
     */
    protected EntityInfo(@NonNull String entityId,
                         @NonNull String entityPrefix,
                         @NonNull EntityType type,
                         @NonNull Set<ConfigStoreItemInfo> items,
                         int versionNumber,
                         @Nullable EntityInfo priorVersion) {
        this.entityId = Objects.requireNonNull(entityId);
        this.entityLocation = Objects.requireNonNull(entityPrefix);
        this.type = Objects.requireNonNull(type);
        this.componentItems.addAll(items);
        final String concatVersion = this.componentItems.stream()
                .map(itemInfo -> {
                    String s3Location = itemInfo.getName();
                    int i = s3Location.indexOf(entityLocation) + entityLocation.length();
                    String nonLocationSpecificString = s3Location.substring(i);
                    return Objects.hash(nonLocationSpecificString, itemInfo.getTag());
                })
                .map(String::valueOf)
                .sorted()
                .collect(Collectors.joining());
        this.version = UUID.nameUUIDFromBytes(concatVersion.getBytes()).toString();
        this.versionNumber = versionNumber;
        this.priorVersion = priorVersion;
        this.idPrefix = StringUtils.defaultIfEmpty(StringUtils.substring(entityLocation, 0,
                Math.max(StringUtils.lastIndexOf(entityLocation, this.entityId), 0)), "");
    }



    @Override
    public String getId() {
        return entityId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getIdPrefix() {
       return idPrefix;
    }

    public String getLocationPrefix() {
        return entityLocation;
    }


    @Override
    public int getPatchVersion() {
        return versionNumber;
    }

    public EntityType getType() {
        return type;
    }


    public Set<ConfigStoreItemInfo> getItemInfo() {
        return componentItems;
    }

    public Set<String> getFilteredItemNames() {
        return Collections.emptySet();
    }

    public EntityInfo getPriorVersion() {
        return priorVersion;
    }

    public final EntityInfo setPriorVersion(EntityInfo priorVersion) {
        if (priorVersion.getPatchVersion() >= getPatchVersion()) {
            String message = String.format("prior version must have lower version number [this.versionNumber=%s, " +
                    "priorVersion.versionNumber=%s", getPatchVersion(), priorVersion.getPatchVersion());
            throw new IllegalArgumentException(message);
        }
        this.priorVersion = priorVersion;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityInfo)) return false;

        EntityInfo that = (EntityInfo) o;

        if (!getVersion().equals(that.getVersion())) return false;
        if (!getId().equals(that.getId())) return false;
        if (getMajorVersion() != that.getMajorVersion()) return false;
        if (getMinorVersion() != that.getMinorVersion()) return false;
        if (getPatchVersion() != that.getPatchVersion()) return false;
        return getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVersion(), getId(), getType(), getMajorVersion(), getMinorVersion(), getPatchVersion());
    }

    @Override
    public String toString() {
        return "EntityInfo{" +
                "version='" + version + '\'' +
                ", entityId='" + entityId + '\'' +
                ", versionNumber='" + versionNumber + '\'' +
                ", entityLocation='" + entityLocation + '\'' +
                ", type=" + type +
                '}';
    }

    public static class Pip extends EntityInfo {

        public Pip(String entityId, String entityLocation, Set<ConfigStoreItemInfo> items) {
            super(entityId, entityLocation, EntityType.PIP, items, DEFAULT_VERSION_NUMBER, null);
        }

        @Override
        public String getName() {
            return getId();
        }

        @Override
        public int getMajorVersion() {
            return DEFAULT_VERSION_NUMBER;
        }

        @Override
        public int getMinorVersion() {
            return DEFAULT_VERSION_NUMBER;
        }
    }

    public static class Access extends EntityInfo implements PolicyInfo.Major {

        final String policyShortName;
        final String policyFullName;
        final int majorVersion;

        public Access(String entityId,
                      String entityLocationPrefix,
                      int entityVersionNumber,
                      String policyShortName,
                      String policyFullName,
                      int policyMajorVersion,
                      Set<ConfigStoreItemInfo> items) {
            super(entityId,
                    entityLocationPrefix,
                    EntityType.ACCESS,
                    items,
                    entityVersionNumber,
                    null);
            this.majorVersion = policyMajorVersion;
            this.policyShortName = Objects.requireNonNull(policyShortName);
            this.policyFullName = Objects.requireNonNull(policyFullName);
        }

        @NonNull
        public String getPolicyShortName() {
            return policyShortName;
        }

        @Override
        public String getPolicyFullName() {
            return policyFullName;
        }

        @Override
        public int getPolicyMajorVersion() {
            return majorVersion;
        }

        @Override
        public String getName() {
            return policyFullName;
        }

        @Override
        public int getMajorVersion() {
            return majorVersion;
        }

        @Override
        public int getMinorVersion() {
            return DEFAULT_VERSION_NUMBER;
        }

        @Override
        public String toString() {
            return super.toString() + "[Access{" +
                    "policyShortName='" + policyShortName + '\'' +
                    ", majorVersion=" + majorVersion +
                    ", policyFullName=" + policyFullName +
                    "}]";
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    public static class Policy extends EntityInfo implements PolicyInfo.Patch {

        private final String policyShortName;
        private final String policyFullName;
        private final int policyMinorVersion;
        private final int policyMajorVersion;

        private static boolean recognizedItem(ConfigStoreItemInfo item) {
            return !ConfigStoreItem.Type.UNRECOGNIZED.equals(ConfigStoreItem.getTypeFromPath(item.getName()));
        }

        private final Set<String> filteredItemNames;

        public Policy(@NonNull PolicyDefinition definition, Set<ConfigStoreItemInfo> items) {
            super(definition.getPolicyFullNameWithVersion(), definition.getPolicyLocation(), EntityType.POLICY,
                    items.stream().filter(Policy::recognizedItem).collect(Collectors.toSet()),
                    definition.getPolicyPatchVersion(), null);
            this.filteredItemNames = items.stream()
                    .filter(item -> !recognizedItem(item))
                    .map(ConfigStoreItemInfo::getName)
                    .collect(Collectors.toSet());

            this.policyShortName = Objects.requireNonNull(definition.getPolicyShortName());
            this.policyFullName = Objects.requireNonNull(definition.getPolicyFullName());
            this.policyMajorVersion = definition.getPolicyMajorVersion();
            this.policyMinorVersion = definition.getPolicyMinorVersion();
        }

        @Override
        public String getPolicyShortName() {
            return policyShortName;
        }

        @Override
        public String getPolicyFullName() {
            return getName();
        }

        @Override
        public int getPolicyMinorVersion() {
            return getMinorVersion();
        }

        @Override
        public int getPolicyMajorVersion() {
            return getMajorVersion();
        }


        @Override
        public Set<String> getFilteredItemNames() {
            return filteredItemNames;
        }

        @Override
        public int getPolicyPatchVersion() {
            return getPatchVersion();
        }


        @Override
        public String getName() {
            return policyFullName;
        }

        @Override
        public int getMajorVersion() {
            return policyMajorVersion;
        }

        @Override
        public int getMinorVersion() {
            return policyMinorVersion;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }
}
