package com.nppang.backend.controller;

import com.nppang.backend.dto.CreateReceiptRequest;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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

    @DeleteMapping("/receipts/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable String receiptId) {
        try {
            // Service의 removeReceipts가 List를 받으므로 리스트로 감싸서 전달
            receiptService.removeReceipts(Collections.singletonList(receiptId)).join();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/receipts")
    public ResponseEntity<Void> deleteReceipts(@RequestBody List<String> receiptIds) {
        try {
            receiptService.removeReceipts(receiptIds).join();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
