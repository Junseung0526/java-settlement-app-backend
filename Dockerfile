FROM gradle:8.8-jdk21-jammy AS builder

WORKDIR /app

COPY build.gradle settings.gradle /app/
COPY gradlew /app/
COPY gradle /app/gradle

RUN chmod +x gradlew

RUN ./gradlew dependencies

COPY src /app/src

RUN ./gradlew bootJar

FROM eclipse-temurin:21-jre-alpine

ENV SERVER_PORT=8080

RUN apk update && \
    apk add --no-cache tesseract-ocr tesseract-ocr-data-kor && \
    rm -rf /var/cache/apk/*

ENV TESSDATA_PREFIX=/usr/share/tessdata

ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=builder ${JAR_FILE} app.jar

COPY --from=builder /app/src/main/resources/kor.traineddata ${TESSDATA_PREFIX}/kor.traineddata

ENTRYPOINT ["java", "-Dserver.port=${SERVER_PORT}", "-jar", "/app.jar"]
