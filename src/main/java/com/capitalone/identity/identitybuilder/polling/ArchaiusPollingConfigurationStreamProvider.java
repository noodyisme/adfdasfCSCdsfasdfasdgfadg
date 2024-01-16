package com.capitalone.identity.identitybuilder.polling;

import com.amazonaws.services.s3.AmazonS3;
import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import com.capitalone.identity.identitybuilder.client.DevLocalProperties;
import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.client.s3.ConfigStoreClientS3Configuration;
import com.capitalone.identity.identitybuilder.client.s3.S3BucketResolver;
import com.capitalone.identity.identitybuilder.client.s3.S3ConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.ConfigStoreBusinessException;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.netflix.config.*;
import com.netflix.config.sources.S3ConfigurationSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Lazy
@Conditional({
        DynamicUpdateConfigurationProperties.ExternalConfigurationEnabled.class,
        DevLocalProperties.DevLocalDisabled.class,
})
@Component
public class ArchaiusPollingConfigurationStreamProvider implements PollingConfigurationStreamProvider {

    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);
    private static final String BASE = "csc.dynamic-updates";
    private static final String TIME_OF_DAY_UTC = BASE + ".time-of-day-utc";
    private static final String POLLING_INTERVAL = BASE + ".polling-interval";

    private static final String FILE_NAME = "polling.properties";

    private static final int POLLING_DELAY_MILLIS = 300000;

    DynamicStringProperty duration;
    DynamicStringProperty timeOfDay;
    private static DynamicPropertyFactory dynamicPropertyFactory;

    ArchaiusPollingConfigurationStreamProvider() {
        duration = DynamicPropertyFactory.getInstance().getStringProperty(POLLING_INTERVAL, "");
        timeOfDay = DynamicPropertyFactory.getInstance().getStringProperty(TIME_OF_DAY_UTC, "");
    }

    public ArchaiusPollingConfigurationStreamProvider(String timeOfDayProp, String durationProp) {
        timeOfDay = DynamicPropertyFactory.getInstance().getStringProperty(timeOfDayProp, "");
        duration = DynamicPropertyFactory.getInstance().getStringProperty(durationProp, "");
    }

    public ArchaiusPollingConfigurationStreamProvider(@NonNull ConfigStoreClientS3Configuration configuration,
                                                      @NonNull String filename) {
        this(configuration, filename, true);
    }

    @Inject
    ArchaiusPollingConfigurationStreamProvider(@NonNull S3ConfigurationProperties s3ConfigurationProperties,
        @Value("${identitybuilder.policycore.feature.version-forwarder.enabled:false}") boolean throwIfNotFound) {
        this(new S3BucketResolver(s3ConfigurationProperties), s3ConfigurationProperties, FILE_NAME, throwIfNotFound);
    }

    ArchaiusPollingConfigurationStreamProvider(@NonNull S3BucketResolver s3BucketResolver,
                                               @NonNull S3ConfigurationProperties s3ConfigurationProperties,
                                               String fileName,
                                               boolean throwIfNotFound) {
        this(new ConfigStoreClientS3Configuration(
                        s3BucketResolver.getBucketName(),
                        "",
                        s3BucketResolver.getRegions(),
                        s3ConfigurationProperties.getCredentialProfileName(),
                        s3ConfigurationProperties.getIsProxyEnabled()),
                fileName,
                throwIfNotFound);
    }


    ArchaiusPollingConfigurationStreamProvider(@NonNull ConfigStoreClientS3Configuration configuration,
                                               String fileName, boolean throwIfNotFound) {
        AmazonS3 s3Client = configuration.getAwsClient();
        try {
            PolledConfigurationSource source = new S3ConfigurationSource(s3Client, configuration.getBucketName(),
                    configuration.getRootPrefix() + fileName);

            DynamicConfiguration dynamicConfiguration = new DynamicConfiguration(
                    source, new FixedDelayPollingScheduler(POLLING_DELAY_MILLIS, POLLING_DELAY_MILLIS, false));
            initOnce(dynamicConfiguration);
            duration = DynamicPropertyFactory.getInstance().getStringProperty(POLLING_INTERVAL, "");
            timeOfDay = DynamicPropertyFactory.getInstance().getStringProperty(TIME_OF_DAY_UTC, "");
        } catch (Exception e) {
            if (throwIfNotFound) {
                LOGGER.error("Failed to retrieve external polling file at startup. A polling.properties file must be present in the S3 bucket to complete startup.", e);
                throw new ConfigStoreBusinessException("Could not retrieve polling.properties file at startup.", e);
            } else {
                LOGGER.warn("Failed to retrieve external polling file at startup. Using the default polling properties set in application.properties", e);
                duration = null;
            }
        }
    }

    private static synchronized void initOnce(DynamicConfiguration dynamicConfiguration) {
        if (dynamicPropertyFactory == null) {
            dynamicPropertyFactory = DynamicPropertyFactory.initWithConfigurationSource(dynamicConfiguration);
        }
    }


    @Override
    public Flux<DynamicUpdateProperties> getPollingConfigurationStream() {
        if (duration == null) return Flux.empty();

        return Flux.combineLatest(
                        Flux.create(new ArchaiusStringPropertyConsumer(timeOfDay))
                                .distinctUntilChanged(),
                        Flux.create(new ArchaiusStringPropertyConsumer(duration))
                                .distinctUntilChanged(),
                        (BiFunction<String, String, DynamicUpdateProperties>) ArchaiusConfigurationUpdateProperties::new)
                //this will ensure when both time of day and the duration are updated at the same time, only one event is emitted
                .bufferTimeout(2, Duration.ofMillis(50))
                .map(list -> list.get(list.size() - 1));
    }

    public static class ArchaiusConfigurationUpdateProperties implements DynamicUpdateProperties {
        private final LocalTime timeOfDay;
        private final Duration interval;

        public ArchaiusConfigurationUpdateProperties(String timeOfDay, String interval) {
            LocalTime timeOfDay1;
            Duration interval1;

            if (!Strings.isBlank(timeOfDay)) {
                try {
                    timeOfDay1 = LocalTime.parse(timeOfDay);
                } catch (Exception e) {
                    LOGGER.error("Error parsing time of day: ", e);
                    timeOfDay1 = null;
                }
            } else {
                timeOfDay1 = null;
            }

            if (!Strings.isBlank(interval)) {
                try {
                    interval1 = Duration.parse(interval);
                } catch (Exception e) {
                    LOGGER.error("Error duration: ", e);
                    interval1 = null;
                }
            } else {
                interval1 = null;
            }

            this.interval = interval1;
            this.timeOfDay = timeOfDay1;

        }

        @Override
        public LocalTime getTimeOfDayUTC() {
            return timeOfDay;
        }

        @Override
        public Duration getInterval() {
            return interval;
        }

        @Override
        public String toString() {
            return "ArchaiusConfigurationUpdateProperties{" +
                    "timeOfDay=" + timeOfDay +
                    ", interval=" + interval +
                    '}';
        }
    }

    private static class ArchaiusStringPropertyConsumer implements Consumer<FluxSink<String>> {

        private final DynamicStringProperty property;

        private ArchaiusStringPropertyConsumer(DynamicStringProperty property) {
            this.property = property;
        }

        @Override
        public void accept(FluxSink<String> stringFluxSink) {
            final Runnable emitNonNull = () -> Optional.ofNullable(property.getValue()).ifPresent(stringFluxSink::next);
            // emit whenever property changes
            property.addCallback(emitNonNull);
            // emit current value of the property to start stream
            emitNonNull.run();
        }
    }

}
