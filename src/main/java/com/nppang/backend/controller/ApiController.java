package com.nppang.backend.controller;

import com.nppang.backend.dto.NppangRequest;
import com.nppang.backend.dto.NppangResponse;
import com.nppang.backend.dto.ReceiptInfo;
import com.nppang.backend.service.NppangService;
import com.nppang.backend.service.OcrService;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final OcrService ocrService;
    private final NppangService nppangService;

    public ApiController(OcrService ocrService, NppangService nppangService) {
        this.ocrService = ocrService;
        this.nppangService = nppangService;
    }

    /**
     * 영수증 이미지를 업로드하고 Tesseract OCR을 호출하여 파싱된 정보를 반환합니다.
     * URL: /api/v1/ocr/receipt
     * @param file 업로드된 영수증 이미지 파일
     * @return 파싱된 ReceiptInfo DTO (JSON)
     */

    @PostMapping("/ocr/receipt")
    public ResponseEntity<?> uploadReceiptAndDoOcr(@RequestParam("file") MultipartFile file) {

        logger.info("--- [API CALL] /ocr/receipt endpoint accessed for parsing. ---");

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            logger.error("400 Bad Request: File is empty or filename is null.");
            return ResponseEntity.badRequest().body("Error: File is empty or missing.");
        }

        try {
            ReceiptInfo parsedInfo = ocrService.doOcrAndParse(file);
            logger.info("Parsing successful. Total Amount: {}", parsedInfo.getTotalAmount());
            return ResponseEntity.ok(parsedInfo);

        } catch (IOException e) {
            logger.error("400 Bad Request: Error processing uploaded file bytes.", e);
            return ResponseEntity.badRequest().body("Error processing uploaded file.");

        } catch (TesseractException e) {
            logger.error("500 Internal Server Error: Tesseract OCR execution failed.", e);
            return ResponseEntity.internalServerError().body(
                    "Tesseract OCR Error: Tesseract engine configuration or tessdata path might be incorrect. Detail: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("500 Internal Server Error: An unexpected error occurred.", e);
            return ResponseEntity.internalServerError().body("An unexpected server error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/nppang/calculate")
    public ResponseEntity<NppangResponse> calculateNppang(@RequestBody NppangRequest request) {
        try {
            NppangResponse response = nppangService.calculateNppang(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
