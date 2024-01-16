package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;

public class PollingUtil {

    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);
    private static final long ONE_DAY_MILLIS = Duration.ofDays(1).toMillis();
    private static final long MINIMUM_INTERVAL = Duration.ofSeconds(2).toMillis();

    private PollingUtil() {
    }

    public static boolean isInvalidDuration(@Nullable Duration duration) {
        if (duration == null) return true;

        final long millis = duration.toMillis();
        return (millis < MINIMUM_INTERVAL || millis > ONE_DAY_MILLIS || (ONE_DAY_MILLIS % millis) != 0);
    }

    /**
     * Similar to {@link Flux#interval}, but with key modifications.
     * <ol>
     *     <li>Instead of emitting {@code long} values, this flux emits {@link ZonedDateTime} that represents the
     *     time that the emissions were meant to occur (making it auditable).</li>
     *     <li>Calibrates the interval to match a time-of-day target.</li>
     *     <li>Intervals limited to a maximum of 24hrs, and must be whole factor of 24 hours to ensure time of day
     *     target can be met.</li>
     * </ol>
     *
     * @param interval        how often emissions should occur. Must be 24 hours or a factor of 24 hours
     *                        (e.g. 12hrs, 2hrs, 30min, 20s, etc.).
     * @param timeOfDayTarget emissions will be calibrated to occur at this time of day
     * @param scheduler       used for calculating intervals/timing
     * @return Flux that emits values on the provided interval, or complete with error if interval is invalid.
     */
    static Flux<ZonedDateTime> pollEveryInterval(@NonNull Duration interval,
                                                 @NonNull LocalTime timeOfDayTarget,
                                                 @NonNull Scheduler scheduler) {
        Objects.requireNonNull(scheduler);
        if (isInvalidDuration(interval)) {
            String message = String.format("Specified duration '%s' must be a factor of 1 day in milliseconds", interval);
            throw new IllegalArgumentException(message);
        }

        // defer used because calculation of current time should be based on subscription time
        return Flux.defer(() -> {
            final ZoneId timezone = ZoneId.of("UTC");
            final Instant instant = Instant.ofEpochMilli(scheduler.now(TimeUnit.MILLISECONDS));
            final ZonedDateTime now = ZonedDateTime.ofInstant(instant, timezone);
            LocalDateTime firstPollWallTime = getFirstPollWallTime(now, timeOfDayTarget, interval);
            ZonedDateTime firstPoll = firstPollWallTime.atZone(timezone);
            Duration firstPollDelay = Duration.ofMillis(now.until(firstPoll, MILLIS));

            return Flux.interval(firstPollDelay, interval, scheduler)
                    .map(index -> firstPoll.plus(interval.toMillis() * index, MILLIS));
        });

    }

    /**
     * Calls {@link #pollEveryInterval(Duration, LocalTime, Scheduler)} with data from supplied argument
     * if the supplied argument has all non null values. If nulls are found then returned stream will end with
     * error.
     *
     * @param config    if null or object has null values, {@link IllegalArgumentException} will be thrown
     * @param scheduler used for testing. If set to {@code null} then {@link Schedulers#boundedElastic()} is used
     * @see PollingUtil#pollEveryInterval(Duration, LocalTime, Scheduler)
     */
    static Flux<ZonedDateTime> pollEveryInterval(@Nullable DynamicUpdateProperties config,
                                                 @Nullable Scheduler scheduler) {

        final Duration interval = config != null ? config.getInterval() : null;
        final LocalTime timeOfDayUTC = config != null ? config.getTimeOfDayUTC() : null;
        if (interval == null || timeOfDayUTC == null) {
            String message = String.format("Invalid Poll Configuration, null property in config: %s", config);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        } else {
            return PollingUtil.pollEveryInterval(
                    interval,
                    timeOfDayUTC,
                    Optional.ofNullable(scheduler).orElse(Schedulers.boundedElastic())
            );
        }
    }


    /**
     * @return first poll time occurs after the 'now' argument, calibrated to timeOfDayStart.
     */
    private static LocalDateTime getFirstPollWallTime(@NonNull ZonedDateTime now, @NonNull LocalTime timeOfDayStart,
                                                      Duration interval) {

        LocalDateTime actualDateTime = now.toLocalDateTime();

        LocalDateTime targetDateTime = timeOfDayStart.atDate(now.toLocalDate());

        // get first target date time that occurs in the future
        while (targetDateTime.isBefore(actualDateTime) || targetDateTime.isEqual(actualDateTime)) {
            targetDateTime = targetDateTime.plus(interval);
        }

        // scan backward by interval to find first interval that occurs in the future
        LocalDateTime adjustedDateTime = targetDateTime.minus(interval);
        while (adjustedDateTime.isAfter(actualDateTime)) {
            targetDateTime = adjustedDateTime;
            adjustedDateTime = adjustedDateTime.minus(interval);
        }

        return targetDateTime;
    }

}
