package com.zaidathar.dynamic.parser.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class MappingConfigLoader {
    private final Map<String, MappingRule> mappings = new HashMap<>();

    @PostConstruct
    public void init() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        InputStream input = getClass().getClassLoader().getResourceAsStream("mappings.yaml");
        Map<String, MappingRule> loaded = mapper.readValue(input,
                mapper.getTypeFactory().constructMapType(Map.class, String.class, MappingRule.class));
        mappings.putAll(loaded);
    }

    public MappingRule getRule(String fromTo) {
        return mappings.get(fromTo);
    }
}

