package com.alumni.searcher.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "phantombuster")
public record PhantomBusterProperties(
        String baseUrl,
        String apiKey,
        String agentId,
        String sessionCookie,
        int maxResults,
        Duration pollInterval,
        Duration timeout) {

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(agentId) && StringUtils.hasText(sessionCookie);
    }
}
