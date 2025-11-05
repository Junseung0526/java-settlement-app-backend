package com.nppang.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server; // Server 클래스 추가
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List; // List import 추가

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String railwayServerUrl = "https://settlment-app-production.up.railway.app/";

        String localServerUrl = "http://localhost:8080";

        return new OpenAPI()
                .info(new Info()
                        .title("API Document")
                        .description("영수증 ocr 인식을 통한 통합 분배 API")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url(railwayServerUrl).description("Railway Production Server"),
                        new Server().url(localServerUrl).description("Local Development Server")
                ));
    }
}
