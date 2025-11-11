package com.nppang.backend.controller;

import com.nppang.backend.dto.*;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.service.ReceiptService;
import com.nppang.backend.service.SettlementService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final ReceiptService receiptService;

    // 새로운 정산을 생성하는 API
    @PostMapping
    public ResponseEntity<SettlementResponse> createSettlement(@RequestBody CreateSettlementRequest request) {
        try {
            Settlement settlement = settlementService.createSettlement(request.getSettlementName(), request.getGroupId());
            SettlementResponse response = SettlementResponse.builder()
                    .settlementId(settlement.getId())
                    .settlementName(settlement.getName())
                    .groupId(settlement.getGroupId())
                    .totalAmount(0L)
                    .receipts(Collections.emptyList())
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(SettlementResponse.builder()
                    .settlementId(null)
                    .settlementName("Error creating settlement: " + e.getMessage())
                    .totalAmount(0L)
                    .receipts(Collections.emptyList())
                    .build());
        }
    }

    // 특정 정산의 상세 정보를 조회하는 API
    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable String settlementId) {
        try {
            CompletableFuture<Settlement> settlementFuture = settlementService.getSettlement(settlementId);
            CompletableFuture<List<Receipt>> receiptsFuture = receiptService.getReceiptsBySettlementId(settlementId);

            Settlement settlement = settlementFuture.join();
            List<Receipt> receipts = receiptsFuture.join();

            if (settlement == null) {
                return ResponseEntity.notFound().build();
            }

            List<ReceiptDto> receiptDtos = receipts.stream().map(ReceiptDto::from).collect(Collectors.toList());
            long totalAmount = receipts.stream().mapToLong(r -> r.getTotalAmount() != null ? r.getTotalAmount() : 0).sum();

            SettlementResponse response = SettlementResponse.builder()
                    .settlementId(settlement.getId())
                    .settlementName(settlement.getName())
                    .groupId(settlement.getGroupId())
                    .totalAmount(totalAmount)
                    .receipts(receiptDtos)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(500).build(); // Or more specific error handling
        }
    }

    // 특정 정산의 최종 결과를 계산하는 API입니다.
    @PostMapping("/{settlementId}/calculate")
    public ResponseEntity<CalculationResultDto> calculateSettlement(
            @PathVariable String settlementId,
            @RequestBody CalculateSettlementRequest request) {
        try {
            CalculationResultDto result = settlementService.calculateAndFinalizeSettlement(settlementId, request.getReceiptIds()).join();
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }
}
