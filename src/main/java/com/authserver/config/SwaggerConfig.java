package com.authserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
                                import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.swagger.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI authServerOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url(serverUrl)))
                .info(new Info()
                        .title("Auth Server API")
                        .description("Spring Boot 기반 인증 서버 REST API 문서")
                        .version("v1.0.0"));
    }
}
