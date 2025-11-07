package com.nppang.backend.controller;

import com.nppang.backend.dto.*;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.service.NppangService;
import com.nppang.backend.service.SettlementService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final NppangService nppangService;

    @PostMapping
    public ResponseEntity<SettlementResponse> createSettlement(@RequestBody CreateSettlementRequest request) {
        Settlement settlement = settlementService.createSettlement(request.getSettlementName(), request.getGroupId());
        return ResponseEntity.ok(SettlementResponse.from(settlement));
    }

    @PostMapping("/{settlementId}/receipts")
    public ResponseEntity<AddReceiptResponse> addReceipt(
            @PathVariable String settlementId,
            @RequestParam("file") MultipartFile file) throws TesseractException, IOException, ExecutionException, InterruptedException {

        Receipt receipt = settlementService.addReceiptToSettlement(settlementId, file).get();
        AddReceiptResponse response = new AddReceiptResponse(settlementId, receipt.getId(), ReceiptDto.from(receipt));
        return ResponseEntity.ok(response);
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
