package com.capitalone.identity.identitybuilder.model;

import org.springframework.lang.NonNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a full object "state" as well as the set of {@link Delta} operations that
 * transform from a previous, unlinked, version of object state.
 *
 * @param <T> generic {@link Versionable} objects that can be used to compare state.
 */
public class EntityState<T extends Versionable> {

    private final Set<T> entities;
    private final Set<Delta<T>> changes;

    public EntityState() {
        this(Collections.emptySet());
    }

    public EntityState(Set<T> entities) {
        this(entities, entities.stream().map(Delta::add).collect(Collectors.toSet()));
    }

    private EntityState(Set<T> entities, Set<Delta<T>> changes) {
        this.entities = entities;
        this.changes = changes;
    }

    public Set<T> getCurrentVersions() {
        return entities;
    }

    public Set<Delta<T>> getChanges() {
        return changes;
    }

    /**
     * Create a new {@link EntityState} that is a delta between this {@link EntityState} and
     * the set of entities in the provided argument.
     *
     * @param nextEntities the set of entities that represents the updated state
     * @return an {@link EntityState} object that has information about
     */
    public EntityState<T> getUpdatedState(Set<T> nextEntities) {

        final Set<T> next = new HashSet<>(nextEntities);

        // entities to delete
        final Set<String> nextIds = next.stream().map(Versionable::getId).collect(Collectors.toSet());
        final Set<Delta<T>> toDelete = entities.stream()
                .filter(entity -> !nextIds.contains(entity.getId()))
                .map(Delta::delete)
                .collect(Collectors.toSet());

        // entities to add
        final Set<String> prevIds = entities.stream().map(Versionable::getId).collect(Collectors.toSet());
        Set<Delta<T>> toAdd = next.stream()
                .filter(entity -> !prevIds.contains(entity.getId()))
                .map(Delta::add)
                .collect(Collectors.toSet());

        // entities to modify
        final Map<String, String> modifiedMap = entities.stream().collect(
                Collectors.toMap(Versionable::getId, Versionable::getVersion));
        Set<Delta<T>> toUpdate = next.stream()
                .filter(item -> {
                    String existingVersion = modifiedMap.get(item.getId());
                    return existingVersion != null && !existingVersion.equals(item.getVersion());
                })
                .map(Delta::update)
                .collect(Collectors.toSet());

        Set<Delta<T>> allChanges = new HashSet<>();
        allChanges.addAll(toAdd);
        allChanges.addAll(toDelete);
        allChanges.addAll(toUpdate);
        return new EntityState<>(next, allChanges);
    }

    public static class Delta<T extends Versionable> {

        public static <T extends Versionable> Delta<T> add(T item) {
            return new Delta<>(ChangeType.ADD, item);
        }

        public static <T extends Versionable> Delta<T> update(T item) {
            return new Delta<>(ChangeType.UPDATE, item);
        }

        public static <T extends Versionable> Delta<T> delete(T item) {
            return new Delta<>(ChangeType.DELETE, item);
        }

        /**
         * @deprecated use getter
         */
        public final ChangeType type;


        /**
         * @deprecated use getter
         */
        public final T entityInfo;

        public Delta(@NonNull ChangeType type, @NonNull T entityInfo) {
            this.type = Objects.requireNonNull(type);
            this.entityInfo = Objects.requireNonNull(entityInfo);
        }

        public ChangeType getType() {
            return type;
        }

        public T getEntityInfo() {
            return entityInfo;
        }

        @Override
        public String toString() {
            return "Delta{" +
                    "type=" + type +
                    ", entityInfo=" + entityInfo +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Delta<?> delta = (Delta<?>) o;
            return getType() == delta.getType() &&
                    getEntityInfo().equals(delta.getEntityInfo());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getType(), getEntityInfo());
        }

        public enum ChangeType {
            ADD,
            UPDATE,
            DELETE
        }
    }
}
