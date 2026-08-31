package com.jf.PetApp.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Shared RestTemplate for every outbound call to a third-party API (Brapi, Gemini,
 * LibreTranslate). Without an explicit timeout, a slow or hung provider blocks the request
 * thread indefinitely — these values are a deliberate, generous-but-bounded ceiling, not a
 * measurement of any provider's expected latency.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Separate from {@link #restTemplate(RestTemplateBuilder)}: Claude Opus 5 runs adaptive
     * thinking by default, which routinely takes well past 10s to finish a reply (measured
     * 7-10s+ even on a trivial prompt) — the 10s ceiling above is sized for fast, non-reasoning
     * APIs (Brapi/Gemini/LibreTranslate) and was cutting every Anthropic call off mid-response,
     * throwing a {@code ResourceAccessException} that looked like a network failure but was
     * actually this timeout. Read timeout here is generous enough for a normal chat-length
     * reply without leaving a hung request thread forever.
     */
    @Bean
    public RestTemplate anthropicRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(45))
                .build();
    }
}
