package com.capitalone.identity.identitybuilder.polling;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PollIntervalParsingTest {

    @Test
    void getTime() {
        Duration duration = Duration.ofHours(2);
        String twoHr = duration.toString();
        System.out.println(twoHr);
        assertNotNull(Duration.parse(twoHr));
    }

    @Test
    void getTimeOfDay() {
        String time = "23:59:59";
        LocalTime parse = LocalTime.parse(time);
        assertNotNull(parse);
    }
}