package com.capitalone.identity.identitybuilder.polling

import com.capitalone.identity.identitybuilder.events.ApplicationEvent
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties


/**
 * Occurs when a polling configuration is applied successfully
 */
@PolicyCoreEvent
data class PollingConfigurationApplied(val configuration: DynamicUpdateProperties) : ApplicationEvent()
