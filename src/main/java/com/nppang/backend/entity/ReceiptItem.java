package com.nppang.backend.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptItem {
    private String name;
    private Long price;
    private List<String> participants; // List of user IDs
}
