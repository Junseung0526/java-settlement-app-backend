# 1. 빌드 스테이지
FROM gradle:8.8-jdk21-jammy AS builder

WORKDIR /app

COPY build.gradle settings.gradle /app/
COPY gradlew /app/
COPY gradle /app/gradle

# gradlew 실행 권한 부여 (Permission denied 해결)
RUN chmod +x gradlew

RUN ./gradlew dependencies

COPY src /app/src

RUN ./gradlew bootJar

# 2. 실행 스테이지
FROM eclipse-temurin:21-jre-alpine

ENV SERVER_PORT=8080

# Tesseract 및 한국어 데이터 설치
RUN apk update && \
    apk add --no-cache tesseract-ocr tesseract-ocr-data-kor && \
    rm -rf /var/cache/apk/*

ENV TESSDATA_PREFIX=/usr/share/tessdata

ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=builder ${JAR_FILE} app.jar

# 프로젝트 내부의 kor.traineddata 복사
# Alpine 패키지에 파일이 있다면 덮어쓰거나, 없는 경우 추가됩니다.
COPY --from=builder /app/src/main/resources/kor.traineddata ${TESSDATA_PREFIX}/kor.traineddata

ENTRYPOINT ["java", "-Dserver.port=${SERVER_PORT}", "-jar", "/app.jar"]
