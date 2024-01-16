package com.capitalone.identity.identitybuilder.model

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel
import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModelParser
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem.*
import com.capitalone.identity.identitybuilder.model.abac.PolicyAccess
import com.capitalone.identity.identitybuilder.model.abac.StoredPolicyAccessParser
import com.capitalone.identity.identitybuilder.model.parsing.PolicyManifestJsonFileParser
import com.capitalone.identity.identitybuilder.model.parsing.PolicyManifestParser.ManifestProcessingException
import com.capitalone.identity.identitybuilder.model.parsing.PolicyMetadata
import java.util.*


abstract class Entity : Versionable {

    abstract val info: EntityInfo
    abstract val items: Set<ConfigStoreItem>

    override fun getId(): String = info.id
    override fun getVersion(): String = info.version
    override fun getIdPrefix(): String = info.idPrefix

    data class Policy(
        override val info: EntityInfo.Policy,
        override val items: Set<ConfigStoreItem>,
        val policyMetadata: PolicyMetadata,
    ) : Entity() {
        constructor(info: EntityInfo.Policy, items: Set<ConfigStoreItem>)
                : this(info, items, parsePolicyMetadata(info, items))

        constructor(info: EntityInfo.Policy, items: Set<ConfigStoreItem>, entityActivationStatus: EntityActivationStatus, compileVersion: Int)
                : this(info, items, PolicyMetadata(entityActivationStatus, compileVersion))

        constructor(info: EntityInfo.Policy, items: Set<ConfigStoreItem>, entityActivationStatus: EntityActivationStatus)
                : this(info, items, PolicyMetadata(entityActivationStatus))

        val configManagementModel: Optional<ConfigManagementModel> by lazy {
            getConfigManagementModelForEnv(null)
        }

        fun getConfigManagementModelForEnv(env: String?) :Optional<ConfigManagementModel> {
            val defaults: String? = getItem(items, Type.CONFIG_DEFAULT).map { it.content }.orElse(null)
            val schema: String? = getItem(items, Type.CONFIG_SCHEMA).map { it.content }.orElse(null)
            val useCases: Set<ConfigStoreItem> = getItems(items, Type.CONFIG_USECASE)
            val features: Set<ConfigStoreItem> = getItems(items, Type.CONFIG_FEATURES)
            return Optional.ofNullable(ConfigManagementModelParser.parse(info.locationPrefix, defaults, schema, useCases, features, env))
        }

        val processItems: Set<ConfigStoreItem> get() = getItems(items, Type.PROCESS)
        val ruleItems: Set<ConfigStoreItem> get() = getItems(items, Type.RULES)
        val entityActivationStatus: EntityActivationStatus by lazy { policyMetadata.status }
        val compileVersion: Int by lazy { policyMetadata.compileVersion }

        companion object {
            fun parsePolicyMetadata(info: EntityInfo, items: Set<ConfigStoreItem>): PolicyMetadata {
                return getItem(items, Type.POLICY_STATUS_SPARSE)
                    .flatMap { item: ConfigStoreItem ->
                        try {
                            val manifestParser = PolicyManifestJsonFileParser()
                            return@flatMap manifestParser.parsePolicyMetadata(item.content)
                        } catch (e: ManifestProcessingException) {
                            throw IllegalArgumentException(info.toString(), e)
                        }
                    }
                    .orElseThrow {
                        IllegalArgumentException("Unrecognized or unknown Status in policy-info.json file. $info")
                    }
            }

        }
    }

    data class Pip(override val info: EntityInfo.Pip, override val items: Set<ConfigStoreItem>) : Entity() {
        val routeFile: ConfigStoreItem get() = items.iterator().next()
    }

    data class Access(override val info: EntityInfo.Access, override val items: Set<ConfigStoreItem>) : Entity() {
        var policyAccess: PolicyAccess = try {
            val storedAccess = StoredPolicyAccessParser.getInstance()
                .parsePolicyAccess(items.iterator().next().content)
            PolicyAccess(info, storedAccess)
        } catch (e: ConfigStoreBusinessException) {
            throw e
        } catch (e: RuntimeException) {
            throw ConfigStoreBusinessException("Error creating policy access entity", e)
        }

    }

    data class Simple(override val info: EntityInfo, override val items: Set<ConfigStoreItem>) : Entity()

}
