package com.nppang.backend.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    // Tesseract 훈련 데이터(tessdata) 경로 설정.
    // application.properties나 yml 파일에 설정해야 합니다.
    @Value("${tesseract.datapath}")
    private String tessDataPath;

    // 한국어 언어 코드 설정
    private static final String LANGUAGE = "kor";

    /**
     * 업로드된 이미지 파일에서 텍스트를 추출합니다.
     * @param file 영수증 이미지 파일 (MultipartFile)
     * @return 추출된 텍스트
     */
    public String doOcr(MultipartFile file) throws IOException, TesseractException {
        // 1. Tesseract 객체 생성 및 설정
        ITesseract tesseract = new Tesseract();

        // 훈련 데이터 경로 설정
        tesseract.setDatapath(tessDataPath);

        // 인식할 언어 설정 (한국어)
        tesseract.setLanguage(LANGUAGE);

        // (선택 사항) OCR 엔진 모드 설정 (일반적으로 기본 설정이 좋음)
        // tesseract.setOcrEngineMode(ITesseract.OEM_LSTM_ONLY);

        // 2. MultipartFile을 Tesseract가 처리할 수 있는 File 객체로 변환
        // 임시 파일을 생성하여 사용합니다.
        File tempFile = convertMultipartFileToFile(file);

        try {
            // 3. OCR 실행 및 결과 반환
            String result = tesseract.doOCR(tempFile);
            return result;
        } finally {
            // 4. 임시 파일 삭제 (리소스 정리)
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * MultipartFile을 java.io.File로 변환하는 헬퍼 메서드
     */
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        // 파일 이름으로 임시 파일 생성
        File convFile = File.createTempFile("ocr-temp-", file.getOriginalFilename());
        // MultipartFile의 내용을 임시 파일로 복사
        file.transferTo(convFile);
        return convFile;
    }
}
