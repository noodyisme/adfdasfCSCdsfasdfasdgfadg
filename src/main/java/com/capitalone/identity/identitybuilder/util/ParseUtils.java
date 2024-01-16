package com.capitalone.identity.identitybuilder.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ParseUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ParseUtils() {
    }
}
