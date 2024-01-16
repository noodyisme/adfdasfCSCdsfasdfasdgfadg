package com.capitalone.identity.identitybuilder.configmanagement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagementModelTest {

    private static final String USE_CASE_A = "LOB.DIV.CHANNEL.APP.USECASE_A";
    private static final String USE_CASE_B = "useCase_B_valid";
    private static final String APP_CASE = "LOB.DIV.CHANNEL.APP";
    private static final String KEY_A = "key_a";
    private static final String KEY_B = "key_b";
    private static final String KEY_C = "key_c";
    private static final String KEY_Z = "key_z";
    private static final String EMPTY_STRING = "";
    private static ConfigManagementModel configModel;

    @BeforeAll
    public static void getTestPolicyConfiguration() {
        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put(KEY_A, "x");
        defaults.put(KEY_B, true);
        defaults.put(KEY_C, true);

        Map<String, Serializable> useCaseA = new HashMap<>();
        useCaseA.put(KEY_B, false);
        useCaseA.put(KEY_Z, true);

        Map<String, Serializable> useCaseB = new HashMap<>();
        useCaseB.put(KEY_A, "b");
        useCaseB.put(KEY_B, true);

        Map<String, Serializable> appLevelUseCase = new HashMap<>();
        appLevelUseCase.put(KEY_A, "app");
        appLevelUseCase.put(KEY_B, false);

        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        useCases.put(USE_CASE_A, useCaseA);
        useCases.put(APP_CASE, appLevelUseCase);
        useCases.put(USE_CASE_B, useCaseB);

        configModel = ConfigManagementModel.newInstance(defaults, useCases);
    }

    @Test
    void constructor_ValidArgs() {
        Map<String, Serializable> defaults = new HashMap<>();
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        assertDoesNotThrow(() -> ConfigManagementModel.newInstance(defaults, useCases));

    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void constructor_InvalidArgs() {
        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put("key_a", "a");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        assertThrows(NullPointerException.class, () -> ConfigManagementModel.newInstance(null, useCases));
        assertThrows(NullPointerException.class, () -> ConfigManagementModel.newInstance(defaults, null));

        // null value in defaults
        defaults.put("key_a", null);
        assertThrows(NullPointerException.class, () -> ConfigManagementModel.newInstance(defaults, useCases));
        defaults.put("key_a", "a"); // cleanup

        // null useCase
        useCases.put("useCase_n", null);
        assertThrows(NullPointerException.class, () -> ConfigManagementModel.newInstance(defaults, useCases));

        // null value in useCase
        HashMap<String, Serializable> useCaseN = new HashMap<>();
        useCaseN.put("key_a", null);
        useCases.put("useCase_n", useCaseN);
        assertThrows(NullPointerException.class, () -> ConfigManagementModel.newInstance(defaults, useCases));

    }

    @Test
    void constructor_InvalidArgsDefaultOverridesFeature() {
        Map<String, Serializable> features = new HashMap<String, Serializable>() {{
            put("featureX", "valueX");
        }};
        Map<String, Serializable> defaults = new HashMap<String, Serializable>() {{
            put("featureX", "valueY");
            put("propertyA", "valueA");
        }};
        Map<String, Map<String, Serializable>> usecases = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> ConfigManagementModel.newInstance(defaults, usecases, features));

    }

    @Test
    void constructor_Immutable_Configurations() {
        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put("string", "abc");
        defaults.put("boolean", true);
        defaults.put("integer", 1);
        defaults.put("number", 1.1);
        ArrayList<Serializable> array = new ArrayList<>();
        array.add("1");
        array.add("2");
        array.add("3");
        defaults.put("array", array);

        HashMap<String, Map<String, Serializable>> useCases = new HashMap<>();
        Map<String, Serializable> useCaseA = new HashMap<>();
        useCaseA.put("string", "xyz");
        useCases.put("useCase_A", useCaseA);

        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, useCases);

        /*
         * Modify default objects.
         */
        defaults.put("string", "def");
        assertEquals("abc", config.getValueOrThrow("string", null));

        defaults.put("boolean", false);
        assertEquals(true, config.getValueOrThrow("boolean", null));

        defaults.put("integer", 2);
        assertEquals(1, config.getValueOrThrow("integer", null));

        defaults.put("number", 2.2);
        assertEquals(1.1, config.getValueOrThrow("number", null));

        array.add("4");
        defaults.put("array", new ArrayList<>());
        @SuppressWarnings("rawtypes") List resultList = (List) config.getValueOrThrow("array", null);
        assertFalse(resultList.isEmpty());
        assertFalse(resultList.contains("4"));

        /*
         * Modify useCase objects.
         */
        useCaseA.put("string", "123");
        assertEquals("xyz", config.getValueOrThrow("string", "useCase_A"));

        useCases.put("useCase_A", new HashMap<>());
        assertEquals("xyz", config.getValueOrThrow("string", "useCase_A"));


    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "default"})
    void configModificationThrows(String useCaseArg) {
        Map<String, Serializable> defaults = Collections.singletonMap("property-A", "A");
        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        useCases.put("abc", Collections.emptyMap());

        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, useCases);

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Map<String, Serializable> resultUsecase = config.getConfiguration(
                useCaseArg, MatchingStrategies.MATCH_ALL_NON_NULL
        ).get();

        assertThrows(UnsupportedOperationException.class, () -> resultUsecase.put("property-A", "ABC"));
        assertThrows(UnsupportedOperationException.class, () -> resultUsecase.put("property-B", "ABC"));
        assertThrows(UnsupportedOperationException.class, () -> resultUsecase.remove("property-A"));
        assertThrows(UnsupportedOperationException.class, () -> resultUsecase.remove("property-A"));

    }


    @Test
    void hasKey() {
        // keys present in default values
        assertTrue(configModel.isValidKey(KEY_A));
        assertTrue(configModel.isValidKey(KEY_B));
        // keys not present in default values
        assertFalse(configModel.isValidKey("key_y"));
        assertFalse(configModel.isValidKey(KEY_Z));
    }


    @ParameterizedTest
    @ValueSource(strings = {APP_CASE, "LOB.DIV.CHANNEL.APP.USECASE_A", "useCase_B_valid"})
    void isUseCaseDefinedTrue_test(String useCase) {
        assertTrue(configModel.isUseCaseDefined(useCase));
    }

    @ParameterizedTest
    @ValueSource(strings = {APP_CASE, "LOB.DIV.CHANNEL.APP.USECASE_A", "LOB.DIV.CHANNEL.APP.MISSING_CASE"})
    void isAppLevelUseCaseDefinedTrue_test(String useCase) {
        assertTrue(configModel.isAppLevelUseCaseDefined(useCase));
    }


    @ParameterizedTest
    @ValueSource(strings = {"LOB.CHANNEL.APP", "LOB.DIV.CHANNEL.NON_EXISTENT_APP.USECASE_A",
            "useCase_invalid_A", EMPTY_STRING, "missing-case", "*&^invalid_characters!"})
    void isUseCaseDefinedFalse_test(String useCase) {
        assertFalse(configModel.isUseCaseDefined(useCase));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOB.CHANNEL.APP", "LOB.DIV.CHANNEL.NON_EXISTENT_APP.USECASE_A",
            "useCase_valid_A", EMPTY_STRING, "missing-case", "*&^invalid_characters!"})
    void isAppLevelUseCaseDefinedFalse_test(String useCase) {
        assertFalse(configModel.isAppLevelUseCaseDefined(useCase));
    }


    @Test
    void getValueOrThrow_return_override() {
        assertEquals(false, configModel.getValueOrThrow(KEY_B, USE_CASE_A));
    }

    @Test
    void getValueOrThrow_return_UseCase() {
        assertEquals(false, configModel.getValueOrThrow(KEY_B, USE_CASE_A));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOB.DIV.CHANNEL.APP.MISSING_USECASE", APP_CASE})
    void getValueOrThrow_return_appLevel(String useCase) {
        assertEquals(false, configModel.getValueOrThrow(KEY_B, useCase));
    }

    //Key C is only valid for defaults, so they should all revert to defaults here.
    @ParameterizedTest
    @ValueSource(strings = {APP_CASE + ".MISSING_CASE", "useCase_A_valid", APP_CASE,
            "LOB.DIV.CHANNEL.APP.USECASE_B_VALID", EMPTY_STRING})
    void getValueOrThrow_invalidKey_returnDefault(String useCase) {
        assertEquals(true, configModel.getValueOrThrow(KEY_C, useCase));
    }

    @ParameterizedTest
    @ValueSource(strings = {"useCase_A_valid", "missing-case",
            "LOB.DIV.CHANNEL.nonexistant_application.UseCase", "LOB.DIV.CHANNEL.NOTHING_HERE",
            "nonexistant_useCase", EMPTY_STRING})
    void getValueOrThrow_return_default_forMissingUsecase(String useCase) {
        assertEquals("x", configModel.getValueOrThrow(KEY_A, useCase));
    }

    @ParameterizedTest
    @ValueSource(strings = {"useCase_A_Valid", EMPTY_STRING, "useCase_x",
            APP_CASE, "LOB.DIV.CHANNEL.APP.USECASE_B_VALID"})
    void getValueOrThrow_throws_when_key_missing(String useCase) {
        assertThrows(IllegalArgumentException.class, () -> configModel.getValueOrThrow(KEY_Z, useCase));

    }

}
