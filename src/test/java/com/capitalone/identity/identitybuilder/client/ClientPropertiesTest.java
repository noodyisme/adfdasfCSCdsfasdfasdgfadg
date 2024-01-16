package com.capitalone.identity.identitybuilder.client;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientPropertiesTest {

    private final String prod = "prod";
    private final String qa = "qa";
    private final String dev = "dev";
    
    @ParameterizedTest
    @ValueSource(strings = {prod, qa, dev})
    void construct_ok(String environment) {
        ClientProperties properties = new ClientProperties(environment);
        assertEquals(ClientEnvironment.fromProperty(environment),properties.getClientEnvironment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev-local", "", "test"})
    void construct_ok_bucket_args(String environment) {
        assertThrows(IllegalArgumentException.class,()->new ClientProperties(environment));
    }

}
