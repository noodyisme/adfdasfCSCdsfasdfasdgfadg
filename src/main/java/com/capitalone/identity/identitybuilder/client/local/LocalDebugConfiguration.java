package com.capitalone.identity.identitybuilder.client.local;

import com.capitalone.identity.identitybuilder.client.*;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Configuration
@Conditional(DevLocalProperties.DevLocalDebugRootDirectoryEnabled.class)
@Import(ConfigStoreClientConfiguration.class)
@ComponentScan
public class LocalDebugConfiguration {

    @Bean
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    ConfigStoreClient getLocalDebugClient(DevLocalProperties localDebugProperties,
                                          ClientProperties clientProperties,
                                          Optional<ScanRequester> scanRequester,
                                          Optional<ConfigStoreClient_ApplicationEventPublisher> scanPublisher) {

        return new LocalDebugConfigStoreClient(
                localDebugProperties.getRootDirectory(),
                clientProperties.getClientEnvironment(),
                scanRequester.orElse(Flux::never),
                scanPublisher.orElse(ConfigStoreClient_ApplicationEventPublisher.EMPTY));
    }

}
