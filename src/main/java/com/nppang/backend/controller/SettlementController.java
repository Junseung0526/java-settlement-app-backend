package com.nppang.backend.controller;

import com.nppang.backend.dto.*;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.service.NppangService;
import com.nppang.backend.service.SettlementService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

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
            return ResponseEntity.ok(SettlementResponse.from(settlement));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(SettlementResponse.builder()
                    .settlementId(null)
                    .settlementName("Error creating settlement: " + e.getMessage())
                    .totalAmount(0L)
                    .alcoholAmount(0L)
                    .receipts(Collections.emptyList())
                    .build());
        }
    }

    @PostMapping("/{settlementId}/receipts")
    public CompletableFuture<ResponseEntity<AddReceiptResponse>> addReceipt(
            @PathVariable String settlementId,
            @RequestParam("file") MultipartFile file) {

        return settlementService.addReceiptToSettlement(settlementId, file)
                .thenApply(receipt ->
                        new AddReceiptResponse(settlementId, receipt.getId(), ReceiptDto.from(receipt))
                )
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    return ResponseEntity.status(500).body(null);
                });
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable String settlementId) throws ExecutionException, InterruptedException {
        Settlement settlement = settlementService.getSettlement(settlementId).get();
        return ResponseEntity.ok(SettlementResponse.from(settlement));
    }

    @PostMapping("/{settlementId}/calculate")
    public ResponseEntity<CompletableFuture<NppangResponse>> calculateSettlement(
            @PathVariable String settlementId,
            @RequestBody CalculateSettlementRequest request) throws ExecutionException, InterruptedException {
        Settlement settlement = settlementService.getSettlement(settlementId).get();
        CompletableFuture<NppangResponse> response = nppangService.calculateNppangForSettlement(settlement, request.getAlcoholDrinkers());
        return ResponseEntity.ok(response);
    }
}
