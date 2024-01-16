package com.capitalone.identity.identitybuilder.client.dynamic;

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.polling.PollingUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

@Lazy
@Conditional(DynamicUpdateConfigurationProperties.Enabled.class)
@Component
public class DynamicUpdateConfigurationProperties implements DynamicUpdateProperties {

    private final Duration pollingInterval;
    private final LocalTime timeOfDay;

    private static final String BASE = "csc.dynamic-updates";
    public static final String ENABLED = BASE + ".enabled";
    private static final String TIME_OF_DAY_UTC = BASE + ".time-of-day-utc";
    private static final String POLLING_INTERVAL = BASE + ".polling-interval";
    private static final String EXTERNAL_CONFIGURATION_ENABLED = BASE + ".external-configuration.enabled";

    public DynamicUpdateConfigurationProperties(
            @Value("${" + TIME_OF_DAY_UTC + ":#{null}}") String timeOfDayUTC,
            @Value("${" + POLLING_INTERVAL + ":#{null}}") String interval) {

        this.pollingInterval = Optional.ofNullable(interval).map(Duration::parse)
                .map(this::validateDuration)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unspecified " +
                        "polling interval [%s]. Property must be specified when %s=true", POLLING_INTERVAL, ENABLED)));

        this.timeOfDay = Optional.ofNullable(timeOfDayUTC)
                .map(LocalTime::parse)
                .orElse(LocalTime.of(2, 0));
    }

    private Duration validateDuration(Duration input) {
        if (PollingUtil.isInvalidDuration(input)) {
            throw new IllegalArgumentException(String.format("invalid interval %s", input));
        }
        return input;
    }

    @Override
    @NonNull
    public LocalTime getTimeOfDayUTC() {
        return timeOfDay;
    }

    @Override
    @NonNull
    public Duration getInterval() {
        return pollingInterval;
    }

    @Override
    public String toString() {
        return "DynamicUpdateConfigurationProperties{" +
                "pollingInterval=" + pollingInterval +
                ", timeOfDay=" + timeOfDay +
                '}';
    }


    public static final class Enabled extends AllNestedConditions {

        Enabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(value = ENABLED, matchIfMissing = true)
        public static class CscDynamicUpdatesEnabled {
        }

    }

    public static final class Disabled extends NoneNestedConditions {

        Disabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(Enabled.class)
        public static class CscDynamicUpdatesDisabled {
        }
    }

    public static final class ExternalConfigurationEnabled extends AllNestedConditions {
        ExternalConfigurationEnabled() {super(ConfigurationPhase.PARSE_CONFIGURATION);}

        @ConditionalOnProperty(value = EXTERNAL_CONFIGURATION_ENABLED, matchIfMissing = true)
        public static class CscDynamicUpdatesExternalConfigurationEnabled {
        }

        @Conditional(Enabled.class)
        public static class DynamicUpdatesEnabled {
        }

    }

    public static final class ExternalConfigurationDisabled extends NoneNestedConditions {
        ExternalConfigurationDisabled() {super(ConfigurationPhase.PARSE_CONFIGURATION);}

        @Conditional(ExternalConfigurationEnabled.class)
        public static class CscDynamicUpdatesExternalConfigurationDisabled {
        }
    }
}
