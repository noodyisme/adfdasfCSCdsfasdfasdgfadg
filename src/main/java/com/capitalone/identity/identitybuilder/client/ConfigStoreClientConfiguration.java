package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
@ComponentScan(basePackageClasses = {DynamicUpdateConfigurationProperties.class, ClientProperties.class})
public class ConfigStoreClientConfiguration {

    ConfigStoreClientConfiguration() {
    }

    @Lazy
    @Conditional(ClientProperties.LibraryEnabled.class)
    @Bean
    ClientEnvironment getEnvironment(ClientProperties properties) {
        return properties.getClientEnvironment();
    }

    @Component
    @Conditional(ClientProperties.LibraryDisabled.class)
    static class NoOpConfigStoreClient implements ConfigStoreClient {

        @Override
        public Entity getEntity(EntityInfo entityInfo) {
            throw new IllegalStateException("Caller should not have entity info to get");
        }

        @Override
        public Flux<EntityInfo> getEntityInfo(EntityType type, EntityType... typeFilter) {
            return Flux.empty();
        }

        @Override
        public Flux<List<EntityState.Delta<EntityInfo>>> getEntityUpdatesBatch(List<EntityInfo> startList, EntityType type, EntityType... typeFilter) {
            return Flux.empty();
        }
    }
}
