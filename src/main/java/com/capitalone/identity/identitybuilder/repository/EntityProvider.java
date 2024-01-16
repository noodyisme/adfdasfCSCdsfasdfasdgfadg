package com.capitalone.identity.identitybuilder.repository;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyManifestJsonFileParser;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyManifestParser;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import org.springframework.lang.NonNull;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provide logic for retrieval of {@link Entity} objects.
 */
public class EntityProvider {

    private final ItemStore store;
    private final ClientEnvironment environment;
    private final ScanRequester scanRequester;
    private final ConfigStoreScanCompleted_Publisher scanPublisher;

    final Map<EntityType, EntityFactory> entityFactoryMap = new EnumMap<>(EntityType.class);

    public EntityProvider(@NonNull ItemStore store, @NonNull ScanRequester scanRequester,
                          @NonNull ConfigStoreScanCompleted_Publisher scanPublisher) {
        this(store, scanRequester, scanPublisher, ClientEnvironment.PROD);
    }

    public EntityProvider(@NonNull ItemStore store, @NonNull ScanRequester scanRequester,
                          @NonNull ConfigStoreScanCompleted_Publisher scanPublisher,
                          ClientEnvironment environment) {
        this.store = Objects.requireNonNull(store);
        this.environment = environment;
        this.scanRequester = Objects.requireNonNull(scanRequester);
        this.scanPublisher = Objects.requireNonNull(scanPublisher);

        entityFactoryMap.put(EntityType.PIP, store.getFactoryForEntityType(EntityType.PIP));
        entityFactoryMap.put(EntityType.ACCESS, store.getFactoryForEntityType(EntityType.ACCESS));
        entityFactoryMap.put(EntityType.POLICY, store.getFactoryForEntityType(EntityType.POLICY));

    }

    public Entity getEntity(EntityInfo info) {
        Set<ConfigStoreItem> items = info.getItemInfo()
                .parallelStream()
                .map(this::getConfigStoreItem)
                .collect(Collectors.toSet());

        PolicyManifestJsonFileParser manifestParser = new PolicyManifestJsonFileParser();
        if (info instanceof EntityInfo.Policy) {
            return getPolicy((EntityInfo.Policy) info, items, manifestParser);
        } else if (info instanceof EntityInfo.Pip) {
            return new Entity.Pip((EntityInfo.Pip) info, items);
        } else if (info instanceof EntityInfo.Access) {
            return new Entity.Access((EntityInfo.Access) info, items);
        } else {
            return new Entity.Simple(info, items);
        }
    }

    private Entity.Policy getPolicy(EntityInfo.Policy info, Set<ConfigStoreItem> items, PolicyManifestJsonFileParser manifestParser) {
        EntityActivationStatus status;
        if (info.getPatchVersion() == 0
                && info.getItemInfo().stream().noneMatch(item -> ConfigStoreItem.Type.POLICY_STATUS_SPARSE.equals(ConfigStoreItem.getTypeFromPath(item.getName())))) {
            String metadataLocation = info.getLocationPrefix().substring(0, info.getLocationPrefix().indexOf(info.getPolicyVersion()) - 1)
                    + "/metadata.json";
            status = store.getSingleStoredItemInfo(metadataLocation)
                    .map(itemInfo -> {
                        try {
                            return store.getItem(itemInfo);
                        } catch (IOException e) {
                            throw new IllegalArgumentException(e);
                        }
                    })
                    .flatMap(item -> {
                        try {
                            return manifestParser.parseVersionStatusFromLegacyMetadata(item.content, info.getPolicyVersion());
                        } catch (PolicyManifestParser.ManifestProcessingException e) {
                            throw new IllegalArgumentException(e);
                        }
                    })
                    .map(policyStatus -> policyStatus.toActivationStatus(environment))
                    .orElseThrow(() -> new IllegalArgumentException("Unable to resolve activation status. " +
                            "policy-metadata.json not found, and status could not otherwise be determined from " +
                            "metadata.json file at this location:=" + metadataLocation));

            return new Entity.Policy(info, items, status);
        } else {
            // sparse metadata.json location
            return new Entity.Policy(info, items);
        }
    }

    public Flux<EntityInfo> getEntities(EntityType type, EntityType... typeFilter) {
        return getEntities(combineEntityTypeFilters(type, typeFilter));
    }

    private Flux<EntityInfo> getEntities(EntityType[] entityFilter) {
        return store.getStoredItemInfo()
                // Populate entity builder with components objects
                .scan(Optional.<EntityBuilder>empty(), (entityBuilder, objectInfo) -> {
                    EntityBuilder curBuilder = entityBuilder.orElse(null);
                    if (curBuilder != null && curBuilder.addItem(objectInfo)) {
                        return entityBuilder;
                    } else {
                        EntityBuilder newBuilder = getStoredObjectEntityBuilder(objectInfo.getName(), entityFilter);
                        if (newBuilder != null) {
                            newBuilder.addItem(objectInfo);
                        }
                        return Optional.ofNullable(newBuilder);
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .bufferUntilChanged()
                .map(entityBuilders -> entityBuilders.get(0))
                .distinct()
                .map(EntityBuilder::build)
                // Group all versions with the same entity ID into a list
                .bufferUntilChanged(EntityInfo::getId)
                // Sort by version number
                .map(versionList -> versionList.stream()
                        .sorted(Comparator.comparingInt(EntityInfo::getPatchVersion))
                        .collect(Collectors.toList()))
                // Pack list into linked list where only final version is emitted
                .map(versionList -> versionList.stream().reduce((current, next) -> next.setPriorVersion(current)))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Flux<EntityState.Delta<EntityInfo>> getEntityUpdates(List<EntityInfo> startList,
                                                                EntityType type,
                                                                EntityType... typeFilter) {
        return getEntityUpdatesBatch(startList, type, typeFilter).flatMapIterable(batch -> batch);
    }

    public Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> startList,
                                                                           EntityType type, EntityType... typeFilter) {
        EntityType[] finalTypeFilter = combineEntityTypeFilters(type, typeFilter);
        Set<EntityType> filterList = new HashSet<>(Arrays.asList(finalTypeFilter));

        final List<EntityInfo> startState = startList.stream()
                .filter(entityInfo -> filterList.contains(entityInfo.getType()))
                .collect(Collectors.toList());
        return scanRequester.getScanRequests()
                .transform(EntityUtil.streamOfSnapshots(startState, () -> getEntities(finalTypeFilter)))
                .doOnNext(snapshot -> scanPublisher.publishEvent(new ConfigStoreScanCompleted(snapshot.getSourceItem())))
                .map(EntityUtil.SnapshotHolder::getChanges);
    }

    EntityBuilder getStoredObjectEntityBuilder(final String objectName, final EntityType[] typeFilter) {
        for (EntityType type : typeFilter) {
            EntityFactory factory = entityFactoryMap.get(type);
            if (factory != null) {
                EntityBuilder builder = factory.newBuilder(objectName);
                if (builder != null) {
                    return builder;
                }
            }
        }
        return null;
    }

    private EntityType[] combineEntityTypeFilters(EntityType type, EntityType... typeFilter) {
        Set<EntityType> filterList = new HashSet<>();
        filterList.add(type);
        filterList.addAll(Arrays.asList(typeFilter));
        return filterList.toArray(new EntityType[0]);
    }

    private ConfigStoreItem getConfigStoreItem(ConfigStoreItemInfo info) {
        try {
            return store.getItem(info);
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

}
