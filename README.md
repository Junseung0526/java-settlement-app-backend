# Java Settlement App Backend

This is a Spring Boot backend application for a settlement service, also known as "N-ppang" (더치페이) in Korea. The application provides functionalities for calculating shared expenses and parsing receipt information using Optical Character Recognition (OCR).

## Features

- **Receipt OCR**: Upload a receipt image to automatically extract the total amount, transaction date, store name, and alcohol-related expenses.
- **Settlement Calculation**: Calculates the amount per person for a group, with special consideration for those who consumed alcohol.

## Technologies Used

- Java 21
- Spring Boot 3
- Gradle
- Tesseract OCR (via Tess4J)
- Firebase Admin
- Springdoc OpenAPI (Swagger UI)
- Lombok

## How to Run

### Prerequisites

- Java 21
- Gradle
- Tesseract OCR installed and configured. The `tessdata` path needs to be configured in `application.yml`.

### Build

```bash
./gradlew build
```

### Run

```bash
java -jar build/libs/app.jar
```

The application will be available at `http://localhost:8080`.

## API Endpoints

The API documentation is available at `http://localhost:8080/swagger-ui.html` when the application is running.

### OCR

- **POST** `/api/v1/ocr/receipt`
  - Upload a receipt image to parse its content.
  - **Request**: `multipart/form-data` with a `file` parameter containing the image.
  - **Response**: `ReceiptInfo` JSON object with the parsed data.

### Settlement Calculation

- **POST** `/api/v1/nppang/calculate`
  - Calculates the amount to be paid per person.
  - **Request Body**: `NppangRequest` JSON object.
    ```json
    {
      "totalAmount": 50000,
      "alcoholAmount": 15000,
      "totalPeople": 5,
      "alcoholDrinkers": 3
    }
    ```
  - **Response**: `NppangResponse` JSON object.
    ```json
    {
      "amountPerPerson": 7000,
      "amountForAlcoholDrinker": 12000
    }
    ```
