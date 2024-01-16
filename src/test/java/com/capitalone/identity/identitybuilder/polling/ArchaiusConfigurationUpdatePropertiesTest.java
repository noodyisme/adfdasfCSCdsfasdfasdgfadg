package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.polling.ArchaiusPollingConfigurationStreamProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ArchaiusConfigurationUpdatePropertiesTest {

    @ParameterizedTest
    @ValueSource(strings = {"5s", "ABC", "", "1"})
    void invalidIntervals(String interval) {
        final ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties archaiusConfigurationUpdateProperties =
                assertDoesNotThrow(() -> new ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties(null, interval));
        assertNull(archaiusConfigurationUpdateProperties.getInterval());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT5S", "PT2M", "PT12H", "PT24H", "-PT5M", "PT1S"})
    void validIntervals(String interval) {
        Duration parsed = Duration.parse(interval);
        ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties properties = new ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties(null, interval);
        assertEquals(parsed, properties.getInterval());
        assertNull(properties.getTimeOfDayUTC());
    }


    @ParameterizedTest
    @ValueSource(strings = {"12:00:00", "15:01:30"})
    void validTimeOfDay(String timeOfDay) {
        ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties properties = new ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties(timeOfDay, "PT5S");
        assertNotNull(properties.getTimeOfDayUTC());
    }


    @ParameterizedTest
    @ValueSource(strings = {"25:00:00", "-05:01:30", "", "1:30"})
    void invalidTimeOfDay(String timeOfDay) {
        final ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties archaiusConfigurationUpdateProperties =
                assertDoesNotThrow(() -> new ArchaiusPollingConfigurationStreamProvider.ArchaiusConfigurationUpdateProperties(timeOfDay, "PT5S"));
        assertNull(archaiusConfigurationUpdateProperties.getTimeOfDayUTC());
    }
}