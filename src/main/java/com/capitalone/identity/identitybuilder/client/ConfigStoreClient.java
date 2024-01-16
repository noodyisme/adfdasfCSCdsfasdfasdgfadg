package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import com.capitalone.identity.identitybuilder.client.local.LocalDebugItemStore;
import com.capitalone.identity.identitybuilder.client.s3.ConfigStoreClientS3Configuration;
import com.capitalone.identity.identitybuilder.client.s3.S3ItemStore;
import com.capitalone.identity.identitybuilder.events.PolicyCoreEventPublisher;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.polling.*;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import reactor.core.publisher.Flux;

import java.util.List;

@PolicyCoreEventPublisher(value = {
        ConfigStoreScanCompleted.class,
        PollingConfigurationApplied.class,
        PollingConfigurationErrorOccurred.class,
})
public interface ConfigStoreClient {

    /**
     * Retrieve a full {@link Entity} object.
     *
     * @param entityInfo that can be obtained via {@link #getEntityInfo(EntityType, EntityType...)}
     * @return full entity info.
     * @throws ConfigStoreBusinessException when there is a problem creating the entity related to business content
     *                                      of the entity and not something external like an I/O or authentication
     *                                      issue. This exception means the entity stored in Config Store is not well-
     *                                      formed and should not be consumed by calling classes. Suggested workaround
     *                                      is to use {@link EntityInfo#getPriorVersion()} and attempt to load an old
     *                                      version of the entity.
     */
    Entity getEntity(EntityInfo entityInfo);

    /**
     * Retrieves a stream of all {@link EntityInfo} objects in configuration store that match the filter.
     * <p>
     * Note: {@link EntityType#POLICY} entities are not supported at this time.
     *
     * @param type       required first entity type that should be included in the stream
     * @param typeFilter additional entity types that should be included in the stream.
     * @return a {@link Flux<EntityInfo>} that represents all stored entities of argument type.
     */
    Flux<EntityInfo> getEntityInfo(EntityType type, EntityType... typeFilter);

    /**
     * Get a stream of {@link EntityState.Delta<EntityInfo>} objects based on the provided set of start entities.
     * <p>
     *
     * @param startList  initial list of entities from which changes will be calculated. This list must be sorted in natural order by {@link EntityInfo#getId()} ()}
     * @param type       required first entity type to monitor
     * @param typeFilter additional entity types to monitor
     * @return a stream of changes that represent when an entity is added, deleted, or updated in config store. This
     * stream will terminate in an error if the supplied list is not sorted in natural order by id.
     */
    default Flux<EntityState.Delta<EntityInfo>> getEntityUpdates(List<EntityInfo> startList, EntityType type, EntityType... typeFilter) {
        return this.getEntityUpdatesBatch(startList, type, typeFilter).flatMapIterable(batch -> batch);
    }

    /**
     * Get a stream of {@link EntityState.Delta<EntityInfo>} objects delivered in batches.
     * <p>
     *
     * @param startList  initial list of entities from which changes will be calculated. This list must be sorted in natural order by {@link EntityInfo#getId()} ()}
     * @param type       required first entity type to monitor
     * @param typeFilter additional entity types to monitor
     * @return a stream of changes, embedded in lists, that represent when an entity is added, deleted, or updated in config store. This
     * stream will terminate in an error if the supplied startList is not sorted in natural order by id.
     */
    Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> startList, EntityType type, EntityType... typeFilter);

    static ConfigStoreClient newS3Client(ConfigStoreClientS3Configuration configStoreClientS3Configuration,
                                         PollingConfiguration properties,
                                         ConfigStoreClient_ApplicationEventPublisher publisher) {

        ItemStore s3Store = new S3ItemStore(
                configStoreClientS3Configuration.getAwsClient(),
                configStoreClientS3Configuration.getBucketName(),
                configStoreClientS3Configuration.getRootPrefix());

        String pollingPropertiesName = properties.getExternalPollingPropertiesObjectKey();
        PollingConfigurationStreamProvider streamProvider = pollingPropertiesName != null
                ? new ArchaiusPollingConfigurationStreamProvider(configStoreClientS3Configuration, pollingPropertiesName)
                : null;

        SimpleScanRequester scanRequester = new SimpleScanRequester(
                properties,
                streamProvider,
                publisher != null ? publisher : ConfigStoreClient_ApplicationEventPublisher.EMPTY,
                properties.getScheduler());

        return new ConfigStoreClientImpl(s3Store, scanRequester,
                publisher != null ? publisher : ConfigStoreClient_ApplicationEventPublisher.EMPTY);
    }

    static ConfigStoreClient newLocalClient(String directory,
                                            PollingConfiguration properties,
                                            ConfigStoreClient_ApplicationEventPublisher publisher) {

        ItemStore itemStore = new LocalDebugItemStore(directory);
        SimpleScanRequester scanRequester = new SimpleScanRequester(
                properties, null,
                publisher != null ? publisher : ConfigStoreClient_ApplicationEventPublisher.EMPTY,
                properties.getScheduler());
        return new ConfigStoreClientImpl(itemStore, scanRequester,
                publisher != null ? publisher : ConfigStoreClient_ApplicationEventPublisher.EMPTY);
    }

}
