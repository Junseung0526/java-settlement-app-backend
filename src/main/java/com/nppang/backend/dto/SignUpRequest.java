package com.nppang.backend.dto;

import lombok.Data;

@Data
public class SignUpRequest {
    private String username;
    private String nickname;
    private String password;
}