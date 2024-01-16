package com.capitalone.identity.identitybuilder.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void requireNotBlank(String value) {
        assertThrows(IllegalArgumentException.class, () -> StringUtils.requireNotNullOrBlank(value));
    }

    @Test
    void requireNotNull() {
        assertThrows(NullPointerException.class, () -> StringUtils.requireNotNullOrBlank(null));
    }

    @Test
    void calculateHash() {
        String hash = "da32fc4a5dc93f8100b8d16cb741763f";
        String content = "{\n  \"Status\": \"ACTIVE\"\n}\n";
        assertEquals(hash,StringUtils.getContentHash(content));
    }

}
