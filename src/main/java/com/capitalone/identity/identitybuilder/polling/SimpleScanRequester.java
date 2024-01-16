package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.ConfigStoreConstants;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.model.ScanRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Optional;

@Lazy
@Conditional(DynamicUpdateConfigurationProperties.Enabled.class)
@Component
public class SimpleScanRequester implements ScanRequester {

    private static final Logger LOGGER = LogManager.getLogger(ConfigStoreConstants.LOGGER_NAME);

    final PollingConfigurationStreamProvider configurationProvider;
    final DynamicUpdateProperties properties;
    final Scheduler scheduler;
    private final ConfigStoreClient_ApplicationEventPublisher pollingEventListener;

    @Inject
    public SimpleScanRequester(@Autowired(required = false)
                               @Nullable DynamicUpdateProperties properties,
                               @Autowired(required = false)
                               @Nullable PollingConfigurationStreamProvider configurationProvider,
                               @Autowired(required = false)
                               @Nullable ConfigStoreClient_ApplicationEventPublisher pollingEventListener,
                               @Qualifier("identitybuilder.configstoreclient.pollingScheduler")
                               @Autowired(required = false)
                               @Nullable Scheduler scheduler) {
        this.configurationProvider = configurationProvider != null ? configurationProvider : Flux::empty;
        this.properties = properties;
        this.scheduler = scheduler;
        this.pollingEventListener = pollingEventListener != null ? pollingEventListener
                : ConfigStoreClient_ApplicationEventPublisher.EMPTY;
    }

    @Override
    public Flux<ScanRequest> getScanRequests() {
        // prepare a default configuration based on application properties in case provider is empty
        final Flux<DynamicUpdateProperties> defaultStream = Optional.ofNullable(properties)
                .map(Flux::just)
                .orElse(Flux.error(new IllegalArgumentException("Scan configuration not found")));

        return configurationProvider.getPollingConfigurationStream()
                .switchIfEmpty(defaultStream)
                .switchMap(this::createPollingEventStream)
                .map(zonedDateTime -> zonedDateTime.toInstant().toEpochMilli())
                .map(ScanRequest::new);
    }

    /**
     * Transform polling configuration into polling events. Notifies listener of success/failure of polling
     * configuration.
     *
     * @param configuration polling configuration settings
     * @return a stream of polling events
     */
    private Flux<ZonedDateTime> createPollingEventStream(DynamicUpdateProperties configuration) {
        try {
            LOGGER.info("Polling Configuration: {}", configuration);
            return PollingUtil.pollEveryInterval(configuration, scheduler)
                    .doOnSubscribe((subscription -> {
                        pollingEventListener.publishEvent(new PollingConfigurationApplied(configuration));
                    }));
        } catch (Exception e) {
            LOGGER.error("Likely Polling Configuration Error. Polling will pause until " +
                    "updated polling.properties configuration found in S3.", e);
            pollingEventListener.publishEvent(new PollingConfigurationErrorOccurred(configuration, e));
            return Flux.empty();
        }
    }

}
