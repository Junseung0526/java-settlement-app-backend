package com.nppang.backend.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class UserGroup {
    private String id;
    private String name;
    private Map<String, Boolean> members = new HashMap<>();
}
