package com.jf.PetApp.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpClientConfigTest {

    private final HttpClientConfig config = new HttpClientConfig();

    @Test
    void restTemplate_ConfiguresAFiveSecondConnectTimeoutAndTenSecondReadTimeout() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate expected = new RestTemplate();
        when(builder.connectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.readTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(expected);

        RestTemplate result = config.restTemplate(builder);

        verify(builder).connectTimeout(Duration.ofSeconds(5));
        verify(builder).readTimeout(Duration.ofSeconds(10));
        assertSame(expected, result);
    }

    @Test
    void restTemplate_WithARealBuilder_BuildsAUsableRestTemplate() {
        RestTemplate result = config.restTemplate(new RestTemplateBuilder());

        assertSame(RestTemplate.class, result.getClass());
    }
}
