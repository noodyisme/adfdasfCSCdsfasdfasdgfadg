package com.capitalone.identity.identitybuilder.model;


import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class EntityUtil {

    private EntityUtil() {
    }

    /**
     * When the upstream source flux emits a value, a {@link SnapshotHolder} is emitted that holds
     * the latest snapshot of {@link Versionable}s (e.g. Entities) gathered from the
     * {@code snapshotProvider} arg, a summary of changes from the previous snapshot in
     * the form of {@link SnapshotHolder#getChanges()}, and the value emitted from the source
     * flux in {@link SnapshotHolder#getSourceItem()}.
     * <p/>
     *
     * @param startItems            initial list of items that represents the first snapshot from which
     *                              deltas are calculated from. This snapshot is only used once. Subsequent
     *                              snapshots are pulled from {@code snapshotProvider}
     * @param snapshotItemsProvider provides a stream that represents a current snapshot
     * @param <T>                   {@link Versionable} objects from which snapshots can be calculated
     * @return a function that can be used in an argument to {@link Flux#transform(Function)}).
     */
    public static <T extends Versionable> Function<Flux<ScanRequest>, Publisher<SnapshotHolder<T>>> streamOfSnapshots(
            List<T> startItems, Supplier<Flux<T>> snapshotItemsProvider) {
        return sourceFlux -> sourceFlux
                .scan(new SnapshotHolder<>(startItems), (prevSnapshotHolder, sourceItem) -> {
                    final Flux<T> startStream = Flux.fromIterable(prevSnapshotHolder.getItems());
                    if (Schedulers.isInNonBlockingThread()) {
                        final String msg = "Blocking operations are not allowed on this thread. Please publish events" +
                                "from a non-blocking scheduler such as Schedulers.boundedElastic()";
                        throw new IllegalStateException(msg);
                    }
                    //Suppression Note: see non-blocking thread check above
                    //noinspection BlockingMethodInNonBlockingContext
                    final List<T> endItems = snapshotItemsProvider.get()
                            .collectList()
                            .block();

                    final Flux<T> endStream = Flux.fromIterable(Objects.requireNonNull(endItems));

                    //Suppression Note: collectList never emits null
                    //noinspection BlockingMethodInNonBlockingContext,ReactiveStreamsNullableInLambdaInTransform
                    return getDeltaStream(startStream, endStream)
                            .collectList()
                            .map(changes -> new SnapshotHolder<>(endItems, changes, sourceItem))
                            .block();
                })
                .filter(state -> state.getSourceItem() != null); // skips first emission, which is the start state
    }

    /**
     * Given two ordered streams of {@link Versionable} that represent start and end state, calculate
     * the "delta" between the two and emit as events.
     *
     * @param <T>        {@link Versionable}
     * @param startState ordered stream
     * @param endState   ordered stream
     * @return {@link Flux<EntityState.Delta<T>>} that represents the difference between the two
     * @throws IllegalArgumentException if start state or end state isn't ordered by location/path
     */
    public static <T extends Versionable> Flux<EntityState.Delta<T>> getDeltaStream(Flux<T> startState, Flux<T> endState) {

        // start process by assuming that all start entities will be deleted and that all end entities added
        final Flux<EntityState.Delta<T>> toDelete = startState
                .map(EntityState.Delta::delete)
                .scan(EntityUtil::enforceOrderedByLocation);

        final Flux<EntityState.Delta<T>> toAdd = endState
                .map(EntityState.Delta::add)
                .scan(EntityUtil::enforceOrderedByLocation);

        return toDelete.mergeComparingWith(toAdd,
                        Comparator.comparing(EntityUtil::getRootPrefixFromDelta))
                .bufferUntilChanged(delta -> delta.getEntityInfo().getId())
                .flatMap(deltas -> {
                    if (deltas.size() == 1) {
                        return Flux.just(deltas.get(0));
                    } else {
                        T v1 = deltas.get(0).getEntityInfo();
                        T v2 = deltas.get(1).getEntityInfo();
                        if (!v1.getVersion().equals(v2.getVersion())) {
                            return Flux.just(EntityState.Delta.update(v2));
                        } else {
                            return Flux.empty();
                        }
                    }
                });
    }

    /**
     *
     * @param delta
     * @return {@link String} concatenation of the {@link Versionable} object's id prefix and id from {@code delta}.
     */
    private static String getRootPrefixFromDelta(EntityState.Delta<?> delta) {
        return delta.getEntityInfo().getIdPrefix() + delta.getEntityInfo().getId();
    }

    private static <T extends Versionable> EntityState.Delta<T> enforceOrderedByLocation(
            EntityState.Delta<T> d1, EntityState.Delta<T> d2) {
        if (EntityUtil.getRootPrefixFromDelta(d2).compareTo(EntityUtil.getRootPrefixFromDelta(d1)) < 0) {
            final String message = String.format("Argument lists are not sorted in alphabetical " +
                            "order by path [locationPrefix '%s' observed before locationPrefix '%s']",
                    EntityUtil.getRootPrefixFromDelta(d1), EntityUtil.getRootPrefixFromDelta(d2));
            throw new IllegalArgumentException(message);
        } else {
            return d2;
        }
    }


    public static class SnapshotHolder<T extends Versionable> {

        private final List<T> state = new ArrayList<>();

        private final List<EntityState.Delta<T>> changes = new ArrayList<>();

        private final ScanRequest sourceItem;

        public SnapshotHolder(List<T> initialState) {
            this.state.addAll(initialState);
            this.sourceItem = null;
        }

        private SnapshotHolder(List<T> endState, List<EntityState.Delta<T>> changes, ScanRequest sourceItem) {
            this.state.addAll(endState);
            this.changes.addAll(changes);
            this.sourceItem = Objects.requireNonNull(sourceItem);
        }

        public List<T> getItems() {
            return state;
        }

        public List<EntityState.Delta<T>> getChanges() {
            return changes;
        }

        public ScanRequest getSourceItem() {
            return sourceItem;
        }

    }

}
