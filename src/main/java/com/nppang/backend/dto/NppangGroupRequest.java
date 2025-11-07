package com.nppang.backend.dto;

import lombok.Data;

@Data
public class NppangGroupRequest {
    private Long totalAmount;
    private Long alcoholAmount;
    private int alcoholDrinkers;
}
