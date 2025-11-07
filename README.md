# Java Settlement App Backend

## Overview

This is a Spring Boot backend application for a settlement service, also known as "N-ppang" (더치페이) in Korea. The application provides functionalities for calculating shared expenses, parsing receipt information using Optical Character Recognition (OCR), and managing settlement groups.

## Features

- **Receipt OCR**: Upload a receipt image (`.png`, `.jpg`, etc.) to automatically parse its content. The service extracts:
  - Total amount
  - Transaction date
  - Store name
  - Alcohol-related expenses
- **Settlement Calculation**:
  - **Manual Mode**: Manually input the total amount, alcohol amount, total people, and number of alcohol drinkers to calculate the settlement.
  - **Group Mode**: Create groups, add members, and automatically calculate the settlement for all members of a group.
- **Group Management**: Basic functionality to create groups and add members.

## Technologies Used

- **Framework**: Spring Boot 3
- **Language**: Java 21
- **Build Tool**: Gradle
- **Database**: H2 In-memory Database
- **ORM**: Spring Data JPA (Hibernate)
- **API Documentation**: Springdoc OpenAPI (Swagger UI)
- **Security**: Spring Security
- **OCR**: Tesseract (via Tess4J)
- **Firebase**: Firebase Admin SDK for potential future integrations (currently includes a test service).
- **Templating**: Thymeleaf (for OCR test page)
- **Utilities**: Lombok, Commons IO

## Project Setup

### Prerequisites

- Java 21
- Gradle
- Tesseract OCR
  - Tesseract needs to be installed on the system where the application is running.
  - The path to the `tessdata` directory must be configured in `src/main/resources/application.yml`.

### Installation & Run

1.  **Build the project**:

    ```bash
    ./gradlew build
    ```

2.  **Run the application**:

    ```bash
    java -jar build/libs/app.jar
    ```

- The application will be available at `http://localhost:8080`.
- The Swagger UI API documentation is available at `http://localhost:8080/swagger-ui.html`.

## Database

The project uses an H2 in-memory database. The database is reset every time the application restarts.

### H2 Console

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb` (use this value to connect)
- **Username**: `sa`
- **Password**: (leave blank)

### Creating Test Data

To use the group features, you need to create users and groups first. You can do this via the H2 console.

1.  **Create a user**:

    ```sql
    INSERT INTO APP_USER (username, password) VALUES ('testuser', 'password');
    ```

2.  **Create a group**:

    ```sql
    INSERT INTO USER_GROUP (name) VALUES ('testgroup');
    ```

## 주요 기능

- **그룹 관리**: 정산을 함께할 사용자를 그룹으로 묶어 관리할 수 있습니다.
- **정산 관리 (영수증 다중 처리)**:
  - 여러 개의 영수증을 하나의 '정산' 단위로 묶어 관리합니다.
  - 영수증 이미지를 업로드하면 OCR 기술로 결제 내역(총액, 주류 금액 등)을 자동 추출하여 정산에 추가합니다.
  - 정산에 포함된 모든 영수증의 합계 금액과 개별 내역을 조회할 수 있습니다.
- **N빵 계산**: 누적된 정산 금액을 바탕으로 그룹 멤버 수, 주류 음주자 수를 고려하여 1인당 부담해야 할 금액을 계산합니다.

## API Endpoints

새로운 정산 중심의 API 엔드포인트입니다.

### 1. 그룹 관리 (`/api/v1/groups`)

- **`POST /`**: 새로운 그룹을 생성합니다.
  - **Request Body**: `{ "name": "새 그룹 이름" }`
- **`GET /`**: 모든 그룹 목록을 조회합니다.
- **`POST /{groupId}/members`**: 특정 그룹에 사용자를 멤버로 추가합니다.
  - **Request Body**: `{ "userId": 1 }`
- **`GET /{groupId}/members`**: 특정 그룹에 속한 모든 멤버 목록을 조회합니다.

### 2. 정산 관리 (`/api/v1/settlements`)

- **`POST /`**: 새로운 정산을 시작합니다. 그룹과 연동하려면 `groupId`를 포함할 수 있습니다.
  - **Request Body**: `{ "settlementName": "오늘의 정산", "groupId": 1 }`
- **`POST /{settlementId}/receipts`**: 특정 정산에 영수증 이미지를 추가하고 OCR로 분석합니다.
  - **Request**: `multipart/form-data` 형식, `file` 키로 이미지 파일 전송
- **`GET /{settlementId}`**: 특정 정산의 상세 내역(누적 금액, 포함된 영수증 목록 등)을 조회합니다.

### 3. 정산 계산

- **`POST /api/v1/settlements/{settlementId}/calculate`**: 최종 정산을 계산합니다.
  - **Request Body**: `{ "alcoholDrinkers": 2 }` (주류 음주자 수)
  - **Response**: 1인당 부담 금액, 주류 음주자 부담 금액을 포함한 결과

- **`POST /api/v1/nppang/calculate`**: (레거시) 수동으로 금액과 인원을 입력하여 간단히 계산합니다.

## Testing

### 기능 테스트 페이지

모든 API 기능을 브라우저에서 직접 테스트해볼 수 있는 페이지입니다.

- **URL**: `http://localhost:8080/api/v1/ocr-test`


## Future Improvements

- **User Management**: Implement proper user registration, login, and authentication using Spring Security.
- **Persistent Database**: Replace the H2 in-memory database with a persistent database like PostgreSQL or MySQL.
- **Firebase Integration**: The existing `FirebaseTestService` can be expanded to provide features like cloud storage for receipts or real-time database synchronization for settlements.
- **Refined Error Handling**: Implement a global exception handler (`@ControllerAdvice`) for more consistent error responses.