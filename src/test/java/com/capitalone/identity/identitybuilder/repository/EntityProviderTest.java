package com.capitalone.identity.identitybuilder.repository;

import com.capitalone.identity.identitybuilder.client.local.LocalDebugItemStore;
import com.capitalone.identity.identitybuilder.client.test.InMemoryItemStore;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityProviderTest {

    InMemoryItemStore itemStore = new InMemoryItemStore();
    @Mock
    ConfigStoreScanCompleted_Publisher scanPublisher;

    @Mock
    LocalDebugItemStore localDebugItemStore;
    private EntityProvider provider;

    @BeforeEach
    void createProvider() {
        this.provider = new EntityProvider(itemStore, Flux::never, scanPublisher);
    }

    @Test
    void testPatternMatching_access() {
        String objectName = "x/y/z/us_consumers/b/policy-c/1/access-control/45/policy-access.json";

        EntityInfo info = provider.getStoredObjectEntityBuilder(objectName, EntityType.values()).build();
        assertNotNull(info);

        assertEquals("x/y/z/us_consumers/b/policy-c/1/access-control/45/policy-access.json", info.getLocationPrefix());
        assertEquals("us_consumers/b/policy-c/1/access-control", info.getId());
        assertEquals(45, info.getPatchVersion());

        assertTrue(info instanceof EntityInfo.Access);
        EntityInfo.Access accessInfo = (EntityInfo.Access) info;
        assertEquals(1, accessInfo.getPolicyMajorVersion());
        assertEquals("policy-c", accessInfo.getPolicyShortName());

    }

    @Test
    void testGetStored_Access() {

        // metadata.json
        itemStore.addItem(
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/1/policy-access.json", "a"),
                        ""
                ),
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"),
                        ""
                ),
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/4/policy-access.json", "a"),
                        ""
                ),
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/2/access-control/100/policy-access.json", "a"),
                        ""
                )
        );

        final EntityInfo.Access expectedResult1 = new EntityInfo.Access(
                "us_consumers/b/c/1/access-control",
                "x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", 10,
                "c",
                "us_consumers/b/c", 1,
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"))
        );

        final EntityInfo.Access expectedResult2 = new EntityInfo.Access(
                "us_consumers/b/c/2/access-control",
                "x/y/z/us_consumers/b/c/2/access-control/100/policy-access.json",
                100, "c", "us_consumers/b/c",
                2, Collections.singleton(new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/2/access-control/100/policy-access.json", "a"))
        );

        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.ACCESS))
                .expectNext(expectedResult1)
                .expectNext(expectedResult2)
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.ACCESS, EntityType.POLICY))
                .expectNext(expectedResult1)
                .expectNext(expectedResult2)
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.POLICY)).verifyComplete();
        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.PIP)).verifyComplete();
        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.PIP, EntityType.POLICY)).verifyComplete();

    }

    @Test
    void testGetStored_Policy() {

        // metadata.json
        itemStore.addItem(
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/5/policy-metadata.json", "a"),
                        ""
                ),
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/5/process/policy_a.xml", "a"),
                        ""
                )
        );

        PolicyDefinition policy = new PolicyDefinition("x/y/z/us_consumers/b/c/1.0/5", "us_consumers/b/c", "c", 1, 0, 5);
        final EntityInfo.Policy expectedResult1 = new EntityInfo.Policy(policy,
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/5/policy-metadata.json", "a"),
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/5/process/policy_a.xml", "a")
                ))
        );


        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.POLICY))
                .expectNext(expectedResult1)
                .verifyComplete();


    }

    @Test
    void testGetStored_PolicyNonPatchNamespace() {

        // metadata.json
        itemStore.addItem(
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/policy-metadata.json", "a"),
                        ""
                ),
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/process/policy_a.xml", "a"),
                        ""
                )
        );

        PolicyDefinition policy = new PolicyDefinition("x/y/z/us_consumers/b/c/1.0", "us_consumers/b/c", "c", 1, 0, 0);
        final EntityInfo.Policy expectedResult1 = new EntityInfo.Policy(policy,
                new HashSet<>(Arrays.asList(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/policy-metadata.json", "a"),
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/process/policy_a.xml", "a")
                ))
        );


        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.POLICY))
                .expectNext(expectedResult1)
                .verifyComplete();


    }

    @Test
    void testGetStored_PolicyLegacyMetadataLocation() {

        // metadata.json
        itemStore.addItem(
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/metadata.json", "a"),
                        "{\"Versions_Supported\": [{\"Version\": \"1.0\", \"Status\": \"READY_FOR_PROD\"}]}"
                ),
                new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/process/policy_a.xml", "a"),
                        ""
                )
        );

        PolicyDefinition policy = new PolicyDefinition("x/y/z/us_consumers/b/c/1.0", "us_consumers/b/c", "c", 1, 0, 0);
        final EntityInfo.Policy expectedResult1 = new EntityInfo.Policy(policy,
                new HashSet<>(Collections.singletonList(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/process/policy_a.xml", "a")
                ))
        );


        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.POLICY))
                .expectNext(expectedResult1)
                .verifyComplete();

        Entity.Policy entity = (Entity.Policy) provider.getEntity(expectedResult1);
        assertNotNull(entity);
        assertEquals(EntityActivationStatus.AVAILABLE, entity.getEntityActivationStatus());
        assertEquals(1, entity.getCompileVersion());

    }

    @Test
    void testGetStored_PolicyThrowsIfNoMetadataFileFoundToDetermineActivationStatus() {

        // metadata.json
        itemStore.addItem(new ConfigStoreItem(
                new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/process/policy_a.xml", "a"),
                ""
        ));

        PolicyDefinition policy = new PolicyDefinition("x/y/z/us_consumers/b/c/1.0", "us_consumers/b/c", "c", 1, 0, 0);
        final EntityInfo.Policy expectedResult1 = new EntityInfo.Policy(policy,
                new HashSet<>(Collections.singletonList(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1.0/process/policy_a.xml", "a")
                ))
        );

        StepVerifier.withVirtualTime(() -> provider.getEntities(EntityType.POLICY))
                .expectNext(expectedResult1)
                .verifyComplete();

        RuntimeException e = assertThrows(IllegalArgumentException.class, () -> provider.getEntity(expectedResult1));

        String message = e.getMessage();
        assertTrue(message.contains("policy-metadata.json not found"));
        assertTrue(message.contains("x/y/z/us_consumers/b/c/metadata.json"));

    }

    @Test
    void testPatternMatching_pip() {
        String objectName = "x/y/us_consumers/routes/a/b/c/d/routefile.xml";
        EntityInfo info = provider.getStoredObjectEntityBuilder(objectName, EntityType.values()).build();

        assertNotNull(info);
        assertEquals("us_consumers/routes/a/b/c/d/routefile.xml", info.getId());
        assertEquals(objectName, info.getLocationPrefix());
        assertEquals(0, info.getPatchVersion());
        assertTrue(info instanceof EntityInfo.Pip);
    }

    @Test
    void getEntityUpdates_normal() {

        itemStore.addItem(new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/routes/a/b/c/d/routefile.xml", "b"),
                        ""
                )
        );

        itemStore.addItem(new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"),
                        ""
                )
        );


        final EntityInfo.Pip expectedResult1 = new EntityInfo.Pip(
                "z/routes/a/b/c/d/routefile.xml",
                "x/y/z/routes/a/b/c/d/routefile.xml",
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/routes/a/b/c/d/routefile.xml", "b"))
        );

        final EntityInfo.Access expectedResult2 = new EntityInfo.Access(
                "us_consumers/b/c/1/access-control",
                "x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json",
                10, "c", "us_consumers/b/c",
                1,
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"))
        );


        this.provider = new EntityProvider(itemStore,
                () -> Flux.just(new ScanRequest(0L)),
                scanPublisher);

        // start state matches -> no event
        List<EntityInfo> startState = Arrays.asList(expectedResult1, expectedResult2);
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(startState, EntityType.ACCESS))
                .verifyComplete();
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(startState, EntityType.ACCESS, EntityType.PIP))
                .verifyComplete();

        // start state is empty -> add event
        List<EntityInfo> startStateEmpty = Collections.emptyList();
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(startStateEmpty, EntityType.ACCESS))
                .expectNext(EntityState.Delta.add(expectedResult2))
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(startStateEmpty, EntityType.ACCESS, EntityType.PIP))
                .expectNext(EntityState.Delta.add(expectedResult1))
                .expectNext(EntityState.Delta.add(expectedResult2))
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(startStateEmpty, EntityType.POLICY))
                .verifyComplete();

    }

    @Test
    void getEntityUpdates_scanPublished() {

        final ScanRequest scanRequest = new ScanRequest(10L);
        this.provider = new EntityProvider(itemStore,
                () -> Flux.just(scanRequest),
                scanPublisher);

        AtomicReference<ConfigStoreScanCompleted> actualResult = new AtomicReference<>();

        // there are no updates, but a scan has occurred
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(Collections.emptyList(), EntityType.POLICY)).verifyComplete();

        // verify scan occurred
        verify(scanPublisher)
                .publishEvent(ArgumentMatchers.argThat(argument -> scanRequest.equals(argument.getRequest())));

    }

    @Test
    void getEntityUpdates_error_propagated() {
        final RuntimeException expectError = new RuntimeException("test");
        final ThrowingItemStore errorItemStore = new ThrowingItemStore(expectError);

        this.provider = new EntityProvider(errorItemStore,
                () -> Flux.just(new ScanRequest(0L)),
                scanPublisher);

        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(Collections.emptyList(), EntityType.ACCESS))
                .verifyErrorSatisfies((error) -> assertEquals(expectError, error));
    }

    @Test
    void getEntityUpdatesBatch_normal() {
        itemStore.addItem(new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/routes/a/b/c/d/routefile.xml", "b"),
                        ""
                )
        );
        itemStore.addItem(new ConfigStoreItem(
                        new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"),
                        ""
                )
        );

        final EntityInfo.Pip expectedResult1 = new EntityInfo.Pip(
                "z/routes/a/b/c/d/routefile.xml",
                "x/y/z/routes/a/b/c/d/routefile.xml",
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/routes/a/b/c/d/routefile.xml", "b"))
        );
        final EntityInfo.Access expectedResult2 = new EntityInfo.Access(
                "us_consumers/b/c/1/access-control",
                "x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json",
                10, "c", "us_consumers/b/c",
                1,
                Collections.singleton(new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/1/access-control/10/policy-access.json", "a"))
        );


        this.provider = new EntityProvider(itemStore,
                () -> Flux.just(new ScanRequest(0L)),
                scanPublisher);

        // start state matches -> no event
        List<EntityInfo> startState = Arrays.asList(expectedResult1, expectedResult2);
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdatesBatch(startState, EntityType.ACCESS))
                .assertNext(entityBatch -> assertTrue(entityBatch.isEmpty())).verifyComplete();
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdatesBatch(startState, EntityType.ACCESS, EntityType.PIP))
                .assertNext(entityBatch -> assertTrue(entityBatch.isEmpty())).verifyComplete();

        // start state is empty -> add event
        List<EntityInfo> startStateEmpty = Collections.emptyList();
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdatesBatch(startStateEmpty, EntityType.ACCESS))
                .expectNext(Collections.singletonList(EntityState.Delta.add(expectedResult2)))
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> provider.getEntityUpdatesBatch(startStateEmpty, EntityType.ACCESS, EntityType.PIP))
                .expectNext(Arrays.asList(EntityState.Delta.add(expectedResult1), EntityState.Delta.add(expectedResult2)))
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> provider.getEntityUpdates(startStateEmpty, EntityType.POLICY))
                .verifyComplete();
    }

    @Test
    void getEntityUpdatesBatch_scanPublished() {

        final ScanRequest scanRequest = new ScanRequest(10L);
        this.provider = new EntityProvider(itemStore,
                () -> Flux.just(scanRequest),
                scanPublisher);

        AtomicReference<ConfigStoreScanCompleted> actualResult = new AtomicReference<>();

        // there are no updates, but a scan has occurred
        StepVerifier.withVirtualTime(() -> provider.getEntityUpdatesBatch(Collections.emptyList(), EntityType.POLICY)).
                assertNext(entityBatch -> assertTrue(entityBatch.isEmpty())).verifyComplete();

        // verify scan occurred
        verify(scanPublisher)
                .publishEvent(ArgumentMatchers.argThat(argument -> scanRequest.equals(argument.getRequest())));

    }

    @Test
    void getEntityUpdatesBatch_error_propagated() {
        final RuntimeException expectError = new RuntimeException("test");
        final ThrowingItemStore errorItemStore = new ThrowingItemStore(expectError);

        this.provider = new EntityProvider(errorItemStore,
                () -> Flux.just(new ScanRequest(0L)),
                scanPublisher);

        StepVerifier.withVirtualTime(() -> provider.getEntityUpdatesBatch(Collections.emptyList(), EntityType.ACCESS))
                .verifyErrorSatisfies((error) -> assertEquals(expectError, error));
    }

    @Test
    void testCatchIOException() throws IOException {
        //arrange
        final Set<ConfigStoreItemInfo> componentItems = new HashSet<>();
        ConfigStoreItemInfo mockItemInfo = new ConfigStoreItemInfo("x/y/z/us_consumers/b/c/metadata.json", "a");
        ConfigStoreItem mockItem = new ConfigStoreItem(
                mockItemInfo,
                "{\"Versions_Supported\": [{\"Version\": \"1.0\", \"Status\": \"READY_FOR_PROD\"}]}");
        EntityInfo.Policy entityInfo =  mock(EntityInfo.Policy.class);
        this.provider = new EntityProvider(localDebugItemStore, () -> Flux.just(new ScanRequest(0L)), scanPublisher);

        when(localDebugItemStore.getSingleStoredItemInfo(any())).thenReturn(Optional.of(mockItemInfo));
        when(localDebugItemStore.getItem(any(ConfigStoreItemInfo.class))).thenThrow(IOException.class);
        when(entityInfo.getLocationPrefix()).thenReturn("a/b/c/1.0");
        when(entityInfo.getPolicyVersion()).thenReturn("1.0");
        when(entityInfo.getItemInfo()).thenReturn(componentItems);

        //act
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> provider.getEntity(entityInfo));

        //assert
        assertEquals(IOException.class, e.getCause().getClass());
    }


    private static final class ThrowingItemStore extends InMemoryItemStore {
        final RuntimeException error;

        private ThrowingItemStore(RuntimeException error) {
            this.error = error;
        }

        @Override
        public Flux<ConfigStoreItemInfo> getStoredItemInfo() {
            return Flux.error(error);
        }
    }

}
