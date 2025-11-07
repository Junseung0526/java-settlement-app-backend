package com.nppang.backend.dto;

import lombok.Data;

@Data
public class NppangRequest {
    private Long totalAmount;
    private Long alcoholAmount;
    private int totalPeople;
    private int alcoholDrinkers;
}
