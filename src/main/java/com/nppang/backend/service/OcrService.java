package com.nppang.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nppang.backend.dto.ReceiptDto;
import com.nppang.backend.entity.ReceiptItem;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    @Value("${tesseract.datapath}")
    private String tessDataPath;
    private static final String LANGUAGE = "kor";

    private final RestTemplate restTemplate;
    private final String geminiApiKey;
    private final String geminiApiUrl;

    public OcrService(RestTemplate restTemplate,
                      @Value("${gemini.api-key}") String geminiApiKey,
                      @Value("${gemini.api-url}") String geminiApiUrl) {
        this.restTemplate = restTemplate;
        this.geminiApiKey = geminiApiKey;
        this.geminiApiUrl = geminiApiUrl;
    }

    // 영수증 이미지 파일에서 텍스트를 분석하여 영수증 DTO를 생성
    public ReceiptDto doOcrAndParse(MultipartFile file) throws IOException, TesseractException {
        String rawText = doOcr(file);
        String improvedText = improveOcrWithGemini(rawText); // Gemini로 텍스트 개선
        return parseReceiptText(improvedText);
    }

    // Tesseract OCR을 사용하여 이미지 파일에서 텍스트를 추출
    private String doOcr(MultipartFile file) throws IOException, TesseractException {
        File tempFile = convertMultipartFileToFile(file);

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(LANGUAGE);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // Gemini API를 사용하여 OCR 텍스트를 개선
    private String improveOcrWithGemini(String rawText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = "다음은 Tesseract OCR로 추출된 한국어 영수증 텍스트입니다. 오류가 있을 수 있습니다. 유효한 영수증 형식으로 텍스트를 수정하고, 항목 이름, 가격, 총액에 특히 주의를 기울여주세요. 줄 바꿈을 유지해주세요. 텍스트:\n\n" + rawText;

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contents = Map.of("parts", Collections.singletonList(textPart));
        Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(contents));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            // 복잡한 JSON 구조 파싱
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return rawText; // Gemini API 호출 실패 시 원본 텍스트 반환
        }
    }

    // MultipartFile을 임시 File 객체로 변환
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("ocr-temp-", file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }

    // 추출된 텍스트를 파싱하여 영수증의 주요 정보를 추출
    private ReceiptDto parseReceiptText(String rawText) {
        Long totalAmount = null;
        String transactionDate = null;
        String storeName = "미확인";

        String[] lines = rawText.split("\n");

        Pattern totalPattern = Pattern.compile("(합계|결제금액|금액|승인금액|총액)\\D*(\\d[\\d, ]+)", Pattern.CASE_INSENSITIVE);
        Matcher totalMatcher = totalPattern.matcher(rawText.replace(" ", "").replace("원", ""));

        if (totalMatcher.find()) {
            try {
                String amountStr = totalMatcher.group(2).replaceAll("[^0-9]", "");
                totalAmount = Long.parseLong(amountStr);
            } catch (NumberFormatException e) {
            }
        }

        Pattern datePattern = Pattern.compile("(\\d{4}[\\./\\- ]\\d{2}[\\./\\- ]\\d{2})");
        Matcher dateMatcher = datePattern.matcher(rawText);

        if (dateMatcher.find()) {
            transactionDate = dateMatcher.group(1).replaceAll("[^0-9]", "-").substring(0, 10);
        }

        try {
            if (lines.length > 0 && !lines[0].trim().isEmpty()) {
                storeName = lines[0].trim().replace(":", "").substring(0, Math.min(lines[0].trim().length(), 25));
            }
        }
        catch (Exception e) {
        }

        List<ReceiptItem> items = new ArrayList<>();
        if (totalAmount != null) {
            ReceiptItem totalItem = ReceiptItem.builder()
                    .name("전체")
                    .price(totalAmount)
                    .participants(new ArrayList<>())
                    .build();
            items.add(totalItem);
        }

        return ReceiptDto.builder()
                .totalAmount(totalAmount)
                .transactionDate(transactionDate)
                .storeName(storeName)
                .items(items)
                .payerId(null)
                .build();
    }
}
