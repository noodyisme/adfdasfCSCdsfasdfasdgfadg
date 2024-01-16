package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.model.ScanRequest;
import reactor.core.publisher.Flux;

public interface ScanRequester {
    Flux<ScanRequest> getScanRequests();
}
