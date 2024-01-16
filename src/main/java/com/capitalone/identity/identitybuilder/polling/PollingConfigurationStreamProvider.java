package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import reactor.core.publisher.Flux;

public interface PollingConfigurationStreamProvider {

    Flux<DynamicUpdateProperties> getPollingConfigurationStream();
}
