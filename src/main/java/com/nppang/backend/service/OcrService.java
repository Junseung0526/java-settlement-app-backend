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

    /**
     * OCR을 수행하고 추출된 텍스트에서 영수증 정보를 파싱합니다.
     * @param file 영수증 이미지 파일
     * @return 파싱된 영수증 정보 DTO
     */

    public ReceiptDto doOcrAndParse(MultipartFile file) throws IOException, TesseractException {
        // 1. OCR 실행
        String rawText = doOcr(file);

        // 2. 파싱 로직 호출
        return parseReceiptText(rawText);
    }

    /*
     * 업로드된 이미지 파일에서 텍스트를 추출합니다.
     * @param file 영수증 이미지 파일
     * @return 추출된 텍스트
     */

    private String doOcr(MultipartFile file) throws IOException, TesseractException {
        File tempFile = convertMultipartFileToFile(file);

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(LANGUAGE);

        try {
            String result = tesseract.doOCR(tempFile);
            return result;
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("ocr-temp-", file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }

    private ReceiptDto parseReceiptText(String rawText) {

        Long totalAmount = null;
        String transactionDate = null;
        String storeName = "미확인";

        String[] lines = rawText.split("\n");

        Pattern totalPattern = Pattern.compile("(합계|결제금액|금액|승인금액|총액)\\D*(\\d[\\d, ]+)", Pattern.CASE_INSENSITIVE);
        Matcher totalMatcher = totalPattern.matcher(rawText.replace(" ", "").replace("원", "")); // 공백 및 '원' 제거 후 매칭

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
            // YYYY-MM-DD 형식으로 정리
            transactionDate = dateMatcher.group(1).replaceAll("[^0-9]", "-").substring(0, 10);
        }

        //상호명 추출 (가장 위의 첫 줄을 상호명으로 가정)
        try {
            if (lines.length > 0 && !lines[0].trim().isEmpty()) {
                storeName = lines[0].trim().replace(":", "").substring(0, Math.min(lines[0].trim().length(), 25)); // 상위 25자 제한
            }
        }
        catch (Exception e) {
        }

        List<ReceiptItem> items = new ArrayList<>();
        if (totalAmount != null) {
            ReceiptItem totalItem = ReceiptItem.builder()
                    .name("전체") // "Total"
                    .price(totalAmount)
                    .participants(new ArrayList<>()) // Participants are unknown at this stage
                    .build();
            items.add(totalItem);
        }

        // DTO 빌드 및 반환
        return ReceiptDto.builder()
                .totalAmount(totalAmount)
                .transactionDate(transactionDate)
                .storeName(storeName)
                .items(items)
                .payerId(null) // Payer is unknown at this stage
                .build();
    }
}
