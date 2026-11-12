package com.molla.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServiceConfig {

    @Bean
    public RestClient aiRestClient(
            RestClientBuilderConfigurer configurer,

            @Value("${app.ai.base-url}")
            String baseUrl,

            @Value("${app.ai.api-key:}")
            String apiKey,

            @Value("${app.ai.connect-timeout}")
            Duration connectTimeout,

            @Value("${app.ai.read-timeout}")
            Duration readTimeout
    ) {
        String normalizedBaseUrl = baseUrl.matches("^https?://.*")
                ? baseUrl
                : "https://" + baseUrl;

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(
                connectTimeout
        );

        requestFactory.setReadTimeout(
                readTimeout
        );

        RestClient.Builder builder = configurer.configure(
                RestClient.builder()
        );

        builder
                .baseUrl(normalizedBaseUrl)
                .requestFactory(requestFactory);

        if (!apiKey.isBlank()) {
            builder.defaultHeader("X-AI-Service-Key", apiKey);
        }

        return builder.build();
    }
}
