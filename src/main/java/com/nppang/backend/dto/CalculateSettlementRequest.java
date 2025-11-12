package com.nppang.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CalculateSettlementRequest {
    private List<String> receiptIds;
}
