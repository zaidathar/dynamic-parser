package com.zaidathar.dynamic.parser.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class MappingConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(MappingConfigLoader.class);
    private final Map<String, MappingRule> mappings = new HashMap<>();
    private String defaultConfigFile = "mappings.yaml";

    public MappingConfigLoader() {
    }

    public MappingConfigLoader(String defaultConfigFile) {
        this.defaultConfigFile = defaultConfigFile;
    }


    @PostConstruct
    public void init() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(defaultConfigFile)) {
            if (input == null) {
                log.error("Configuration file '{}' not found on classpath. No mappings loaded.", defaultConfigFile);
                return;
            }
            loadConfigFromStream(input);
            log.info("Successfully loaded mappings from '{}'. Total rules: {}", defaultConfigFile, mappings.size());
        } catch (IOException e) {
            log.error("Error reading configuration file '{}': {}", defaultConfigFile, e.getMessage(), e);

        } catch (Exception e) {
            log.error("Error parsing configuration file '{}': {}", defaultConfigFile, e.getMessage(), e);
        }
    }

    public void loadConfigFromStream(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            Map<String, MappingRule> loaded = mapper.readValue(inputStream,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, MappingRule.class));
            this.mappings.clear();
            this.mappings.putAll(loaded);
        }
        catch (IOException e){
            log.error("Exception caught while mapping config : {}",e.getMessage());
            throw e;
        }
    }

    public MappingRule getRule(String fromTo) {
        return mappings.get(fromTo);
    }

    public Map<String, MappingRule> getAllRules() {
        return Collections.unmodifiableMap(mappings);
    }
}

