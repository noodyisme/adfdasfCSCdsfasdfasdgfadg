package com.capitalone.identity.identitybuilder.util;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class FluxUtilsTest {

    @Test
    void debounceRealtime() throws InterruptedException {

        Flux<Long> flux = Flux.interval(Duration.ofMillis(10))
                .take(5)
                .transform(FluxUtils.debounce(Duration.ofMillis(100)))
                .take(2);

        AtomicLong result = new AtomicLong(0);
        Disposable subscribe = flux.subscribe(result::set);

        Thread.sleep(600);

        assertEquals(4, result.get());

        subscribe.dispose();

    }

    @Test
    void debounce_chatty() {

        VirtualTimeScheduler scheduler = VirtualTimeScheduler.getOrSet();

        final Sinks.Many<String> chattyPublisher = Sinks.many().multicast().onBackpressureBuffer();

        Duration debounceDuration = Duration.ofSeconds(1);
        Flux<String> flux = chattyPublisher.asFlux()
                .transform(FluxUtils.debounce(debounceDuration, scheduler));

        StepVerifier.withVirtualTime(() -> flux)
                .expectSubscription()
                // no events yet
                .expectNoEvent(Duration.ofMinutes(1))
                // event emitted after debounce duration
                .then(() -> chattyPublisher.tryEmitNext("a"))
                .expectNoEvent(Duration.ofMillis(10))
                .thenAwait(debounceDuration)
                .expectNext("a")
                // latest event emitted after debounce duration
                .then(() -> chattyPublisher.tryEmitNext("b"))
                .thenAwait(Duration.ofMillis(10))
                .then(() -> chattyPublisher.tryEmitNext("c"))
                .thenAwait(Duration.ofMillis(20))
                .then(() -> chattyPublisher.tryEmitNext("d"))
                .thenAwait(debounceDuration)
                .expectNext("d")
                // no events after
                .expectNoEvent(Duration.ofMinutes(1))
                // transformer obeys parent complete
                .then(chattyPublisher::tryEmitComplete)
                .verifyComplete();
    }
}
