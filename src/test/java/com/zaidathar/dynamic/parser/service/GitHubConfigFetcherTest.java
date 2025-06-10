package com.zaidathar.dynamic.parser.service;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubConfigFetcher Tests")
public class GitHubConfigFetcherTest {
    private MockRestServiceServer mockServer;

    @InjectMocks
    private GitHubConfigFetcher gitHubConfigFetcher;

    private static final String REPO_OWNER = "testUser";
    private static final String REPO_NAME = "testRepo";
    private static final String FILE_PATH = "config/test-mappings.yml";
    private static final String BRANCH = "main";
    private static final String GITHUB_RAW_URL = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
            REPO_OWNER, REPO_NAME, BRANCH, FILE_PATH);

    private static final String VALID_YAML_CONTENT = """
        UserData->UserProfile:
          fields:
            fullName: "transform:fullNameWithPrefix"
            userAge: "age"
    """;

    private static final String UPDATED_YAML_CONTENT = """
        UserData->UserProfile:
          fields:
            fullName: "transform:fullNameWithPrefixV2"
            userAge: "age"
    """;

    private static final String HTML_ERROR_CONTENT = "<!DOCTYPE html><html lang=\"en\"><body><h1>404 Not Found</h1></body></html>";

    @BeforeEach
    void setUp() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                .setResponseTimeout(Timeout.ofSeconds(5))
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(gitHubConfigFetcher, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(gitHubConfigFetcher, "repoOwner", REPO_OWNER);
        ReflectionTestUtils.setField(gitHubConfigFetcher, "repoName", REPO_NAME);
        ReflectionTestUtils.setField(gitHubConfigFetcher, "filePath", FILE_PATH);
        ReflectionTestUtils.setField(gitHubConfigFetcher, "branch", BRANCH);
        ReflectionTestUtils.setField(gitHubConfigFetcher, "accessToken", null);
        ReflectionTestUtils.setField(gitHubConfigFetcher, "lastKnownETag", null);
    }

    @Test
    @DisplayName("Should successfully fetch valid YAML content")
    void testFetchConfigContent_Success() {
        mockServer.expect(
                requestTo(GITHUB_RAW_URL)
        )
        .andRespond(
                withSuccess(VALID_YAML_CONTENT, MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.ETAG, "W/\"etag123\"")
        );

        Optional<InputStream> result = gitHubConfigFetcher.fetchConfigContent();

        assertThat(result).isPresent();
        try (InputStream is = result.get()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).isEqualTo(VALID_YAML_CONTENT);
        } catch (Exception e) {
            Assertions.fail("Failed to read InputStream", e);
        }
        mockServer.verify();
    }

    @Test
    @DisplayName("Should return empty optional for 401 Unauthorized HTML page")
    void testFetchConfigContent_UnauthorizedHtml() {
        mockServer.expect(requestTo(GITHUB_RAW_URL))
                .andRespond(withUnauthorizedRequest().contentType(MediaType.TEXT_HTML).body(HTML_ERROR_CONTENT));

        Optional<InputStream> result = gitHubConfigFetcher.fetchConfigContent();

        assertThat(result).isEmpty();
        mockServer.verify();
    }
}
