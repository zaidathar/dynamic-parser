package com.zaidathar.dynamic.parser.controller;

import com.zaidathar.dynamic.parser.config.MappingConfigLoader;
import com.zaidathar.dynamic.parser.service.GitHubConfigFetcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@DisplayName("WebhookController Tests")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookController webhookController;

    @MockitoBean
    private GitHubConfigFetcher mockGitHubConfigFetcher;

    @MockitoBean
    private MappingConfigLoader mockMappingConfigLoader;

    private static final String WEBHOOK_SECRET = "mySuperSecureSecretForTests";
    private static final String WEBHOOK_ENDPOINT = "/webhook/github/config-reload";

    private static final String SAMPLE_PUSH_PAYLOAD = """
        {
          "ref": "refs/heads/main",
          "before": "a1b2c3d4",
          "after": "e5f6g7h8",
          "repository": {
            "id": 12345,
            "node_id": "MDEwOlJlcG9zaXRvcnkxMjM0NQ==",
            "name": "testrepo",
            "full_name": "testuser/testrepo",
            "private": false
          },
          "pusher": {
            "name": "testuser",
            "email": "testuser@example.com"
          },
          "head_commit": {
            "id": "e5f6g7h8",
            "message": "Update config",
            "timestamp": "2023-01-01T12:00:00Z",
            "author": {
              "name": "testuser",
              "email": "testuser@example.com"
            },
            "committer": {
              "name": "testuser",
              "email": "testuser@example.com"
            },
            "added": [],
            "removed": [],
            "modified": ["src/main/resources/mappings.yaml"]
          },
          "sender": {
            "login": "testuser",
            "id": 123456
          },
          "created": false,
          "deleted": false,
          "forced": false
        }
    """;

    private static final String SAMPLE_OTHER_EVENT_PAYLOAD = """
        {"action": "opened", "issue": {"id": 123, "title": "Test Issue"}}
    """;


    private String calculateSignature(String payload, String secret) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKey);
        byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return "sha256=" + formatter.toString();
    }


    @Test
    @DisplayName("Should successfully reload config on valid push event")
    void testHandleGitHubWebhook_ValidPush() throws Exception {

        when(mockGitHubConfigFetcher.fetchConfigContent())
                .thenReturn(Optional.of(new ByteArrayInputStream("test: config".getBytes())));

        String signature = calculateSignature(SAMPLE_PUSH_PAYLOAD, WEBHOOK_SECRET);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isOk());

        verify(mockGitHubConfigFetcher, times(1)).fetchConfigContent();
        verify(mockMappingConfigLoader, times(1)).loadConfigFromStream(any(InputStream.class));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for invalid signature")
    void testHandleGitHubWebhook_InvalidSignature() throws Exception {
        String invalidSignature = calculateSignature(SAMPLE_PUSH_PAYLOAD, "wrongSecret");

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", invalidSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(mockGitHubConfigFetcher, never()).fetchConfigContent();
        verify(mockMappingConfigLoader, never()).loadConfigFromStream(any(InputStream.class));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error if fetcher fails")
    void testHandleGitHubWebhook_FetcherFails() throws Exception {
        when(mockGitHubConfigFetcher.fetchConfigContent()).thenReturn(Optional.empty());

        String signature = calculateSignature(SAMPLE_PUSH_PAYLOAD, WEBHOOK_SECRET);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isInternalServerError());

        verify(mockGitHubConfigFetcher, times(1)).fetchConfigContent();
        verify(mockMappingConfigLoader, never()).loadConfigFromStream(any(InputStream.class));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error if config loader throws IOException")
    void testHandleGitHubWebhook_LoaderThrowsException() throws Exception {
        when(mockGitHubConfigFetcher.fetchConfigContent())
                .thenReturn(Optional.of(new ByteArrayInputStream("malformed: {".getBytes())));

        doThrow(new IOException("Simulated parsing error")).when(mockMappingConfigLoader)
                .loadConfigFromStream(any(InputStream.class));

        String signature = calculateSignature(SAMPLE_PUSH_PAYLOAD, WEBHOOK_SECRET);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isInternalServerError());

        verify(mockGitHubConfigFetcher, times(1)).fetchConfigContent();
        verify(mockMappingConfigLoader, times(1)).loadConfigFromStream(any(InputStream.class));
    }

    @Test
    @DisplayName("Should return 401 if signature header is missing")
    void testHandleGitHubWebhook_MissingSignatureHeader() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isBadRequest());

        verify(mockGitHubConfigFetcher, never()).fetchConfigContent();
    }

    @Test
    @DisplayName("Should return 401 if signature header is malformed")
    void testHandleGitHubWebhook_MalformedSignatureHeader() throws Exception {
        String signature = "invalidformat_sha256=" + calculateSignature(SAMPLE_PUSH_PAYLOAD, WEBHOOK_SECRET).substring(7);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(mockGitHubConfigFetcher, never()).fetchConfigContent();
    }

    @Test
    @DisplayName("Should allow custom webhook secret via @Value")
    void testHandleGitHubWebhook_CustomSecret() throws Exception {

        String newSecret = "anotherCustomSecret";

        ReflectionTestUtils.setField(webhookController, "webhookSecret", newSecret);

        String signature = calculateSignature(SAMPLE_PUSH_PAYLOAD, newSecret);

        when(mockGitHubConfigFetcher.fetchConfigContent())
                .thenReturn(Optional.of(new ByteArrayInputStream("test: custom".getBytes())));

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PUSH_PAYLOAD))
                .andExpect(status().isOk());

        verify(mockGitHubConfigFetcher, times(1)).fetchConfigContent();
    }
}
