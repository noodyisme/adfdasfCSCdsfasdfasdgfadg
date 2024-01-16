package com.capitalone.identity.identitybuilder.configmanagement;

import java.io.Serializable;
import java.util.Map;

public interface ConfigMatchingStrategy {
    Map<String, Serializable> getConfig(String useCase,
                                        Map<String, Map<String, Serializable>> useCaseMap,
                                        Map<String, Serializable> defaults,
                                        Map<String, Serializable> features);
}
