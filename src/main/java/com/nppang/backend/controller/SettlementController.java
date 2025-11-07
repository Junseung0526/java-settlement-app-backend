package com.nppang.backend.controller;

import com.nppang.backend.dto.*;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.service.NppangService;
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
    private final NppangService nppangService;

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

    @PostMapping("/{settlementId}/receipts")
    public CompletableFuture<ResponseEntity<AddReceiptResponse>> addReceipt(
            @PathVariable String settlementId,
            @RequestBody AddReceiptRequest request) {

        return settlementService.addReceiptToSettlement(settlementId, request)
                .thenApply(receipt ->
                        new AddReceiptResponse(settlementId, receipt.getId(), ReceiptDto.from(receipt))
                )
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    return ResponseEntity.status(500).body(null);
                });
    }

    @GetMapping("/{settlementId}")
    public CompletableFuture<ResponseEntity<SettlementResponse>> getSettlement(@PathVariable String settlementId) {
        CompletableFuture<Settlement> settlementFuture = settlementService.getSettlement(settlementId);
        CompletableFuture<List<Receipt>> receiptsFuture = settlementService.getReceiptsForSettlement(settlementId);

        return settlementFuture.thenCombine(receiptsFuture, (settlement, receipts) -> {
            if (settlement == null) {
                return ResponseEntity.notFound().build();
            }

            long totalAmount = receipts.stream().mapToLong(r -> r.getTotalAmount() != null ? r.getTotalAmount() : 0).sum();
            List<ReceiptDto> receiptDtos = receipts.stream().map(ReceiptDto::from).collect(Collectors.toList());

            SettlementResponse response = SettlementResponse.builder()
                    .settlementId(settlement.getId())
                    .settlementName(settlement.getName())
                    .groupId(settlement.getGroupId())
                    .totalAmount(totalAmount)
                    .receipts(receiptDtos)
                    .build();

            return ResponseEntity.ok(response);
        });
    }

    @PostMapping("/{settlementId}/calculate")
    public CompletableFuture<ResponseEntity<CalculationResultDto>> calculateSettlement(
            @PathVariable String settlementId) {
        return settlementService.getSettlement(settlementId)
                .thenCompose(nppangService::calculateSettlement)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(500).build());
    }
}
