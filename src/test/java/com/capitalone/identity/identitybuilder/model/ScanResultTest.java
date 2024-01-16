package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScanResultTest {

    @Mock
    ScanRequest request;

    @Test
    void testGetters() {
        ConfigStoreScanCompleted scanResult = new ConfigStoreScanCompleted(request);
        assertNotNull(scanResult.getRequest());
        assertTrue(scanResult.getEndActual() <= System.currentTimeMillis());
        assertEquals(request, scanResult.getRequest());
    }
}
