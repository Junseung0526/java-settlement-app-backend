package com.nppang.backend.service;

import com.nppang.backend.dto.ReceiptDto;
import com.nppang.backend.entity.ReceiptItem;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    @Value("${tesseract.datapath}")
    private String tessDataPath;
    private static final String LANGUAGE = "kor";

    // 영수증 이미지 파일에서 텍스트를 분석하여 영수증 DTO를 생성
    public ReceiptDto doOcrAndParse(MultipartFile file) throws IOException, TesseractException {
        String rawText = doOcr(file);
        return parseReceiptText(rawText);
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
