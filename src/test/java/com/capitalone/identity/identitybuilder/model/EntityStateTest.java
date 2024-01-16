package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityStateTest {

    private EntityState<MockEntity> getStartState(Set<MockEntity> states) {
        return new EntityState<MockEntity>().getUpdatedState(states);
    }

    @Test
    void getUpdatedState_start() {
        Set<MockEntity> entities = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(2, 0, "root"),
                new MockEntity(3, 0, "root")
        ));
        EntityState<MockEntity> startState = getStartState(entities);
        Set<EntityState.Delta<MockEntity>> changes = startState.getChanges();
        assertEquals(3, changes.size());
        assertTrue(changes.stream().map(EntityState.Delta::getType).allMatch(EntityState.Delta.ChangeType.ADD::equals));

    }

    @Test
    void getUpdatedState_add() {
        Set<MockEntity> entities = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(2, 0, "root")
        ));
        EntityState<MockEntity> start = getStartState(entities);

        Set<MockEntity> update = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(2, 0, "root"),
                new MockEntity(3, 0, "root")
        ));
        EntityState<MockEntity> updatedState = start.getUpdatedState(update);

        Set<EntityState.Delta<MockEntity>> changes = updatedState.getChanges();
        assertEquals(1, changes.size());
        assertTrue(changes.stream().map(EntityState.Delta::getType).allMatch(EntityState.Delta.ChangeType.ADD::equals));

    }

    @Test
    void getUpdatedState_update() {
        Set<MockEntity> entities = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(2, 0, "root")
        ));

        Set<MockEntity> update = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(2, 1, "root")
        ));


        EntityState<MockEntity> start = getStartState(entities);

        EntityState<MockEntity> updatedState = start.getUpdatedState(update);

        Set<EntityState.Delta<MockEntity>> changes = updatedState.getChanges();
        assertEquals(1, changes.size());
        assertTrue(changes.stream().map(EntityState.Delta::getType).allMatch(EntityState.Delta.ChangeType.UPDATE::equals));

    }

    // @TODO make these tests parameterized
    @Test
    void getUpdatedState_delete() {
        Set<MockEntity> entities = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(2, 0, "root"),
                new MockEntity(3, 0, "root"),
                new MockEntity(4, 0, "root"),
                new MockEntity(456, 0, "root")
        ));

        Set<MockEntity> update = new HashSet<>(Arrays.asList(
                new MockEntity(1, 0, "root"),
                new MockEntity(3, 0, "root")
        ));

        EntityState<MockEntity> start = getStartState(entities);

        EntityState<MockEntity> updatedState = start.getUpdatedState(update);

        Set<EntityState.Delta<MockEntity>> changes = updatedState.getChanges();
        assertEquals(3, changes.size());
        assertTrue(changes.stream().map(EntityState.Delta::getType).allMatch(EntityState.Delta.ChangeType.DELETE::equals));

    }

    private static class MockEntity implements Versionable {
        final int id;
        final int version;

        final String location;

        private MockEntity(int id, int version, String location) {
            this.id = id;
            this.version = version;
            this.location = location;
        }

        @Override
        public String getId() {
            return String.valueOf(id);
        }

        @Override
        public String getVersion() {
            return String.valueOf(version);
        }

        @Override
        public String getIdPrefix() {
            return location.substring(0,location.lastIndexOf(id));
        }
    }
}
