package com.nppang.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory; // ⭐️ 추가
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ClovaOcrService {

    private final RestTemplate restTemplate;
    private final String invokeUrl;
    private final String secretKey;
    private final ObjectMapper objectMapper;

    public ClovaOcrService(@Value("${clova.ocr.invoke-url}") String invokeUrl,
                           @Value("${clova.ocr.secret-key}") String secretKey) {
        this.invokeUrl = invokeUrl;
        this.secretKey = secretKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        // RestTemplate에 팩토리 적용
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public String recognizeTextFromImage(MultipartFile file) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-OCR-SECRET", secretKey);

        String encodedImage = Base64.getEncoder().encodeToString(file.getBytes());

        String requestId = UUID.randomUUID().toString();

        Map<String, Object> imageMap = new HashMap<>();
        imageMap.put("format", getFileExtension(file.getOriginalFilename()));
        imageMap.put("name", file.getOriginalFilename());
        imageMap.put("data", encodedImage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("images", new Object[]{imageMap});
        requestBody.put("requestId", requestId);
        requestBody.put("timestamp", System.currentTimeMillis());
        requestBody.put("version", "V2");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String apiResponse = restTemplate.postForObject(invokeUrl, entity, String.class);

        return apiResponse;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        }
        return "JPG";
    }
}
