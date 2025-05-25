package com.zaidathar.dynamic.parser.config;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@DisplayName("MappingConfigLoader Unit Tests")
public class MappingConfigLoaderTest {

    private static final Logger log = LoggerFactory.getLogger(MappingConfigLoaderTest.class);

    private MappingConfigLoader mappingConfigLoader;

    private static final String VALID_TEST_YAML = """
        UserData->UserProfile:
          fields:
            fullName: "transform:fullNameWithPrefix"
            userAge: "age"
            ageInMonths: "transform:ageInMonths"
            profilePic: "profile"
            contact: "contact"
        AnotherData->AnotherProfile:
          fields:
            id: "userId"
            name: "userName"
    """;

    private static final String MALFORMED_TEST_YAML = """
        InvalidYaml:
          fields:
            key: "value"
        - this is not yaml
    """;

    @BeforeEach
    void init() throws Exception {
        mappingConfigLoader = new MappingConfigLoader();
    }


    @Test
    @DisplayName("Should load valid configuration from stream correctly")
    void testLoadConfigFromStream_ValidYaml() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(VALID_TEST_YAML.getBytes(StandardCharsets.UTF_8));

        mappingConfigLoader.loadConfigFromStream(inputStream);

        MappingRule userDataProfileRule = mappingConfigLoader.getRule("UserData->UserProfile");

        Map<String, String> expectedFields = new HashMap<>(Map.of(
                "fullName", "transform:fullNameWithPrefix",
                "userAge", "age",
                "ageInMonths", "transform:ageInMonths",
                "profilePic", "profile",
                "contact", "contact"
        ));

        Assertions.assertAll(
                () -> Assertions.assertNotNull(userDataProfileRule, "Mapping rule 'UserData->UserProfile' should not be null"),
                () -> Assertions.assertNotNull(userDataProfileRule.getFields(), "Mapping rule fields should not be null"),
                () -> Assertions.assertEquals(expectedFields.size(), userDataProfileRule.getFields().size(),
                        "Number of fields in UserData->UserProfile rule should match expected"),
                () -> Assertions.assertEquals(expectedFields, userDataProfileRule.getFields(),
                        "Fields for 'UserData->UserProfile' should match expected values")
        );

        MappingRule anotherRule = mappingConfigLoader.getRule("AnotherData->AnotherProfile");
        Map<String, String> expectedAnotherFields = new HashMap<>(Map.of(
                "id", "userId",
                "name", "userName"
        ));
        Assertions.assertAll(
                () -> Assertions.assertNotNull(anotherRule, "Mapping rule 'AnotherData->AnotherProfile' should not be null"),
                () -> Assertions.assertEquals(expectedAnotherFields, anotherRule.getFields(),
                        "Fields for 'AnotherData->AnotherProfile' should match expected values")
        );
    }

    @Test
    @DisplayName("Should return null for a non-existent rule")
    void testGetRule_NonExistent() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(VALID_TEST_YAML.getBytes(StandardCharsets.UTF_8));
        mappingConfigLoader.loadConfigFromStream(inputStream);

        MappingRule nonExistentRule = mappingConfigLoader.getRule("NonExistentRule");
        Assertions.assertNull(nonExistentRule, "Non-existent rule should return null");
    }

    @Test
    @DisplayName("Should return an empty map if no rules are loaded")
    void testGetAllRules_NoRulesLoaded() {
        Assertions.assertTrue(mappingConfigLoader.getAllRules().isEmpty(), "All rules should be empty initially");
    }

    @Test
    @DisplayName("Should return all loaded rules")
    void testGetAllRules_RulesLoaded() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(VALID_TEST_YAML.getBytes(StandardCharsets.UTF_8));
        mappingConfigLoader.loadConfigFromStream(inputStream);

        Map<String, MappingRule> allRules = mappingConfigLoader.getAllRules();
        Assertions.assertAll(
                () -> Assertions.assertFalse(allRules.isEmpty(), "All rules should not be empty"),
                () -> Assertions.assertEquals(2, allRules.size(), "Should have 2 rules loaded"),
                () -> Assertions.assertTrue(allRules.containsKey("UserData->UserProfile")),
                () -> Assertions.assertTrue(allRules.containsKey("AnotherData->AnotherProfile"))
        );
    }

    @Test
    @DisplayName("Should throw IOException for malformed YAML")
    void testLoadConfigFromStream_MalformedYaml() {
        InputStream inputStream = new ByteArrayInputStream(MALFORMED_TEST_YAML.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(IOException.class, () -> {
            mappingConfigLoader.loadConfigFromStream(inputStream);
        }, "Loading malformed YAML should throw an IOException");
    }

    @Nested
    @DisplayName("Spring Context Integration Test")
    @SpringBootTest(classes = MappingConfigLoader.class, properties = {"logging.level.com.example.servicea=DEBUG"})
    class SpringContextIntegrationTest {

        @Autowired
        private MappingConfigLoader mappingConfigLoaderAutowired;

        @Test
        @DisplayName("Should load default config via @PostConstruct")
        void testPostConstructLoading() {
            MappingRule rule = mappingConfigLoaderAutowired.getRule("UserData->UserProfile");
            Assertions.assertNotNull(rule, "@PostConstruct should have loaded rules from mappings.yaml");
        }
    }

}
