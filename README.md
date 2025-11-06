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

## API Endpoints

### OCR

- **`POST /api/v1/ocr/receipt`**: Uploads a receipt image for OCR parsing.
  - **Request**: `multipart/form-data` with a `file` key.
  - **Success Response (200 OK)**: `ReceiptInfo` JSON object.
  - **Error Responses**:
    - `400 Bad Request`: If the file is empty or there is an error processing the file.
    - `500 Internal Server Error`: If an OCR processing error or other unexpected server error occurs.

### Settlement Calculation

- **`POST /api/v1/nppang/calculate`**: Calculates settlement manually.
  - **Request Body**: `NppangRequest` JSON object.
  - **Success Response (200 OK)**: `NppangResponse` JSON object.
  - **Error Response (400 Bad Request)**: If the input values are invalid (e.g., `totalPeople <= 0`).

### Group Management

- **`POST /api/v1/groups`**: Creates a new group.
  - **Request Body**: `{ "name": "Group Name" }`
  - **Success Response (200 OK)**: `UserGroup` JSON object.

- **`POST /api/v1/groups/{groupId}/members`**: Adds a user to a group.
  - **Request Body**: `{ "userId": 1 }`
  - **Success Response (200 OK)**: `GroupMember` JSON object.
  - **Error Response (IllegalArgumentException)**: If the group or user is not found.

- **`GET /api/v1/groups/{groupId}/members`**: Retrieves all members of a group.
  - **Success Response (200 OK)**: A list of `GroupMember` JSON objects.

- **`POST /api/v1/groups/{groupId}/calculate`**: Calculates settlement for a group.
  - **Request Body**: `NppangGroupRequest` JSON object.
  - **Success Response (200 OK)**: `NppangResponse` JSON object.

## Testing

### OCR Test Page

The application includes a simple HTML page for testing the OCR functionality directly in the browser.

- **URL**: `http://localhost:8080/api/v1/ocr-test`

## Future Improvements

- **User Management**: Implement proper user registration, login, and authentication using Spring Security.
- **Persistent Database**: Replace the H2 in-memory database with a persistent database like PostgreSQL or MySQL.
- **Firebase Integration**: The existing `FirebaseTestService` can be expanded to provide features like cloud storage for receipts or real-time database synchronization for settlements.
- **Refined Error Handling**: Implement a global exception handler (`@ControllerAdvice`) for more consistent error responses.