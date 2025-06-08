package com.zaidathar.dynamic.parser.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.zaidathar.dynamic.parser.service.GitHubConfigFetcher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MappingConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(MappingConfigLoader.class);
    private final Map<String, MappingRule> mappings = new HashMap<>();
    private final GitHubConfigFetcher gitHubConfigFetcher;
    private final PathMatchingResourcePatternResolver resourcePatternResolver;


    public MappingConfigLoader(GitHubConfigFetcher gitHubConfigFetcher) {

        this.gitHubConfigFetcher = gitHubConfigFetcher;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
    }


    @PostConstruct
    public void init() {
        log.info("Attempting to load configuration...");

        boolean githubLoadSuccess = false;
        try {
            assert gitHubConfigFetcher != null;
            Optional<InputStream> githubStreamOptional = gitHubConfigFetcher.fetchConfigContent();
            if (githubStreamOptional.isPresent()) {
                loadConfigFromStream(githubStreamOptional.get());
                log.info("Successfully loaded configuration from GitHub.");
                githubLoadSuccess = true;
            } else {
                log.warn("GitHub configuration fetch returned no content or encountered an issue. Falling back to classpath.");
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching configuration from GitHub. Falling back to classpath. Error: {}", e.getMessage(), e);
        }


        if (!githubLoadSuccess) {
            log.info("Attempting to load configuration from classpath (fallback).");
            try {

                Resource resource = resourcePatternResolver.getResource("classpath:mappings.yaml");

                if (!resource.exists()) {
                    log.error("Classpath configuration file '{}' not found. No default mappings loaded.", resource.getFilename());
                    return;
                }

                try (InputStream input = resource.getInputStream()) {
                    loadConfigFromStream(input);
                    log.info("Successfully loaded mappings from classpath '{}'. Total rules: {}", resource.getFilename(), mappings.size());
                }
            } catch (IOException e) {
                log.error("Error reading classpath configuration file: {}", e.getMessage(), e);
            } catch (Exception e) {
                log.error("Error parsing classpath configuration file: {}", e.getMessage(), e);
            }
        }

        if (!githubLoadSuccess && mappings.isEmpty()) {
            log.error("No configuration could be loaded from GitHub or classpath. Application might not function as expected.");
        }
    }

    public void loadConfigFromStream(InputStream inputStream) throws IOException {
        log.info("Loading config ....");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            Map<String, MappingRule> loaded = mapper.readValue(
                    inputStream,
                    mapper
                        .getTypeFactory()
                        .constructMapType(
                                Map.class,
                                String.class,
                                MappingRule.class
                        )
            );
            log.info("Clearing mapping ....");
            this.mappings.clear();
            log.info("After clearing mappings size : {}",mappings.size());
            this.mappings.putAll(loaded);
            log.info("After inserting mappings size : {}",mappings.size());

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

