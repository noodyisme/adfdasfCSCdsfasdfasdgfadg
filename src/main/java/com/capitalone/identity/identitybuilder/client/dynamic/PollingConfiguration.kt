package com.capitalone.identity.identitybuilder.client.dynamic

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties
import reactor.core.scheduler.Scheduler
import java.time.Duration
import java.time.LocalTime

data class PollingConfiguration @JvmOverloads constructor(
    private val interval: Duration,
    private val timeOfDayUTC: LocalTime = LocalTime.of(2, 0),
    var externalPollingPropertiesObjectKey: String? = null,
    var scheduler: Scheduler? = null,
) : DynamicUpdateProperties {

    override fun getTimeOfDayUTC(): LocalTime = timeOfDayUTC

    override fun getInterval(): Duration = interval

}
