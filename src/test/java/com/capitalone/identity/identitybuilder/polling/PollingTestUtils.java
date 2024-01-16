package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.*;
import java.util.function.Function;

public class PollingTestUtils {

    /**
     * Get test scheduler initialized to UTC local time.
     *
     * @see #getTestScheduler(LocalTime, ZoneId)
     */
    static VirtualTimeScheduler getTestScheduler(LocalTime initialTimeUtc) {
        return getTestScheduler(initialTimeUtc, ZoneId.of("UTC"));
    }

    /**
     * Get scheduler initialized to a specific time of day. Needed to test polling function edge-cases.
     *
     * @param localTime local time to initialize the scheduler to
     * @param zoneId    time zone of the local time
     * @return {@link VirtualTimeScheduler} initialized to local time {@link reactor.test.StepVerifier} tests
     */
    static VirtualTimeScheduler getTestScheduler(LocalTime localTime, ZoneId zoneId) {
        return getTestScheduler(localTime, it -> it.atZone(zoneId).toInstant());
    }

    /**
     * Get test scheduler initialized to local time at zone offset.
     *
     * @see #getTestScheduler(LocalTime, ZoneId)
     */
    static VirtualTimeScheduler getTestScheduler(LocalTime localTime, ZoneOffset offset) {
        return getTestScheduler(localTime, it -> it.atOffset(offset).toInstant());
    }

    private static VirtualTimeScheduler getTestScheduler(LocalTime localTime, Function<LocalDateTime, Instant> instantProvider) {
        LocalDateTime localDateTime = LocalDate.now().atTime(localTime);
        final VirtualTimeScheduler testScheduler = VirtualTimeScheduler.getOrSet();
        Instant instant = instantProvider.apply(localDateTime);
        testScheduler.advanceTimeTo(instant);
        return testScheduler;
    }

    static DynamicUpdateProperties mockProperties(LocalTime time, Duration duration) {
        return new DynamicUpdateProperties() {
            @Override
            public LocalTime getTimeOfDayUTC() {
                return time;
            }

            @Override
            public Duration getInterval() {
                return duration;
            }
        };
    }
    
}
