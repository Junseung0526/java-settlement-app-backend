package com.nppang.backend.entity;

import lombok.Data;

@Data
public class AppUser {
    private String id;
    private String username;
    private String password; // In a real app, this should be hashed
}

