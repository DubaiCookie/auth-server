package com.authserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
                                import org.springframework.context.annotation.Bean;
                                import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI authServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Server API")
                        .description("Spring Boot 기반 인증 서버 REST API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Auth Server Team")
                                .email("support@authserver.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
