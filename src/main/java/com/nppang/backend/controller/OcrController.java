package com.nppang.backend.controller;

import com.nppang.backend.dto.ReceiptDto;
import com.nppang.backend.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/parse")
    public ResponseEntity<ReceiptDto> parseReceipt(@RequestParam("file") MultipartFile file) {
        try {
            ReceiptDto receiptDto = ocrService.doOcrAndParse(file);
            return ResponseEntity.ok(receiptDto);
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
            return ResponseEntity.status(500).build();
        }
    }
}
