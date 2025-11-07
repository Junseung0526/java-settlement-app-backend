package com.nppang.backend.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Settlement {
    private String id;
    private String name;
    private String groupId;
    private Map<String, Receipt> receipts;
}
