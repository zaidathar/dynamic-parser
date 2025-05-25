package com.zaidathar.dynamic.parser.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MappingRule {
    private Map<String, String> fields;

    public MappingRule() {
        this.fields = new HashMap<>();
    }

    public MappingRule(Map<String, String> fields) {
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingRule that = (MappingRule) o;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }

    @Override
    public String toString() {
        return "MappingRule{" +
                "fields=" + fields +
                '}';
    }
}