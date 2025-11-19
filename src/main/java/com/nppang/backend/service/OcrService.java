package com.nppang.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    @Value("${tesseract.datapath}")
    private String tessDataPath;
    private static final String LANGUAGE = "kor";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    private final String geminiApiUrl;

    public OcrService(RestTemplate restTemplate,
                      ObjectMapper objectMapper,
                      @Value("${gemini.api-key}") String geminiApiKey,
                      @Value("${gemini.api-url}") String geminiApiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        this.geminiApiUrl = geminiApiUrl;
    }

    // [메인 흐름] 1. OCR 수행 -> 2. Gemini 분석 시도 -> (실패 시) 3. 자체 Fallback 수행
    public ReceiptDto doOcrAndParse(MultipartFile file) throws IOException, TesseractException {
        String rawText = doOcr(file);
        return extractReceiptDataWithGemini(rawText);
    }

    // [핵심 로직 1] Tesseract OCR 실행 (이미지 -> 텍스트 변환)
    private String doOcr(MultipartFile file) throws IOException, TesseractException {
        // Tesseract는 File 객체가 필요하므로 임시 파일 생성
        File tempFile = convertMultipartFileToFile(file);
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(LANGUAGE);

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            // 메모리 누수 방지를 위해 임시 파일 삭제
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // [핵심 로직 2] Gemini API 호출 및 데이터 구조화
    private ReceiptDto extractReceiptDataWithGemini(String rawText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 프롬프트 설정: 역할 부여 + 제약 사항 + 예시 데이터
        String prompt = """
                SYSTEM_ROLE: 너는 한국어 영수증 OCR 데이터 후처리 전문가야.
                INPUT: Tesseract OCR로 추출된 노이즈가 섞인 영수증 텍스트.
                TASK: 텍스트를 분석하여 아래 정의된 JSON 스키마에 맞춰 데이터를 구조화해.
                
                [제약 사항]
                1. 출력 형식: 오직 순수한 JSON 문자열만 출력 (마크다운 제외).
                2. 상호명(storeName): 지점명, 대표자 등 제외하고 가게 이름만 추출.
                3. 날짜(transactionDate): 'YYYY-MM-DD' 형식 필수.
                4. 금액(totalAmount): '합계', '결제금액'의 숫자만 추출.
                5. 품목(items): 부가세, 할인 등 제외하고 실제 구매 상품만 추출.
                
                [출력 JSON 예시]
                {
                  "storeName": "김밥천국",
                  "transactionDate": "2024-11-19",
                  "totalAmount": 15000,
                  "items": [
                    {"name": "참치김밥", "price": 4500},
                    {"name": "라볶이", "price": 5500}
                  ]
                }
                
                [분석할 OCR 텍스트]
                %s
                """.formatted(rawText);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contents = Map.of("parts", Collections.singletonList(textPart));
        Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(contents));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null || !response.containsKey("candidates")) {
                throw new RuntimeException("No response from Gemini");
            }

            // 응답 파싱: JSON 문자열 추출
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String jsonResponse = (String) parts.get(0).get("text");

            jsonResponse = cleanJsonString(jsonResponse);
            return parseJsonToDto(jsonResponse);

        } catch (Exception e) {
            System.err.println("Gemini API Error: " + e.getMessage());
            // ★ Gemini API 실패 시, 정규식 기반 Fallback 메서드로 전환
            return parseFallback(rawText);
        }
    }

    // [핵심 로직 3] Fallback: 정규표현식을 이용한 수동 파싱 (AI 실패 대비)
    private ReceiptDto parseFallback(String rawText) {
        String storeName = "상호명 미확인";
        String transactionDate = null;
        long totalAmount = 0L;

        // 1. 상호명: 첫 번째 유효한 줄을 상호명으로 추정
        String[] lines = rawText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.contains("영수증") && trimmed.length() > 1) {
                storeName = trimmed;
                break;
            }
        }

        // 2. 날짜: YYYY-MM-DD 형식을 정규식으로 검색
        Pattern datePattern = Pattern.compile("(\\d{4})[-/.년\\s]+(\\d{1,2})[-/.월\\s]+(\\d{1,2})");
        Matcher dateMatcher = datePattern.matcher(rawText);
        if (dateMatcher.find()) {
            transactionDate = String.format("%s-%02d-%02d",
                    dateMatcher.group(1),
                    Integer.parseInt(dateMatcher.group(2)),
                    Integer.parseInt(dateMatcher.group(3)));
        }

        // 3. 금액: '합계', '총액' 키워드 뒤의 숫자 검색
        Pattern amountPattern = Pattern.compile("(합계|총액|결제금액|받을금액).*?([\\d,]+)");
        Matcher amountMatcher = amountPattern.matcher(rawText);
        if (amountMatcher.find()) {
            String amountStr = amountMatcher.group(2).replaceAll("[^0-9]", "");
            if (!amountStr.isEmpty()) {
                totalAmount = Long.parseLong(amountStr);
            }
        }

        // 4. 아이템: 개별 품목 파싱 불가 시 '전체 금액' 1건으로 처리
        List<ReceiptItem> items = new ArrayList<>();
        if (totalAmount > 0) {
            items.add(ReceiptItem.builder()
                    .name("스캔된 품목(상세 내역 미포함)")
                    .price(totalAmount)
                    .participants(new ArrayList<>())
                    .build());
        }

        return ReceiptDto.builder()
                .storeName(storeName + " (자동분석)")
                .transactionDate(transactionDate)
                .totalAmount(totalAmount)
                .items(items)
                .payerId(null)
                .build();
    }

    private String cleanJsonString(String jsonResponse) {
        if (jsonResponse.startsWith("```json")) {
            jsonResponse = jsonResponse.substring(7);
        } else if (jsonResponse.startsWith("```")) {
            jsonResponse = jsonResponse.substring(3);
        }
        if (jsonResponse.endsWith("```")) {
            jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
        }
        return jsonResponse.trim();
    }

    private ReceiptDto parseJsonToDto(String jsonStr) {
        try {
            JsonNode root = objectMapper.readTree(jsonStr);

            String storeName = root.path("storeName").asText("미확인");
            String date = root.has("transactionDate") && !root.get("transactionDate").isNull()
                    ? root.get("transactionDate").asText()
                    : null;
            Long totalAmount = root.path("totalAmount").asLong(0);

            List<ReceiptItem> items = new ArrayList<>();
            JsonNode itemsNode = root.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    items.add(ReceiptItem.builder()
                            .name(itemNode.path("name").asText("알 수 없음"))
                            .price(itemNode.path("price").asLong(0))
                            .participants(new ArrayList<>())
                            .build());
                }
            }

            // 아이템이 없으면 총액만 있는 단일 아이템 생성
            if (items.isEmpty() && totalAmount > 0) {
                items.add(ReceiptItem.builder()
                        .name("전체")
                        .price(totalAmount)
                        .participants(new ArrayList<>())
                        .build());
            }

            return ReceiptDto.builder()
                    .storeName(storeName)
                    .transactionDate(date)
                    .totalAmount(totalAmount)
                    .items(items)
                    .payerId(null)
                    .build();

        } catch (JsonProcessingException e) {
            System.err.println("JSON Parsing Error: " + e.getMessage());
            return ReceiptDto.builder().storeName("파싱 오류").build();
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("ocr-temp-", file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }
}
