package com.nppang.backend.controller;

import com.nppang.backend.service.FirebaseTestService;
import com.nppang.backend.service.ClovaOcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final FirebaseTestService firebaseTestService;
    private final ClovaOcrService clovaOcrService;

    public TestController(FirebaseTestService firebaseTestService, ClovaOcrService clovaOcrService) {
        this.firebaseTestService = firebaseTestService;
        this.clovaOcrService = clovaOcrService;
    }

    @PostMapping("/ocr/receipt")
    public ResponseEntity<String> uploadReceiptAndDoOcr(@RequestParam("file") MultipartFile file) {

        logger.info("--- [API CALL] /ocr/receipt endpoint accessed. ---");

        if (file.isEmpty()) {
            logger.error("File is empty (400 Bad Request)");
            return ResponseEntity.badRequest().body("Error: File is empty.");
        }

        logger.info("Received file: Name={}, Size={} bytes, ContentType={}",
            file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            String apiResponseJson = clovaOcrService.recognizeTextFromImage(file);
            logger.info("Clova OCR API call successful.");

            return ResponseEntity.ok("Successfully called Clova OCR. Full API Response:\n" + apiResponseJson);

        } catch (Exception e) {
            logger.error("Error during Clova OCR processing.", e);
            return ResponseEntity.internalServerError().body("Clova OCR API Error: " + e.getMessage());
        }
    }
}
