package com.capitalone.identity.identitybuilder.model;


import com.capitalone.identity.identitybuilder.util.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ConfigStoreItem {

    static Pattern dmnPattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/rules/*.*\\.dmn");
    static Pattern processPattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/process/*.*\\.xml");
    static Pattern libraryPattern = Pattern.compile(".*/routes/*.*\\.xml");
    static Pattern configDefaultPattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/config/defaults\\.json");
    static Pattern configUsecasePattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/config/*.*\\.json");
    static Pattern configUseCaseAllowedPattern = Pattern.compile("(.*/defaults\\.json|.*/schema\\.json|.*/features(.*)\\.json)");
    static Pattern configSchemaPattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/config/schema\\.json");
    static Pattern configFeaturesPattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/config/features(?:|-[A-Za-z-].*)\\.json");
    static Pattern policyStatusPattern = Pattern.compile(".*/metadata\\.json");
    static Pattern policyStatusSparsePattern = Pattern.compile(".*/\\d+\\.\\d+(?:|/\\d+)/policy-metadata\\.json");

    @NonNull
    public static Type getTypeFromPath(@Nullable String path) {
        if (path == null) {
            return Type.UNRECOGNIZED;
        } else if (dmnPattern.matcher(path).matches()) {
            return Type.RULES;
        } else if (libraryPattern.matcher(path).matches()) {
            return Type.LIBRARY;
        } else if (processPattern.matcher(path).matches()) {
            return Type.PROCESS;
        } else if (policyStatusPattern.matcher(path).matches()) {
            return Type.POLICY_STATUS;
        } else if (policyStatusSparsePattern.matcher(path).matches()) {
            return Type.POLICY_STATUS_SPARSE;
        } else if (!configUseCaseAllowedPattern.matcher(path).matches()
                && configUsecasePattern.matcher(path).matches()) {
            return Type.CONFIG_USECASE;
        } else if (configDefaultPattern.matcher(path).matches()) {
            return Type.CONFIG_DEFAULT;
        } else if (configSchemaPattern.matcher(path).matches()) {
            return Type.CONFIG_SCHEMA;
        } else if (configFeaturesPattern.matcher(path).matches()) {
            return Type.CONFIG_FEATURES;
        }else {
            return Type.UNRECOGNIZED;
        }
    }

    @NonNull
    public static Optional<ConfigStoreItem> getItem(@NonNull Set<ConfigStoreItem> items, @NonNull Type type) {
        return items.stream().filter(item -> ConfigStoreItem.getTypeFromPath(item.info.getName()) == type).findAny();
    }

    @NonNull
    public static Set<ConfigStoreItem> getItems(@NonNull Set<ConfigStoreItem> items, @NonNull Type type) {
        return items.stream().filter(item -> ConfigStoreItem.getTypeFromPath(item.info.getName()) == type).collect(Collectors.toSet());
    }

    @NonNull
    public final String content;

    @NonNull
    public final ConfigStoreItemInfo info;

    public ConfigStoreItem(@NonNull ConfigStoreItemInfo info, String content) {
        this.info = Objects.requireNonNull(info);
        this.content = Objects.requireNonNull(content);
    }

    public ConfigStoreItem(@NonNull String name, String content) {
        this(name, content, StringUtils.getContentHash(content));
    }

    public ConfigStoreItem(@NonNull String name, @NonNull String content, @NonNull String contentTag) {
        this.info = new ConfigStoreItemInfo(Objects.requireNonNull(name), Objects.requireNonNull(contentTag));
        this.content = Objects.requireNonNull(content);
    }

    public String getName() {
        return info.getName();
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigStoreItem that = (ConfigStoreItem) o;
        return content.equals(that.content) &&
                info.equals(that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, info);
    }

    public enum Type {
        PROCESS,
        RULES,
        LIBRARY,
        CONFIG_SCHEMA,
        CONFIG_DEFAULT,
        CONFIG_USECASE,
        CONFIG_FEATURES,
        POLICY_STATUS,
        POLICY_STATUS_SPARSE,
        UNRECOGNIZED
    }

}
