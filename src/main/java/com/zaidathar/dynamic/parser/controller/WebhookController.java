package com.zaidathar.dynamic.parser.controller;

import com.zaidathar.dynamic.parser.config.MappingConfigLoader;
import com.zaidathar.dynamic.parser.service.GitHubConfigFetcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Optional;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final GitHubConfigFetcher gitHubConfigFetcher;
    private final MappingConfigLoader mappingConfigLoader;

    @Value("${app.config.webhook.secret}")
    private String webhookSecret;

    public WebhookController(GitHubConfigFetcher gitHubConfigFetcher, MappingConfigLoader mappingConfigLoader) {
        this.gitHubConfigFetcher = gitHubConfigFetcher;
        this.mappingConfigLoader = mappingConfigLoader;
    }

    @PostMapping("/webhook/github/config-reload")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-GitHub-Event") String githubEvent,
            HttpServletRequest request) {

        log.info("Received GitHub webhook event: {}", githubEvent);
        try {
            if (!verifySignature(payload, signature, webhookSecret)) {
                log.warn("Webhook signature mismatch. Request denied.");
                return new ResponseEntity<>("Invalid signature", HttpStatus.UNAUTHORIZED);
            }
            log.debug("Webhook signature verified successfully.");
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    "Internal server error during signature verification",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (!"push".equals(githubEvent)) {
            log.info("Ignoring non-push event: {}", githubEvent);
            return new ResponseEntity<>("Event type ignored", HttpStatus.OK);
        }

        try {
            Optional<InputStream> configStreamOptional = gitHubConfigFetcher.fetchConfigContent();
            if (configStreamOptional.isPresent()) {
                mappingConfigLoader.loadConfigFromStream(configStreamOptional.get());
                log.info("Configuration successfully hot-reloaded via GitHub webhook.");
                return new ResponseEntity<>("Configuration reloaded", HttpStatus.OK);
            } else {
                log.warn("Webhook received but failed to fetch new config content from GitHub.");
                return new ResponseEntity<>("Failed to fetch config", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            log.error("Error during configuration reload via webhook: {}", e.getMessage(), e);
            return new ResponseEntity<>("Error reloading config", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/fetch/config")
    public ResponseEntity<String> fetchConfig(){
        try{
            Optional<InputStream> configStreamOptional = gitHubConfigFetcher.fetchConfigContent();
            if (configStreamOptional.isPresent()) {
                mappingConfigLoader.loadConfigFromStream(configStreamOptional.get());
                log.info("Configuration successfully hot-reloaded.");
                return new ResponseEntity<>("Configuration reloaded", HttpStatus.OK);
            } else {
                log.warn("Reload Config call received but failed to fetch new config content from GitHub.");
                return new ResponseEntity<>("Failed to fetch config", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            log.error("Error during configuration reload via webhook: {}", e.getMessage(), e);
            return new ResponseEntity<>("Error reloading config", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean verifySignature(String payload, String signatureHeader, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }

        String actualSignature = signatureHeader.substring(7);

        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKey);

        byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String expectedHash = formatter.toString();

        return expectedHash.equals(actualSignature);
    }
}