package com.capitalone.identity.identitybuilder.model;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.configmanagement.ConfigurationManagementValidationError;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityTest {

    private final PolicyDefinition policyDefinition = new PolicyDefinition("abc", "abc", "abc", 1, 0, 0);
    private final EntityInfo info = new EntityInfo.Policy(policyDefinition, Collections.emptySet());
    private final Entity entity = new Entity.Simple(info, Collections.emptySet());

    private ConfigStoreItem expectRule;
    private ConfigStoreItem expectProcess;
    private ConfigStoreItem expectConfigDefault;
    private ConfigStoreItem expectConfigSchema;
    private ConfigStoreItem expectConfigUsecase;
    private ConfigStoreItem expectConfigFeature;
    private Set<ConfigStoreItem> expectConfigSet;

    @BeforeEach
    void setup() {
        expectRule = null;
        expectProcess = null;
        expectConfigDefault = null;
        expectConfigSchema = null;
        expectConfigUsecase = null;
        expectConfigFeature = null;
        expectConfigSet = new HashSet<>();
    }
    @Test
    void getInfo() {
        assertEquals(info, entity.getInfo());
    }

    @Test
    void getItems() {
        assertTrue(entity.getItems().isEmpty());
    }

    @Test
    void getId() {
        assertEquals(info.getId(), entity.getId());
    }

    @Test
    void getVersion() {
        assertEquals(info.getVersion(), entity.getVersion());
    }

    @Test
    void isActive() {
        PolicyDefinition policySpec = new PolicyDefinition("a/b/c", "2.0");
        EntityInfo.Policy policyInfo = new EntityInfo.Policy(policySpec, Collections.emptySet());
        ConfigStoreItem policyMetadata = new ConfigStoreItem(
                new ConfigStoreItemInfo("a/b/c/1.0/policy-metadata.json", "a"),
                "{\"Status\": \"ACTIVE\"}");
        Entity.Policy policy = new Entity.Policy(policyInfo, Collections.singleton(policyMetadata));
        assertEquals(EntityActivationStatus.ACTIVE, policy.getEntityActivationStatus());
    }

    @Test
    void isDisabled() {
        PolicyDefinition policySpec = new PolicyDefinition("a/b/c", "1.0");
        EntityInfo.Policy policyInfo = new EntityInfo.Policy(policySpec, Collections.emptySet());
        ConfigStoreItem policyMetadata = new ConfigStoreItem(
                new ConfigStoreItemInfo("a/b/c/1.0/policy-metadata.json", "a"),
                "{\"Status\": \"DISABLED\"}");
        Entity.Policy policy = new Entity.Policy(policyInfo, Collections.singleton(policyMetadata));
        assertEquals(EntityActivationStatus.DISABLED, policy.getEntityActivationStatus());
    }

    @Test
    void isParseError() {
        PolicyDefinition policySpec = new PolicyDefinition("a/b/c", "1.0");
        EntityInfo.Policy policyInfo = new EntityInfo.Policy(policySpec, Collections.emptySet());
        ConfigStoreItem policyMetadata = new ConfigStoreItem(
                new ConfigStoreItemInfo("a/b/c/1.0/policy-metadata.json", "a"),
                "");
        assertThrows(IllegalArgumentException.class,
                () -> new Entity.Policy(policyInfo, Collections.singleton(policyMetadata)));
    }

    @Test
    void getEntity_policy() {
        EntityInfo.Policy mock = Mockito.mock(EntityInfo.Policy.class);
        PolicyMetadata policyMetadata = new PolicyMetadata(EntityActivationStatus.AVAILABLE, 1);
        when(mock.getLocationPrefix()).thenReturn("a/b/c/1.0");
        ConfigStoreItem expectRule = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/rules/a.dmn", "a"), "");
        ConfigStoreItem expectProcess = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/process/b.xml", "a"), "");
        ConfigStoreItem expectConfigDefault = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/defaults.json", "a"), "{}");
        ConfigStoreItem expectConfigSchema = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/schema.json", "a"), "{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\"type\": \"object\",\"required\": [],\"additionalProperties\": false,\"properties\": {}}");
        ConfigStoreItem expectConfigUsecase = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/usecase.json", "a"), "{}");
        Set<ConfigStoreItem> items = new HashSet<>(Arrays.asList(
                expectRule,
                expectProcess,
                expectConfigDefault,
                expectConfigSchema,
                expectConfigUsecase
        ));


        Entity.Policy policy = new Entity.Policy(mock, items, policyMetadata);
        assertNotNull(policy);
        assertEquals(Collections.singleton(expectRule), policy.getRuleItems());
        assertEquals(Collections.singleton(expectProcess), policy.getProcessItems());

        ConfigManagementModel expectedModel = ConfigManagementModel.newInstance(
                new HashMap<>(),
                new HashMap<String, Map<String, Serializable>>() {{
                    put("usecase", new HashMap<>());
                }});

        ConfigManagementModel actualModel = policy.getConfigManagementModel().orElse(null);

        assertEquals(expectedModel, actualModel);

    }

    @Test
    void getEntity_policy_delayConfigError() {
        EntityInfo.Policy mock = Mockito.mock(EntityInfo.Policy.class);
        String location = "a/b/c/1.0";
        when(mock.getLocationPrefix()).thenReturn(location);
        Set<ConfigStoreItem> items = new HashSet<>(Arrays.asList(
                new ConfigStoreItem("a/b/c/1.0/policy-metadata.json", "{\"Status\": \"AVAILABLE\"}"),
                new ConfigStoreItem("a/b/c/1.0/config/defaults.json", "{}"),
                new ConfigStoreItem("a/b/c/1.0/config/schema.json", "{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\"type\": \"object\",\"required\": [],\"additionalProperties\": false,\"properties\": {}}"),
                new ConfigStoreItem("a/b/c/1.0/config/usecase.json", "<INVALID INPUT>")
        ));

        Entity.Policy policy = assertDoesNotThrow(() -> new Entity.Policy(mock, items));
        RuntimeException exception = assertThrows(ConfigurationManagementValidationError.class,
                policy::getConfigManagementModel);
        assertTrue(exception.getMessage().contains(location));

    }

    @Test
    void getEntity_pip() {
        ConfigStoreItem expectProcess = new ConfigStoreItem(new ConfigStoreItemInfo("a/routes/c/b.xml", "a"), "");

        EntityInfo.Pip mock = Mockito.mock(EntityInfo.Pip.class);
        Entity.Pip pip = new Entity.Pip(mock, Collections.singleton(expectProcess));
        assertNotNull(pip);
        assertEquals(expectProcess, pip.getRouteFile());
    }

    @Test
    void getEntity_access() {
        String content = "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"schemaVersion\": \"0\", \"policyNamespace\": \"a/b/c\", \"policyMajorVersion\": 1, \"clients\": []}";

        String name = "a/b/c/1/access-control/0/policy-access.json";
        ConfigStoreItemInfo itemInfo = new ConfigStoreItemInfo(name, "a");
        Set<ConfigStoreItem> singleton = Collections.singleton(new ConfigStoreItem(itemInfo, content));
        EntityInfo.Access accessInfo = new EntityInfo.Access(name, name, 0, "c", "a/b/c",
                1, Collections.singleton(itemInfo));

        Entity.Access access = new Entity.Access(accessInfo, singleton);
        assertNotNull(access.getPolicyAccess());
    }

    @Test
    void getEntity_access_throws_parseError() {
        String invalidJsonContent = "invalid json content";
        Set<ConfigStoreItem> singleton = Collections.singleton(new ConfigStoreItem(
                new ConfigStoreItemInfo("a/b/c/1/access-control/0/policy-access.json", "a"),
                invalidJsonContent
        ));

        EntityInfo.Access mock = Mockito.mock(EntityInfo.Access.class);
        ConfigStoreBusinessException e = assertThrows(ConfigStoreBusinessException.class, () -> new Entity.Access(mock, singleton));
        assertTrue(e.getCause() instanceof JsonProcessingException);
    }

    @Test
    void getEntity_access_throws_namespaceErrors() {
        String contentWithWrongPolicyMajorVersion = "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"schemaVersion\": \"0\", \"policyNamespace\": \"a/b/c\", \"policyMajorVersion\": 2, \"clients\": []}";
        ConfigStoreItemInfo itemInfo = new ConfigStoreItemInfo("a/b/c/1/access-control/0/policy-access.json", "a");
        Set<ConfigStoreItem> singleton = Collections.singleton(new ConfigStoreItem(itemInfo, contentWithWrongPolicyMajorVersion));

        EntityInfo.Access accessEntity = new EntityInfo.Access("a/b/c/1/access-control",
                "a/b/c/1/access-control/0/policy-access.json", 0, "c", "a/b/c", 1, Collections.singleton(itemInfo));
        ConfigStoreBusinessException e = assertThrows(ConfigStoreBusinessException.class, () -> new Entity.Access(accessEntity, singleton));
        assertTrue(e.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void getEntity_policy_level_config() {
        EntityInfo.Policy mock = Mockito.mock(EntityInfo.Policy.class);
        PolicyMetadata policyMetadata = new PolicyMetadata(EntityActivationStatus.AVAILABLE, 1);
        populateConfigSet();
        ConfigStoreItem expectRule = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/rules/a.dmn", "a"), "");
        ConfigStoreItem expectProcess = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/process/b.xml", "a"), "");

        Entity.Policy policy = new Entity.Policy(mock, expectConfigSet, policyMetadata);
        assertNotNull(policy);
        assertEquals(Collections.singleton(expectRule), policy.getRuleItems());
        assertEquals(Collections.singleton(expectProcess), policy.getProcessItems());
    }
    @Test
    void getConfigModel_policy_level_config() {
        EntityInfo.Policy mock = Mockito.mock(EntityInfo.Policy.class);
        PolicyMetadata policyMetadata = new PolicyMetadata(EntityActivationStatus.AVAILABLE, 1);
        when(mock.getLocationPrefix()).thenReturn("a/b/c/1.0");
        populateConfigSet();

        HashMap defaultMap = new HashMap();
        defaultMap.put("something","abc");

        HashMap resultMap = new HashMap<>();
        resultMap.put("something2","abc");
        resultMap.put("something","abc");

        HashMap featuresMap = new HashMap<>();
        featuresMap.put("policy-core.mockMode",true);

        ConfigManagementModel expectedModel = ConfigManagementModel.newInstance(
                defaultMap,
                new HashMap<String, Map<String, Serializable>>() {{
                    put("usecase", resultMap);
                }},
                featuresMap);

        Entity.Policy policy = new Entity.Policy(mock, expectConfigSet, policyMetadata);

        ConfigManagementModel actualModel = policy.getConfigManagementModel().orElse(null);

        assertEquals(expectedModel, actualModel);

    }

    private void populateConfigSet() {
        expectRule = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/rules/a.dmn", "a"), "");
        expectProcess = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/process/b.xml", "a"), "");
        expectConfigDefault = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/defaults.json", "a"), "{\"something\":\"abc\"}");
        expectConfigSchema = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/schema.json", "a"), "{\"$id\":\"v2\",\"$defs\":{\"usecase\":{\"$schema\":\"https://json-schema.org/draft/2019-09/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"something\":{\"type\":\"string\"},\"something2\":{\"type\":\"string\"}}},\"defaults\":{\"allOf\":[{\"$ref\":\"usecase\"},{\"required\":[\"something\"]}]},\"features\":{\"$schema\":\"https://json-schema.org/draft/2019-09/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"policy-core.mockMode\":{\"type\":\"boolean\"}}},\"features-required\":{\"allOf\":[{\"$ref\":\"features\"},{\"required\":[\"policy-core.mockMode\"]}]}}}");
        expectConfigUsecase = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/usecase.json", "a"), "{\"something2\":\"abc\"}");
        expectConfigFeature = new ConfigStoreItem(new ConfigStoreItemInfo("a/b/c/1.0/config/features.json", "a"), "{\"policy-core.mockMode\": true}");
        expectConfigSet = new HashSet<>(Arrays.asList(
                expectRule,
                expectProcess,
                expectConfigDefault,
                expectConfigSchema,
                expectConfigUsecase,
                expectConfigFeature
        ));
    }
}
