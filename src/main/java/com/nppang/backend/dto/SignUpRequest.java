package com.nppang.backend.dto;

import lombok.Data;

@Data
public class SignUpRequest {
    private String username; // 이메일
    private String password;
    private String nickname;
}