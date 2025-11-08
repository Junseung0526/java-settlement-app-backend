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
    public CompletableFuture<ResponseEntity<Receipt>> createReceipt(@RequestBody CreateReceiptRequest request) {
        return receiptService.createReceipt(request)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/receipts/{receiptId}")
    public CompletableFuture<ResponseEntity<Receipt>> getReceipt(@PathVariable String receiptId) {
        return receiptService.getReceiptById(receiptId)
                .thenApply(receipt -> receipt != null ? ResponseEntity.ok(receipt) : ResponseEntity.notFound().build());
    }

    @GetMapping("/groups/{groupId}/receipts")
    public CompletableFuture<ResponseEntity<List<Receipt>>> getGroupReceipts(@PathVariable String groupId) {
        return receiptService.getReceiptsByGroupId(groupId)
                .thenApply(ResponseEntity::ok);
    }
}
