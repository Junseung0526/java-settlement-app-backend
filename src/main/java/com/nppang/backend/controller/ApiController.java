package com.nppang.backend.controller;

import com.nppang.backend.service.OcrService;
import com.nppang.backend.service.FirebaseTestService;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final FirebaseTestService firebaseTestService;
    private final OcrService ocrService; // Tesseract 기반 OcrService로 변경

    public ApiController(FirebaseTestService firebaseTestService, OcrService ocrService) {
        this.firebaseTestService = firebaseTestService;
        this.ocrService = ocrService;
    }

    /**
     * 영수증 이미지를 업로드하고 Tesseract OCR을 호출하여 순수 텍스트 결과를 반환합니다.
     * URL: /api/v1/ocr/receipt
     * @param file 업로드된 이미지 파일 (MultipartFile)
     * @return 추출된 순수 텍스트(String)를 포함하는 ResponseEntity
     */
    @PostMapping("/ocr/receipt")
    public ResponseEntity<String> uploadReceiptAndDoOcr(@RequestParam("file") MultipartFile file) {

        logger.info("--- [API CALL] /ocr/receipt endpoint accessed using Tesseract. ---");

        // 1. 파일 유효성 검사
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            logger.error("400 Bad Request: File is empty or filename is null.");
            return ResponseEntity.badRequest().body("Error: File is empty or missing.");
        }

        // 2. OCR 서비스 호출 (Tesseract)
        try {
            String extractedText = ocrService.doOcr(file);

            logger.info("Tesseract OCR execution successful. Extracted {} characters.", extractedText.length());

            return ResponseEntity.ok(extractedText);

        } catch (IOException e) {
            logger.error("400 Bad Request: Error processing uploaded file bytes.", e);
            return ResponseEntity.badRequest().body("Error processing uploaded file.");

        } catch (TesseractException e) {
            logger.error("500 Internal Server Error: Tesseract OCR execution failed.", e);
            return ResponseEntity.internalServerError().body(
                "Tesseract OCR Error: Tesseract engine configuration or tessdata path is incorrect. Detail: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("500 Internal Server Error: An unexpected error occurred.", e);
            return ResponseEntity.internalServerError().body("An unexpected server error occurred: " + e.getMessage());
        }
    }

    // *FirebaseTestService를 사용하는 다른 엔드포인트는 여기에 추가하시면 됩니다.
}
