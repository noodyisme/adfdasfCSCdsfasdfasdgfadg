package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PollingUtilTest {

    static List<DynamicUpdateProperties> provideBadPollingConfigurations() {
        return Arrays.asList(
                PollingTestUtils.mockProperties(null, null),
                PollingTestUtils.mockProperties(LocalTime.of(10, 0), Duration.ofHours(17)),
                PollingTestUtils.mockProperties(null, Duration.ofDays(1)),
                PollingTestUtils.mockProperties(LocalTime.of(10, 0), null)
        );
    }

    static List<Duration> invalidIntervals() {
        return Arrays.asList(
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofHours(-1),
                Duration.ofSeconds(11),
                Duration.ofHours(13),
                Duration.ofHours(5),
                Duration.ofMillis(11),
                Duration.ofDays(2),
                Duration.ofHours(25)
        );
    }

    /**
     * intervals must be whole factors of 24 hours to ensure target time can be met every day
     */
    static List<Duration> validIntervals() {
        return Arrays.asList(
                Duration.ofSeconds(2),
                Duration.ofSeconds(10),
                Duration.ofSeconds(15),
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),
                Duration.ofMinutes(20),
                Duration.ofHours(1),
                Duration.ofHours(2),
                Duration.ofHours(3),
                Duration.ofHours(4),
                Duration.ofHours(6),
                Duration.ofHours(12),
                Duration.ofHours(24),
                Duration.ofDays(1)
        );
    }

    @Test
    void poll_methods_null_scheduler_handling() {
        LocalTime time = LocalTime.NOON;
        Duration interval = Duration.ofSeconds(300);
        DynamicUpdateProperties properties = PollingTestUtils.mockProperties(time, interval);
        assertThrows(NullPointerException.class, () -> PollingUtil.pollEveryInterval(interval, time, null));
        assertDoesNotThrow(() -> PollingUtil.pollEveryInterval(properties, null));
    }

    @Test
    void poll_methods_null_scheduler_handling_allowsBlockingDownstream() {
        Duration interval = Duration.ofSeconds(2);
        DynamicUpdateProperties properties = PollingTestUtils.mockProperties(LocalTime.NOON, interval);

        // check that the default polling scheduler allows blocking downstream (hint: parallel doesn't allow this)
        Flux<ZonedDateTime> testFlux = PollingUtil.pollEveryInterval(properties, null)
                .take(1)
                .map(result -> {
                    assertFalse(Schedulers.isInNonBlockingThread());
                    return result;
                });

        StepVerifier.create(testFlux)
                .thenAwait(Duration.ofSeconds(2))
                .expectNextCount(1)
                .verifyComplete();

    }

    @ParameterizedTest
    @MethodSource("invalidIntervals")
    @NullSource
    void pollSomething_invalidInterval(Duration interval) {

        assertTrue(PollingUtil.isInvalidDuration(interval));

        final VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();
        LocalTime now = LocalTime.now();
        assertThrows(IllegalArgumentException.class, () -> PollingUtil.pollEveryInterval(interval, now, scheduler));

    }

    @ParameterizedTest
    @MethodSource("validIntervals")
    void pollSomething_validIntervals(Duration interval) {

        assertFalse(PollingUtil.isInvalidDuration(interval));

        final VirtualTimeScheduler scheduler = PollingTestUtils.getTestScheduler(LocalTime.NOON);
        StepVerifier.withVirtualTime(() -> PollingUtil.pollEveryInterval(interval, LocalTime.NOON, scheduler))
                .expectSubscription()
                // timeout before any events are emitted. Tests only that the stream starts ok.
                .verifyTimeout(Duration.ofSeconds(1));
    }

    @Test
    void poll_sameTimeEveryDay() {

        // set up start time
        final ZoneId zoneId = ZoneId.systemDefault();
        final VirtualTimeScheduler myTestScheduler = PollingTestUtils.getTestScheduler(LocalTime.of(23, 59, 59), zoneId);

        // typical configuration settings
        Duration interval = Duration.ofHours(24);
        LocalTime timeOfDay = LocalTime.of(16, 0);

        Flux<LocalTime> zonedDateTimeFlux = PollingUtil.pollEveryInterval(interval, timeOfDay, myTestScheduler)
                .map(emittedTime -> {
                    Instant emitInstant = Instant.ofEpochMilli(myTestScheduler.now(TimeUnit.MILLISECONDS));
                    ZonedDateTime timeOfEmission = ZonedDateTime.ofInstant(emitInstant, ZoneId.of("UTC"));
                    // emitted values match time of day when they are emitted
                    if (!emittedTime.equals(timeOfEmission)) {
                        fail("time of emission should match emission");
                        throw new IllegalArgumentException("time of day mismatch");
                    } else {
                        return emittedTime.toLocalTime();
                    }
                })
                .take(365);

        // simulate a delayed subscription
        myTestScheduler.advanceTimeBy(Duration.ofMinutes(10));

        // Given 24hr interval config, all emissions should be at the target time of day
        List<LocalTime> expected = Arrays.stream(new Integer[365]).map(ignored -> timeOfDay).collect(Collectors.toList());
        StepVerifier.withVirtualTime(() -> zonedDateTimeFlux)
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(10).minusSeconds(1))
                .thenAwait(Duration.ofDays(500))
                .expectNextSequence(expected)
                .verifyComplete();

    }

    @Test
    void pollSomething_frequentlyForLongTime() {

        // start time
        final ZoneId zoneId = ZoneId.systemDefault();
        final VirtualTimeScheduler myTestScheduler = PollingTestUtils.getTestScheduler(LocalTime.of(4, 35, 30), zoneId);

        LocalTime targetTod = LocalTime.of(16, 0);
        Duration interval = Duration.ofSeconds(30);

        int thirtySecondPeriodsIn100Days = 100 * 24 * 60 * 2;
        List<LocalTime> emissions = new ArrayList<>();
        Flux<LocalTime> fluxUnderTest = PollingUtil.pollEveryInterval(interval, targetTod, myTestScheduler)
                .map(ZonedDateTime::toLocalTime)
                .doOnNext(emissions::add)
                .take(thirtySecondPeriodsIn100Days);

        StepVerifier.withVirtualTime(() -> fluxUnderTest)
                .expectSubscription()
                .thenAwait(Duration.ofDays(100))
                .expectNextCount(thirtySecondPeriodsIn100Days)
                .verifyComplete();

        long targetTimeOfDayHitCount = emissions.stream().filter(time -> time.equals(targetTod)).count();
        assertEquals(100, targetTimeOfDayHitCount);
    }

    /**
     * Checks that polling will occur today if target time of day is in the future.
     */
    @Test
    void poll_next_today() {
        // start time
        LocalTime localTime = LocalTime.of(5, 20);
        final VirtualTimeScheduler myTestScheduler = PollingTestUtils.getTestScheduler(localTime, ZoneOffset.ofHours(-5));

        // configuration
        Duration interval = Duration.ofHours(24);
        LocalTime timeOfDay = LocalTime.of(10, 30);

        Flux<LocalTime> fluxUnderTest = PollingUtil.pollEveryInterval(interval, timeOfDay, myTestScheduler)
                .map(ZonedDateTime::toLocalTime)
                .take(1);

        StepVerifier.withVirtualTime(() -> fluxUnderTest)
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(10).minusSeconds(1))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(LocalTime.of(10, 30))
                .verifyComplete();

    }

    /**
     * if the current time happens to be exactly the time of day, then the poll is deferred to the next day
     * to maintain range that is (exclusive, inclusive) and reduce likelihood of off by one errors.
     */
    @Test
    void poll_next_now() {

        // start time
        LocalTime localTime = LocalTime.of(5, 30);
        final VirtualTimeScheduler myTestScheduler = PollingTestUtils.getTestScheduler(localTime, ZoneOffset.ofHours(-5));

        // configuration
        Duration interval = Duration.ofHours(24);
        LocalTime timeOfDay = LocalTime.of(10, 30);

        Flux<LocalTime> fluxUnderTest = PollingUtil.pollEveryInterval(interval, timeOfDay, myTestScheduler)
                .map(ZonedDateTime::toLocalTime)
                .take(1);

        StepVerifier.withVirtualTime(() -> fluxUnderTest)
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(10))
                .thenAwait(Duration.ofDays(1).minusMinutes(10))
                .expectNext(LocalTime.of(10, 30))
                .verifyComplete();

    }

    /**
     * Checks that polling will not occur today if target time of day is in the past.
     */
    @Test
    void poll_next_tomorrow() {

        // start time
        LocalTime localTime = LocalTime.of(5, 20, 1);
        final VirtualTimeScheduler myTestScheduler = PollingTestUtils.getTestScheduler(localTime, ZoneOffset.ofHours(-5));

        // configuration
        Duration interval = Duration.ofHours(24);
        LocalTime timeOfDay = LocalTime.of(10, 20);

        Flux<LocalTime> fluxUnderTest = PollingUtil.pollEveryInterval(interval, timeOfDay, myTestScheduler)
                .map(ZonedDateTime::toLocalTime)
                .take(1);

        StepVerifier.withVirtualTime(() -> fluxUnderTest)
                .expectSubscription()
                .expectNoEvent(Duration.ofDays(1).minusSeconds(1))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(LocalTime.of(10, 20))
                .verifyComplete();

    }


    @ParameterizedTest
    @ValueSource(strings = {
            "PT300S",
            "PT60S",
            "PT5M",
            "PT12H",
            "PT24H",
            "P1D",
    })
    void poll_dynamicConfigurationSignature(String intervalString) {
        // Interval to test
        final Duration interval = Duration.parse(intervalString);

        // Start at 10am utc
        LocalTime targetTime = LocalTime.NOON.minusHours(2);
        VirtualTimeScheduler testScheduler = PollingTestUtils.getTestScheduler(targetTime);

        DynamicUpdateProperties config = PollingTestUtils.mockProperties(targetTime, interval);

        Flux<LocalTime> fluxUnderTest = PollingUtil.pollEveryInterval(config, testScheduler)
                .map(ZonedDateTime::toLocalTime);

        // checks the first two emissions for compliance
        StepVerifier.withVirtualTime(() -> fluxUnderTest)
                .expectSubscription()
                .thenAwait(interval)
                .expectNext(targetTime.plus(interval))
                .thenAwait(interval)
                .expectNext(targetTime.plus(interval).plus(interval))
                .thenCancel()
                .verify();

    }

    @ParameterizedTest
    @MethodSource("provideBadPollingConfigurations")
    @NullSource
    void poll_errorConditions(DynamicUpdateProperties properties) {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();
        assertThrows(IllegalArgumentException.class, () ->
                PollingUtil.pollEveryInterval(properties, scheduler));

    }

}
