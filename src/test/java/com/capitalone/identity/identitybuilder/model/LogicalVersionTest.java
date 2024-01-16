package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LogicalVersionTest {
    private final LogicalVersion testObject = new LogicalVersion() {

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 2;
        }

        @Override
        public int getPatchVersion() {
            return 3;
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"/", "."})
    void getLogicalVersionString(String separator) {
        String expected = "test" + separator + "1.2.3";
        assertEquals(expected, testObject.getLogicalVersionString(separator));
    }

    @Test
    void getMajorVersionString() {
        assertEquals("1", testObject.getMajorVersionString());
    }

    @Test
    void getMinorVersionString() {
        assertEquals("1.2", testObject.getMinorVersionString());
    }

    @Test
    void getPatchVersionString() {
        assertEquals("1.2.3", testObject.getPatchVersionString());
    }
}
