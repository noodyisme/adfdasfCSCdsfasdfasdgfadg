package com.capitalone.identity.identitybuilder.client.dynamic;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PollingConfigurationTest {

    @Test
    void checkConstruct() {
        PollingConfiguration configuration = new PollingConfiguration(Duration.ofSeconds(4));
        assertEquals(LocalTime.of(2, 0), configuration.getTimeOfDayUTC());
        assertEquals(Duration.ofSeconds(4), configuration.getInterval());
        assertNull(configuration.getExternalPollingPropertiesObjectKey());
        assertNull(configuration.getScheduler());
    }

}
