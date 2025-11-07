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
            @PathVariable Long settlementId,
            @RequestParam("file") MultipartFile file) throws TesseractException, IOException {

        Receipt receipt = settlementService.addReceiptToSettlement(settlementId, file);
        AddReceiptResponse response = new AddReceiptResponse(settlementId, receipt.getId(), ReceiptDto.from(receipt));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable Long settlementId) {
        Settlement settlement = settlementService.getSettlement(settlementId);
        return ResponseEntity.ok(SettlementResponse.from(settlement));
    }

    @PostMapping("/{settlementId}/calculate")
    public ResponseEntity<NppangResponse> calculateSettlement(
            @PathVariable Long settlementId,
            @RequestBody CalculateSettlementRequest request) {
        Settlement settlement = settlementService.getSettlement(settlementId);
        NppangResponse response = nppangService.calculateNppangForSettlement(settlement, request.getAlcoholDrinkers());
        return ResponseEntity.ok(response);
    }
}
