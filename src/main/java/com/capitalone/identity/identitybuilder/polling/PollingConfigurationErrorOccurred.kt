package com.capitalone.identity.identitybuilder.polling

import com.capitalone.identity.identitybuilder.events.ApplicationEvent
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties


/**
 * Occurs when there was an error applying polling configuration properties.
 */
@PolicyCoreEvent
data class PollingConfigurationErrorOccurred(
    val configuration: DynamicUpdateProperties,
    val error: Throwable
) : ApplicationEvent()
