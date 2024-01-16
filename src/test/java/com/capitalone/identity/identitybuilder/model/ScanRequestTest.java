package com.capitalone.identity.identitybuilder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanRequestTest {

    final long startScheduled = System.currentTimeMillis() - 10;
    final ScanRequest request = new ScanRequest(startScheduled, ScanRequest.ScanType.POLL);

    @Test
    void getStartActual() {
        assertTrue(request.getStartScheduled() < request.getStartActual());
    }

    @Test
    void getStartScheduled() {
        assertEquals(startScheduled, request.getStartScheduled());
    }

    @Test
    void getScanType() {
        assertEquals(ScanRequest.ScanType.POLL, request.getScanType());
    }

}
