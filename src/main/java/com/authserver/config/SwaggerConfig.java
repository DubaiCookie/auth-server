package com.authserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.server-url}")
    private String serverOrigin;

    @Bean
    public OpenAPI authServerOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Auth Server API")
                        .description("Spring Boot 기반 인증 서버 REST API 문서")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url("/").description("Current Server"),
                        new Server().url(serverOrigin).description("Production Server")
                ));

        return openAPI;
    }
}
