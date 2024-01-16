package com.capitalone.identity.identitybuilder.model

/**
 * Represents a request to scan this config store for changes to entities.
 */
class ScanRequest @JvmOverloads constructor(
    val startScheduled: Long,
    val scanType: ScanType = ScanType.POLL
) {
    val startActual: Long = System.currentTimeMillis()

    enum class ScanType {
        LOAD, POLL
    }
}
