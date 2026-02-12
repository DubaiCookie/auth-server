package com.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정
 */
@Configuration
public class WebClientConfig {

    @Value("${queue.server.url}")
    private String queueServerUrl;

    @Bean
    public WebClient queueWebClient() {
        return WebClient.builder()
                .baseUrl(queueServerUrl)
                .build();
    }
}

