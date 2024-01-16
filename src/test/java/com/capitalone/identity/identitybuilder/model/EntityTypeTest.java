package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EntityTypeTest {

    @Test
    void sorted_pip_first() {
        List<EntityType> sorted = Arrays.stream(EntityType.values()).sorted().collect(Collectors.toList());
        assertEquals(0, sorted.indexOf(EntityType.PIP));
        assertEquals(1, sorted.indexOf(EntityType.POLICY));
    }

}
