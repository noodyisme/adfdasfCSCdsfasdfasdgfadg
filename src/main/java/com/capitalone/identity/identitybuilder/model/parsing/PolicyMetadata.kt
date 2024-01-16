package com.capitalone.identity.identitybuilder.model.parsing

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus

data class PolicyMetadata @JvmOverloads constructor(
    val status: EntityActivationStatus,
    var compileVersion: Int = 1,
    var type: String = "ORCHESTRATION_POLICY",
)
