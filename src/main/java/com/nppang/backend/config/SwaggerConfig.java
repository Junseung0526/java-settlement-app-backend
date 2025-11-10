package com.nppang.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String railwayServerUrl = "https://settlment-app-production.up.railway.app/";

        String localServerUrl = "http://localhost:8080";

        // JWT 설정
        // 1. "bearerAuth"라는 이름의 SecurityScheme 정의
        String schemeName = "bearerAuth";
        SecurityScheme securityScheme = new SecurityScheme()
                .name("Authorization") // 헤더 이름
                .type(SecurityScheme.Type.APIKEY) // 타입: API 키 (Header에 넣기 위해)
                .in(SecurityScheme.In.HEADER)     // 위치: Header
                .description("Enter 'Bearer ' followed by your token. Example: 'Bearer eyJhbGci...'");

        // 2. 모든 API에 이 "bearerAuth"를 적용하라는 SecurityRequirement 정의
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(schemeName);

        return new OpenAPI()
                .info(new Info()
                        .title("API Document")
                        .description("영수증 ocr 인식을 통한 통합 분배 API")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url(railwayServerUrl).description("Railway Production Server"),
                        new Server().url(localServerUrl).description("Local Development Server")
                ))
                // 3. 위에서 정의한 2가지를 OpenAPI 객체에 추가
                .components(new Components().addSecuritySchemes(schemeName, securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
