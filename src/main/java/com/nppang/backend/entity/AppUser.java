package com.nppang.backend.entity;

import lombok.Data;

@Data
public class AppUser {
    private String id;
    private String username;
    private String nickname;
    private String password;
}

