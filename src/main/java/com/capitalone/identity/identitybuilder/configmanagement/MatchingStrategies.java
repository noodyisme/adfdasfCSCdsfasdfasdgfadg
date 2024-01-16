package com.capitalone.identity.identitybuilder.configmanagement;

import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchingStrategies {

    private MatchingStrategies() {
    }

    /**
     * Matches only if the exact use-case is defined in the {@link ConfigManagementModel}
     */
    public static final ConfigMatchingStrategy MATCH_EXACT_ONLY = (useCase, useCaseMap, defaults, features) -> useCaseMap.get(useCase);

    /**
     * Matches default config management properties in the {@link ConfigManagementModel}. Guaranteed to match.
     */
    public static final ConfigMatchingStrategy MATCH_DEFAULT_ONLY = (useCase, useCaseMap, defaults, features) -> defaults;

    /**
     * Matches features config management properties in the {@link ConfigManagementModel}
     */
    public static final ConfigMatchingStrategy MATCH_FEATURES_ONLY = ((useCase, useCaseMap, defaults, features) -> features);

    /**
     * Matches only when the use-case argument is in a format of a 'business-event' e.g. 'ABC.DEF.GHI.JKL'
     * and a configuration is defined for the 'app-level' sub-string of that argument, e.g. 'ABC.DEF.GHI'
     * in the {@link ConfigManagementModel}
     */
    public static final ConfigMatchingStrategy MATCH_APP_LEVEL_ONLY = new ConfigMatchingStrategy() {
        private final Pattern useCaseStrictMatching = Pattern.compile(
                "^(?<appLevelToken>(([-_A-Za-z0-9]+[.]){3}[-_A-Za-z0-9]+)?)([.][-_A-Za-z0-9]*+)?$");
        private static final String APP_LEVEL_TOKEN = "appLevelToken";

        private String parseAppLevelUseCase(@Nullable String useCase) {
            if (useCase == null) return null;
            else {
                Matcher strictUseCaseMatcher = useCaseStrictMatching.matcher(useCase);
                if (strictUseCaseMatcher.find(0)) {
                    return strictUseCaseMatcher.group(APP_LEVEL_TOKEN);
                } else {
                    return null;
                }
            }
        }

        @Override
        public Map<String, Serializable> getConfig(String useCase, Map<String, Map<String, Serializable>> useCaseMap,
                                                   Map<String, Serializable> defaults, Map<String, Serializable> features) {
            String useCase1 = parseAppLevelUseCase(useCase);
            return MATCH_EXACT_ONLY.getConfig(useCase1, useCaseMap, defaults, features);
        }

    };

    /**
     * Match is determined by first to match in the priority order below. Result is guaranteed to match a config.
     * <ol>
     *     <li>Exact match of use-case (see {@link #MATCH_EXACT_ONLY}</li>
     *     <li>App-level match if argument is business-event formatted (see {@link #MATCH_APP_LEVEL_ONLY}</li>
     *     <li>Exact match of usecase (see {@link #MATCH_DEFAULT_ONLY}</li>
     *     <li>Exact match of features (see {@link #MATCH_FEATURES_ONLY}</li>
     * </ol>
     */
    public static final ConfigMatchingStrategy MATCH_ALL_NON_NULL = (useCase, useCaseMap, defaults, features) ->
            Optional.ofNullable(MATCH_EXACT_ONLY.getConfig(useCase, useCaseMap, defaults, features)).orElseGet(() ->
                    Optional.ofNullable(MATCH_APP_LEVEL_ONLY.getConfig(useCase, useCaseMap, defaults, features))
                            .orElse(MATCH_DEFAULT_ONLY.getConfig(useCase, useCaseMap, defaults, features)));

}
