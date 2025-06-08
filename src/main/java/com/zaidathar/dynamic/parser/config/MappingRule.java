package com.zaidathar.dynamic.parser.config;

import java.util.HashMap;
import java.util.Map;

public class MappingRule {
    private final Map<String, String> fields;

    public MappingRule() {
        this.fields = new HashMap<>();
    }

    public MappingRule(Map<String, String> fields) {
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }

}