package com.capitalone.identity.identitybuilder.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.function.Function;

public class FluxUtils {

    private FluxUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Provides missing 'debounce' operator from reactive streams specification (see link). Implementation
     * is based on switchMap behavior and is good enough for our needs.
     *
     * @return function that can be used with {@link Flux#transform(Function)} or {@link Flux#as(Function)} to
     * debounce emissions from a source stream
     * @see <a href="http://reactivex.io/documentation/operators/debounce.html">Reactive Streams Specification for
     * debounce</a>
     */
    public static <T> Function<Flux<T>, Publisher<T>> debounce(Duration duration) {
        return debounce(duration, null);
    }

    /**
     * See {@link #debounce(Duration)}
     */
    public static <T> Function<Flux<T>, Publisher<T>> debounce(Duration duration, Scheduler scheduler) {
        return sourceFlux -> sourceFlux
                // if switchMap receives a new value, then any previous value on delay is ignored
                .switchMap(value -> {
                    if (scheduler != null) {
                        return Flux.just(value).delayElements(duration, scheduler);
                    } else {
                        return Flux.just(value).delayElements(duration);
                    }
                });
    }
}
