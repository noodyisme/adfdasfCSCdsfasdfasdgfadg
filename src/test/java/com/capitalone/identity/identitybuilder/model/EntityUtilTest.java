package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class EntityUtilTest {

    @Test
    void getDelta_addNew() {
        Flux<Mock> start = Flux.empty();

        Flux<Mock> end = Flux.just(new Mock("C", "1"));

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(start, end))
                .expectNext(EntityState.Delta.add(new Mock("C", "1")))
                .verifyComplete();
    }

    @Test
    void getDelta_add() {
        Flux<Mock> start = Flux.just(
                new Mock("B", "1"),
                new Mock("D", "1")
        );

        Flux<Mock> end = Flux.just(
                new Mock("A", "1"),
                new Mock("B", "1"),
                new Mock("C", "1"),
                new Mock("D", "1"),
                new Mock("E", "1"),
                new Mock("F", "1")
        );

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(start, end))
                .expectNext(EntityState.Delta.add(new Mock("A", "1")))
                .expectNext(EntityState.Delta.add(new Mock("C", "1")))
                .expectNext(EntityState.Delta.add(new Mock("E", "1")))
                .expectNext(EntityState.Delta.add(new Mock("F", "1")))
                .verifyComplete();
    }

    @Test
    void getDelta_update() {
        Flux<Mock> start = Flux.just(
                new Mock("B", "1"),
                new Mock("D", "1")
        );

        Flux<Mock> end = Flux.just(
                new Mock("B", "2"),
                new Mock("D", "1")
        );

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(start, end))
                .expectNext(EntityState.Delta.update(new Mock("B", "2")))
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(end, start))
                .expectNext(EntityState.Delta.update(new Mock("B", "1")))
                .verifyComplete();
    }

    @Test
    void getDelta_delete() {
        Flux<Mock> start = Flux.just(
                new Mock("B", "1"),
                new Mock("D", "1")
        );

        Flux<Mock> end = Flux.just(
                new Mock("B", "1")
        );

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(start, end))
                .expectNext(EntityState.Delta.delete(new Mock("D", "1")))
                .verifyComplete();

    }

    @Test
    void getDeltaOutOfOrder_throws() {

        Flux<Mock> errorSet = Flux.just(
                new Mock("B", "1"),
                new Mock("A", "1"),
                new Mock("D", "1")
        );

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(Flux.empty(), errorSet))
                .verifyError(IllegalArgumentException.class);

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(errorSet, Flux.empty()))
                .verifyError(IllegalArgumentException.class);

    }

    @Test
    void getDeltaOutOfOrderLocation_throws() {
        Flux<Mock> errorSet = Flux.just(
                new Mock("A", "1", "B/"),
                new Mock("B", "1", "A/"),
                new Mock("C", "1", "D/")
        );

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(Flux.empty(), errorSet))
                .verifyError(IllegalArgumentException.class);

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(errorSet, Flux.empty()))
                .verifyError(IllegalArgumentException.class);
    }
    @Test
    void getDeltaNestedPathLocation_add() {

        Flux<Mock> start = Flux.just(
                new Mock("B", "1"),
                new Mock("D", "1")
        );

        Flux<Mock> end = Flux.just(
                new Mock("A", "1", "A/B/C/"),
                new Mock("B", "1", "A/B/C/D/"),
                new Mock("C", "1", "A/B/C/D/E/"),
                new Mock("B", "1"),
                new Mock("D", "1")

                );

        StepVerifier.withVirtualTime(() -> EntityUtil.getDeltaStream(start, end))
                .expectNext(EntityState.Delta.add(new Mock("A", "1", "A/B/C/")))
                .expectNext(EntityState.Delta.add(new Mock("B", "1", "A/B/C/D/")))
                .expectNext(EntityState.Delta.add(new Mock("C", "1", "A/B/C/D/E/")))
                .verifyComplete();
    }


    @Test
    void calculate_test() {
        List<Mock> startState = Arrays.asList(
                new Mock("B", "1"),
                new Mock("D", "1"));

        List<Mock> stateA = Arrays.asList(
                new Mock("A", "1"),
                new Mock("B", "2"));

        List<Mock> stateB = Arrays.asList(
                new Mock("B", "1"),
                new Mock("D", "1"));

        List<Flux<Mock>> states = Arrays.asList(
                Flux.fromIterable(stateA),
                Flux.fromIterable(stateB)
        );

        AtomicInteger stateIndex = new AtomicInteger(0);
        Duration requesterDelay = Duration.ofSeconds(1);

        ScanRequest request1 = new ScanRequest(1L);
        ScanRequest request2 = new ScanRequest(2L);
        Flux<EntityUtil.SnapshotHolder<Mock>> deltaFlux = Flux.just(request1, request2)
                .delayElements(requesterDelay, VirtualTimeScheduler.getOrSet())
                .transform(EntityUtil.streamOfSnapshots(startState, () -> states.get(stateIndex.getAndIncrement())));

        StepVerifier.withVirtualTime(() -> deltaFlux)
                .expectSubscription()
                .expectNoEvent(requesterDelay)
                .expectNextMatches(snapshot -> snapshot.getSourceItem() == request1
                        && snapshot.getItems().equals(stateA)
                        && snapshot.getChanges().equals(Arrays.asList(
                        EntityState.Delta.add(new Mock("A", "1")),
                        EntityState.Delta.update(new Mock("B", "2")),
                        EntityState.Delta.delete(new Mock("D", "1"))
                )))
                .expectNoEvent(requesterDelay)
                .expectNextMatches(snapshot -> snapshot.getSourceItem() == request2
                        && snapshot.getItems().equals(stateB)
                        && snapshot.getChanges().equals(Arrays.asList(
                        EntityState.Delta.delete(new Mock("A", "1")),
                        EntityState.Delta.update(new Mock("B", "1")),
                        EntityState.Delta.add(new Mock("D", "1"))
                )))
                .verifyComplete();

    }

    @Test
    void snapshotErrorTest() {

        UnsupportedOperationException error = new UnsupportedOperationException("test");
        Supplier<Flux<Mock>> buggySupplier = () -> {
            throw error;
        };

        Flux<EntityUtil.SnapshotHolder<Mock>> deltaFlux = Flux.just(new ScanRequest(1L))
                .transform(EntityUtil.streamOfSnapshots(Collections.emptyList(), buggySupplier));

        StepVerifier.withVirtualTime(() -> deltaFlux)
                .expectSubscription()
                .verifyErrorSatisfies(throwable -> assertEquals(error, throwable));

    }

    @Test
    void snapshotErrorTest_blockingThread() {

        Scheduler blockingScheduler = Schedulers.parallel();

        Flux<EntityUtil.SnapshotHolder<Mock>> deltaFlux = Flux.just(new ScanRequest(1L))
                .publishOn(blockingScheduler)
                .transform(EntityUtil.streamOfSnapshots(Collections.emptyList(), Flux::empty));

        StepVerifier.create(deltaFlux)
                .expectSubscription()
                .verifyErrorSatisfies(error -> {
                    assertTrue(error instanceof IllegalStateException);
                    assertTrue(error.getMessage().contains("Schedulers.boundedElastic()"));
                });

    }

    private static final class Mock implements Versionable {

        private final String id;
        private final String version;
        private final String idPrefix;
        private Mock(String id, String version) {
            this.id = id;
            this.version = version;
            this.idPrefix = "root/";
        }

        private Mock(String id, String version, String idPrefix) {
            this.id = id;
            this.version = version;
            this.idPrefix = idPrefix;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getIdPrefix() {
            return idPrefix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Mock that = (Mock) o;
            return getId().equals(that.getId()) &&
                    getVersion().equals(that.getVersion());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getVersion());
        }

        @Override
        public String toString() {
            return "MockVersionable{" +
                    "id='" + id + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
