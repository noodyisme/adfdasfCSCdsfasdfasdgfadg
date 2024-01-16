package com.capitalone.identity.identitybuilder.client.dynamic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class DynamicUpdateConfigurationPropertiesTest {

    @ParameterizedTest
    @ValueSource(strings = {"5s", "ABC", "", "1", "-PT5M", "PT1S"})
    void invalidIntervals(String interval) {
        assertThrows(RuntimeException.class, () -> new DynamicUpdateConfigurationProperties(null, interval));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT5S", "PT2M", "PT12H", "PT24H"})
    void validIntervals(String interval) {
        Duration parsed = Duration.parse(interval);
        DynamicUpdateConfigurationProperties properties = new DynamicUpdateConfigurationProperties(null, interval);
        assertEquals(parsed, properties.getInterval());
        assertNotNull(properties.getTimeOfDayUTC());
    }

    @Test
    void validTimeOfDay_default() {
        DynamicUpdateConfigurationProperties properties = new DynamicUpdateConfigurationProperties(null, "PT5S");
        // default to 4pm eastern (5pm in the summer)
        assertEquals(LocalTime.of(2, 0), properties.getTimeOfDayUTC());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12:00:00", "15:01:30"})
    void validTimeOfDay(String timeOfDay) {
        DynamicUpdateConfigurationProperties properties = new DynamicUpdateConfigurationProperties(timeOfDay, "PT5S");
        assertNotNull(properties.getTimeOfDayUTC());
    }


    @ParameterizedTest
    @ValueSource(strings = {"25:00:00", "-05:01:30", "", "1:30"})
    void invalidTimeOfDay(String timeOfDay) {
        assertThrows(RuntimeException.class, () -> new DynamicUpdateConfigurationProperties(timeOfDay, "PT5S"));
    }

}
