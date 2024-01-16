package com.capitalone.identity.identitybuilder.model

import com.capitalone.identity.identitybuilder.events.ApplicationEvent
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent

/**
 * This event occurs on completion of a config store scan event (scan of S3 or file directory) and
 * holds information about what initiated the scan along with performance/duration.
 */
@PolicyCoreEvent
data class ConfigStoreScanCompleted @JvmOverloads constructor(
    val request: ScanRequest,
    val endActual: Long = System.currentTimeMillis()
) : ApplicationEvent(Metadata(request.startActual, endActual))
