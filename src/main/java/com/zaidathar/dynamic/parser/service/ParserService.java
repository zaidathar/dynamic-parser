package com.zaidathar.dynamic.parser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaidathar.dynamic.parser.config.MappingConfigLoader;
import com.zaidathar.dynamic.parser.engine.MappingEngine;
import org.springframework.stereotype.Service;

@Service
public class ParserService {
    private final MappingConfigLoader loader;
    private final MappingEngine engine;
    private final ObjectMapper mapper = new ObjectMapper();

    public ParserService(MappingConfigLoader loader, MappingEngine engine) {
        this.loader = loader;
        this.engine = engine;
    }

    public Object convert(Object source, String fromTo, Class<?> targetType) throws Exception {
        return engine.map(source, targetType, loader.getRule(fromTo));
    }
}