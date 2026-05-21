package com.sportvenue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration.
 * Truy cập: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI sportVenueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🏟️ Sport Venue Management API")
                        .description("Nền tảng đặt sân thể thao trực tuyến — REST API Documentation")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Team 1 — SE20A09")
                                .email("sportvenue@example.com"))
                        .license(new License()
                                .name("Academic License")
                                .url("https://github.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Dev"),
                        new Server().url("https://api.sportvenue.example.com").description("Production")
                ));
    }
}
