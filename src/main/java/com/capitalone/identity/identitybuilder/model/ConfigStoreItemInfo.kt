package com.capitalone.identity.identitybuilder.model

import com.capitalone.identity.identitybuilder.util.StringUtils

/**
 * Represents an actual object stored in S3 (or other stored location).
 */
data class ConfigStoreItemInfo(val name: String, val tag: String) {
    init {
        StringUtils.requireNotNullOrBlank(name)
    }
}
