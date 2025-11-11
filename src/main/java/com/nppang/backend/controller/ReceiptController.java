package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateReceiptRequest;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/receipts")
    public ResponseEntity<Receipt> createReceipt(@RequestBody CreateReceiptRequest request) {
        try {
            Receipt receipt = receiptService.createReceipt(request).join();
            return ResponseEntity.ok(receipt);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    @GetMapping("/receipts/{receiptId}")
    public ResponseEntity<Receipt> getReceipt(@PathVariable String receiptId) {
        try {
            Receipt receipt = receiptService.getReceiptById(receiptId).join();
            return receipt != null ? ResponseEntity.ok(receipt) : ResponseEntity.notFound().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }

    @GetMapping("/groups/{groupId}/receipts")
    public ResponseEntity<List<Receipt>> getGroupReceipts(@PathVariable String groupId) {
        try {
            List<Receipt> receipts = receiptService.getReceiptsByGroupId(groupId).join();
            return ResponseEntity.ok(receipts);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build(); // Or more specific error handling
        }
    }
}
