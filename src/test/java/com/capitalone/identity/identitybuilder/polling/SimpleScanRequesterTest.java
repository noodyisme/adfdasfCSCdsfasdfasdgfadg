package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.model.ScanRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SimpleScanRequesterTest {
    @Mock
    ConfigStoreClient_ApplicationEventPublisher applicationEventPublisher;

    @Test
    void emptyConfiguration() {
        VirtualTimeScheduler testScheduler = VirtualTimeScheduler.getOrSet();
        ScanRequester scanRequester = new SimpleScanRequester(null, null, applicationEventPublisher, testScheduler);
        StepVerifier.withVirtualTime(scanRequester::getScanRequests)
                .verifyError(RuntimeException.class);
    }

    @Test
    void getScanRequests_defaultPoll() {
        VirtualTimeScheduler testScheduler = PollingTestUtils.getTestScheduler(LocalTime.NOON.minusHours(1));//VirtualTimeScheduler.getOrSet();

        DynamicUpdateProperties mockProperties = PollingTestUtils.mockProperties(LocalTime.NOON, Duration.ofMinutes(5));
        ScanRequester scanRequester = new SimpleScanRequester(mockProperties,
                null,
                applicationEventPublisher,
                testScheduler);

        StepVerifier.withVirtualTime(() -> scanRequester.getScanRequests().take(10))
                .thenAwait(Duration.ofMinutes(20))
                .expectNextCount(4)
                .thenAwait(Duration.ofMinutes(30))
                .expectNextCount(6)
                .verifyComplete();

    }

    @Test
    void getScanRequests_dynamicPoll() {
        final VirtualTimeScheduler scheduler = PollingTestUtils.getTestScheduler(LocalTime.NOON);

        TestPublisher<DynamicUpdateProperties> propertiesPublisher = TestPublisher.create();

        ScanRequester scanRequester = new SimpleScanRequester(null,
                propertiesPublisher::flux,
                applicationEventPublisher,
                scheduler);

        StepVerifier.withVirtualTime(() -> scanRequester.getScanRequests()
                        .doOnNext(result -> {
                            System.out.println("Poll Event: " + Instant.ofEpochMilli(result.getStartScheduled()));
                        }))
                // simulate an invalid first configuration from config properties provider
                .then(() -> propertiesPublisher.next(PollingTestUtils.mockProperties(null, null)))
                // set illegal interval and expect no events
                .expectNoEvent(Duration.ofHours(1))
                // set interval dynamically
                .then(() -> propertiesPublisher.next(PollingTestUtils.mockProperties(LocalTime.NOON, Duration.ofMinutes(5))))
                .thenAwait(Duration.ofMinutes(25))
                .expectNextCount(5)
                // set illegal interval and expect no events
                .then(() -> propertiesPublisher.next(PollingTestUtils.mockProperties(LocalTime.NOON, Duration.ofDays(2))))
                .expectNoEvent(Duration.ofDays(60))
                // manually end stream
                .then(propertiesPublisher::complete)
                .verifyComplete();

    }

    @Test
    void polling_listener_called_success() {

        DynamicUpdateProperties configurationProperties = PollingTestUtils.mockProperties(LocalTime.NOON, Duration.ofDays(1));

        ScanRequester scanRequester = new SimpleScanRequester(null,
                () -> Flux.just(configurationProperties),
                applicationEventPublisher,
                PollingTestUtils.getTestScheduler(LocalTime.NOON.minusHours(1)));

        StepVerifier.withVirtualTime(scanRequester::getScanRequests)
                .thenAwait(Duration.ofHours(1))
                .expectNextCount(1)
                // end stream before next event arrives
                .verifyTimeout(Duration.ofHours(6));

        Mockito.verify(applicationEventPublisher, times(1))
                .publishEvent(ArgumentMatchers.argThat((ArgumentMatcher<PollingConfigurationApplied>)
                        event -> event.getConfiguration() == configurationProperties));
        Mockito.verifyNoMoreInteractions(applicationEventPublisher);


    }

    @Test
    void polling_listener_called_failure() {

        DynamicUpdateProperties invalidConfiguration = PollingTestUtils.mockProperties(LocalTime.NOON, Duration.ofDays(2));

        ScanRequester scanRequester = new SimpleScanRequester(null,
                () -> Flux.just(invalidConfiguration),
                applicationEventPublisher,
                PollingTestUtils.getTestScheduler(LocalTime.NOON));

        StepVerifier.withVirtualTime(scanRequester::getScanRequests)
                .verifyComplete();

        Mockito.verify(applicationEventPublisher, times(1))
                .publishEvent(ArgumentMatchers.argThat((ArgumentMatcher<PollingConfigurationErrorOccurred>)
                        event -> event.getConfiguration() == invalidConfiguration));
        Mockito.verifyNoMoreInteractions(applicationEventPublisher);

    }

    @Test
    void polling_listener_throws_ignored() {

        TestPublisher<DynamicUpdateProperties> configProvider = TestPublisher.create();
        ScanRequester scanRequester = new SimpleScanRequester(null,
                configProvider::flux,
                applicationEventPublisher,
                PollingTestUtils.getTestScheduler(LocalTime.NOON.minusHours(1)));


        StepVerifier.withVirtualTime(scanRequester::getScanRequests)
                .then(() -> configProvider.next(
                        PollingTestUtils.mockProperties(LocalTime.NOON, Duration.ofDays(1))
                ))
                .thenAwait(Duration.ofHours(1))
                .expectNextCount(1)
                .then(() -> configProvider.next(
                        PollingTestUtils.mockProperties(null, null)
                ))
                // expected behavior of a bad polling configuration is to never emit
                .verifyTimeout(Duration.ofDays(5));

    }

}
