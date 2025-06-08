package com.zaidathar.dynamic.parser.service;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class GitHubConfigFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitHubConfigFetcher.class);

    @Value("${app.config.github.repo-owner}")
    private String repoOwner;

    @Value("${app.config.github.repo-name}")
    private String repoName;

    @Value("${app.config.github.file-path}")
    private String filePath;

    @Value("${app.config.github.branch}")
    private String branch;

    @Value("${app.config.github.access-token:#{null}}")
    private String accessToken;

    private volatile String lastKnownETag = null;

    private final RestTemplate restTemplate;

    public GitHubConfigFetcher() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(requestFactory);
    }


    public Optional<InputStream> fetchConfigContent() {
        String url = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
                repoOwner, repoName, branch, filePath);
        log.debug("Attempting to fetch config from GitHub URL: {}", url);
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null && !accessToken.isEmpty()) {
            headers.set("Authorization", "token " + accessToken);
            log.debug("Using GitHub Personal Access Token for authentication.");
        }
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        if (lastKnownETag != null) {
            headers.setIfNoneMatch(lastKnownETag);
            log.debug("Adding If-None-Match header with ETag: {}", lastKnownETag);
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, headers);

            String newETag = response.getHeaders().getFirst(HttpHeaders.ETAG);
            if (newETag != null) {
                lastKnownETag = newETag;
                log.debug("Updated lastKnownETag to: {}", lastKnownETag);
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully fetched new config content from GitHub.");
                return Optional.of(new ByteArrayInputStream(response.getBody().getBytes(StandardCharsets.UTF_8)));
            } else if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                log.info("Config file on GitHub has not changed (HTTP 304 Not Modified).");
                return Optional.empty();
            } else {
                log.warn("Failed to fetch config from GitHub. Unexpected status code: {}", response.getStatusCode());
                return Optional.empty();
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Config file not found at {}. Double-check repo owner, name, branch, and file path.", url);
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("Authentication/Authorization error when fetching from {}. Check access token and repo permissions. Status: {}", url, e.getStatusCode());
            } else {
                log.error("HTTP client error fetching config from {}: {}", url, e.getMessage(), e);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching config from {}: {}", url, e.getMessage(), e);
            return Optional.empty();
        }
    }
}