package com.zaidathar.dynamic.parser.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;


import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebhookSecurityInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebhookSecurityInterceptor.class);


    private final RateLimiterRegistry rateLimiterRegistry;

    private final Map<String, RateLimiter> minuteLimiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> hourlyLimiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> dailyLimiters = new ConcurrentHashMap<>();


    private final Set<String> githubWebhookIpRanges = new HashSet<>();
    private final RestTemplate restTemplate;

    private static final Pattern IP_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");

    @Value("${app.security.webhook.rate-limit.minute:1.0}")
    private double minuteRateLimit;
    @Value("${app.security.webhook.rate-limit.hour:5.0}")
    private double hourRateLimit;
    @Value("${app.security.webhook.rate-limit.day:20.0}")
    private double dayRateLimit;

    private static final String GITHUB_META_API_URL = "https://api.github.com/meta";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public WebhookSecurityInterceptor(RestTemplateBuilder restTemplateBuilder) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(requestFactory);

        this.rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        fetchGithubWebhookIpRanges();
        scheduler.scheduleAtFixedRate(this::fetchGithubWebhookIpRanges, 0, 6, TimeUnit.HOURS);
        scheduler.scheduleAtFixedRate(this::cleanupOldRateLimiters, 24, 24, TimeUnit.HOURS);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIpAddress(request);
        log.info("Received webhook request from IP: {}", clientIp);

        if (!isIpWhitelisted(clientIp)) {
            log.warn("Unauthorized webhook request from non-whitelisted IP: {}", clientIp);
            response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden: IP not whitelisted.");
            return false;
        }


        if (!acquirePermit(clientIp, minuteRateLimit, Duration.ofMinutes(1), minuteLimiters, "minute")) {
            log.warn("Rate limit exceeded for IP {} (minute).", clientIp);
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests: Minute limit exceeded.");
            return false;
        }
        if (!acquirePermit(clientIp, hourRateLimit, Duration.ofHours(1), hourlyLimiters, "hour")) {
            log.warn("Rate limit exceeded for IP {} (hour).", clientIp);
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests: Hour limit exceeded.");
            return false;
        }
        if (!acquirePermit(clientIp, dayRateLimit, Duration.ofDays(1), dailyLimiters, "day")) {
            log.warn("Rate limit exceeded for IP {} (day).", clientIp);
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests: Day limit exceeded.");
            return false;
        }

        return true;
    }


    private boolean acquirePermit(String ip, double limit, Duration refreshPeriod,
                                  Map<String, RateLimiter> limiters, String periodName) {
        RateLimiter limiter = limiters.computeIfAbsent(periodName, k -> {
            // Reason for taking periodName here because Github Actions had 5K IP
            // Each time getting webhook call from different IP which is why Rate Limiting was not working
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod((int) limit)
                    .limitRefreshPeriod(refreshPeriod)
                    .timeoutDuration(Duration.ZERO)
                    .build();
            return RateLimiter.of("webhook-" + periodName + "-", config);
        });


        boolean acquired = limiter.acquirePermission();
        log.debug("IP {} {} limit acquired: {}", ip, periodName, acquired);
        return acquired;
    }


    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            Matcher matcher = IP_PATTERN.matcher(xForwardedForHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return request.getRemoteAddr();
    }


    private boolean isIpWhitelisted(String clientIp) {
        if (clientIp.equals("0:0:0:0:0:0:0:1") || clientIp.equals("127.0.0.1")) {
            log.debug("Allowing localhost IP for testing: {}", clientIp);
            return true;
        }

        if (githubWebhookIpRanges.isEmpty()) {
            log.warn("GitHub webhook IP ranges are not loaded. Whitelisting might be ineffective. Denying request.");
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(clientIp);
            for (String ipRange : githubWebhookIpRanges) {
                if (isInRange(address, ipRange)) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.error("Error parsing client IP address: {}", clientIp, e);
        }
        return false;
    }

    private boolean isInRange(InetAddress address, String cidr) throws IOException {
        String[] parts = cidr.split("/");
        InetAddress cidrAddress = InetAddress.getByName(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        byte[] ipBytes = address.getAddress();
        byte[] cidrBytes = cidrAddress.getAddress();

        if (ipBytes.length != cidrBytes.length) {
            return false;
        }

        for (int i = 0; i < prefixLength / 8; i++) {
            if (ipBytes[i] != cidrBytes[i]) {
                return false;
            }
        }

        if (prefixLength % 8 != 0) {
            int mask = 0xFF << (8 - (prefixLength % 8));
            if ((ipBytes[prefixLength / 8] & mask) != (cidrBytes[prefixLength / 8] & mask)) {
                return false;
            }
        }
        return true;
    }

    private void fetchGithubWebhookIpRanges() {
        log.info("Fetching GitHub webhook IP ranges from API: {}", GITHUB_META_API_URL);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(GITHUB_META_API_URL, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Set<String> newIpRanges = new HashSet<>((java.util.List<String>) response.getBody().get("actions"));
                if (newIpRanges.isEmpty()) {
                    log.warn("Fetched GitHub webhook IP ranges are empty. This might indicate an API change or issue.");
                    return;
                }
                githubWebhookIpRanges.clear();
                githubWebhookIpRanges.addAll(newIpRanges);
                log.info("Successfully updated GitHub webhook IP ranges. Total ranges: {}", githubWebhookIpRanges.size());
                log.debug("GitHub webhook IP ranges: {}", githubWebhookIpRanges);
            } else {
                log.warn("Failed to fetch GitHub webhook IP ranges. Status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error fetching GitHub webhook IP ranges: {}", e.getMessage(), e);
        }
    }

    private void cleanupOldRateLimiters() {
        log.info("Cleaning up old rate limiters (placeholder - consider distributed solution for scale)...");
        log.info("Rate limiter cleanup complete.");
    }
}
