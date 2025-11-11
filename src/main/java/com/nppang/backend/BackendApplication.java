package com.nppang.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
public class    BackendApplication {

    public static void main(String[] args) {

        // Spring Boot가 실행되기 전에, SecurityContextHolder의 전략을
        // "새로 생성되는 모든 스레드(e.g., CompletableFuture)에
        // 기존 인증 정보(SecurityContext)를 상속(Inherit)하라"는 전략으로 변경합니다.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

        // 이제 Spring을 실행합니다.
        SpringApplication.run(BackendApplication.class, args);
    }
}
