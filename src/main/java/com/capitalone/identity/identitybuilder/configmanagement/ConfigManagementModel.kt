package com.capitalone.identity.identitybuilder.configmanagement

import com.google.common.collect.ImmutableMap
import org.apache.commons.lang.SerializationUtils
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import java.io.Serializable
import java.util.*
import java.util.stream.Collectors

/**
 * Represents a Configuration Management model.
 */
data class ConfigManagementModel private constructor(
    val defaults: Map<String, Serializable>,
    val useCaseMap: Map<String, Map<String, Serializable>>,
    val featuresMap: Map<String, Serializable>?
) {

    companion object {

        @JvmStatic
        fun newInstance(defaults: Map<String, Serializable>, useCases: Map<String, Map<String, Serializable>>)
                : ConfigManagementModel {
            return newInstance(defaults, useCases, null)
        }
        @JvmStatic
        fun newInstance(defaults: Map<String, Serializable>, useCases: Map<String, Map<String, Serializable>>, nonOverrideValues: Map<String, Serializable>?)
                : ConfigManagementModel {
            var newDefaults: Map<String, Serializable> = if(!nonOverrideValues.isNullOrEmpty()) {
                mergeWithNonOverrideValues(defaults, nonOverrideValues)
            } else {
                defaults
            }
            return ConfigManagementModel(
                deepCloneMap(newDefaults),
                Objects.requireNonNull(useCases).entries.stream().collect(
                    Collectors.toMap({ it.key }, { entry: Map.Entry<String, Map<String, Serializable>> ->
                        var combination = mergeWithDefaultValues(entry.value, newDefaults)
                        deepCloneMap(combination)
                    })
                ),
                nonOverrideValues
            )
        }

        private fun deepCloneMap(source: Map<String, Serializable>): Map<String, Serializable> {
            return Objects.requireNonNull(source).entries.stream()
                .collect(
                    ImmutableMap.toImmutableMap({ it.key }, { entry: Map.Entry<String, Serializable> ->
                        SerializationUtils.clone(
                            entry.value
                        ) as Serializable
                    })
                )
        }

        private fun mergeWithDefaultValues(source: Map<String, Serializable>, defaults: Map<String, Serializable>)
                : Map<String, Serializable> {
            val combinedSource: MutableMap<String, Serializable> = HashMap(defaults)
            combinedSource.putAll(Objects.requireNonNull(source))
            return combinedSource
        }

        private fun mergeWithNonOverrideValues(source: Map<String, Serializable>, nonOverrideValues: Map<String, Serializable>)
            :Map<String, Serializable> {
            val combinedSource: MutableMap<String, Serializable> = HashMap(source)
            nonOverrideValues.forEach { nonOverride ->
                if(!combinedSource.containsKey(nonOverride.key)){
                    combinedSource[nonOverride.key] = nonOverride.value
                } else {
                    throw IllegalArgumentException(
                            String.format(
                                    "Policy Level config cannot be present in usecase or defaults: " +
                                            "%s", nonOverride.key
                            )
                    )
                }
            }
            return combinedSource
        }
    }

    /**
     * Check to see if a config management property is defined for this policy.
     *
     * @param paramKey config management property key
     * @return `true` if the config management property is defined
     */
    fun isValidKey(@NonNull paramKey: String): Boolean {
        return defaults[paramKey] != null
    }

    /**
     * Check to see if a use-case is defined for the configuration.
     *
     * @param useCase to check
     * @return `true` if the use-case is defined in this configuration.
     */
    fun isUseCaseDefined(@Nullable useCase: String?): Boolean {
        return getConfiguration(useCase, MatchingStrategies.MATCH_EXACT_ONLY).isPresent
    }

    /**
     * Check to see if an associated app level use-case is defined for the configuration using a given useCase.
     *
     * @param useCase to check (contains app level use-case within name)
     * @return `true` if the app level use-case is defined in this configuration.
     */
    fun isAppLevelUseCaseDefined(@Nullable useCase: String?): Boolean {
        return getConfiguration(useCase, MatchingStrategies.MATCH_APP_LEVEL_ONLY).isPresent
    }

    /**
     * Get a config management property for a use-case. If the use-case is null, or not defined
     * (see [.isUseCaseDefined]), then a default value is returned for the corresponding key.
     *
     * @param paramKey config management property key supported by this policy
     * @param useCase  return nonOverride of default value corresponding to this use-case, if exists
     * @return always returns non-null value (or exception is thrown)
     * @throws RuntimeException if the paramKey is not valid (check [.isValidKey] to avoid)
     */
    fun getValueOrThrow(@NonNull paramKey: String, @Nullable useCase: String?): Any? {
        return getValue(paramKey, useCase, MatchingStrategies.MATCH_ALL_NON_NULL)
            // assume the cause is 'missing key' because matching strategy should always return a config
            .orElseThrow {
                IllegalArgumentException(
                    String.format(
                        "config management property not found: " +
                                "%s (parameter must be present in the set of default properties)", paramKey
                    )
                )
            }
    }

    /**
     * Get a config management configuration for a use-case, given a matching strategy
     *
     * @param useCase  identifier of the use case
     * @param strategy see [MatchingStrategies]
     * @return config management property map
     */
    @NonNull
    fun getConfiguration(
        @Nullable useCase: String?,
        @NonNull strategy: ConfigMatchingStrategy
    ): Optional<Map<String, Serializable>> {
        return Optional.ofNullable(strategy.getConfig(useCase, useCaseMap, defaults, featuresMap))
    }

    /**
     * Get a value from this config management model based on param key, usecase, and matching strategy
     * [MatchingStrategies]
     *
     * @return empty optional indicates either that paramKey is not associated with a config management property,
     * or the config matching strategy didn't return any results.
     */
    fun getValue(
        @NonNull paramKey: String, @Nullable useCase: String?,
        @NonNull strategy: ConfigMatchingStrategy?
    ): Optional<Any?> {
        return getConfiguration(useCase, Objects.requireNonNull(strategy)!!)
            .map { config: Map<String, Serializable> ->
                config[Objects.requireNonNull(paramKey)]
            }
    }

}
