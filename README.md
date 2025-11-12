# N빵(Nppang) 정산 백엔드 애플리케이션

이 프로젝트는 복잡한 정산 시나리오를 처리할 수 있는 N빵(더치페이) 백엔드 서버입니다. 사용자 및 그룹 관리, OCR을 포함한 유연한 영수증 관리, 그리고 선택적 영수증에 기반한 고급 정산 계산 기능을 제공합니다.

## ✨ 주요 기능

- **사용자 관리**: 사용자를 생성하고 이름을 변경할 수 있습니다.
- **그룹 관리**: 정산에 참여할 그룹을 생성하고 멤버를 추가, 수정, 삭제할 수 있습니다.
- **유연한 영수증 관리**:
    - **그룹 중심 관리**: 영수증을 '정산'이 아닌 '그룹'에 직접 추가하여 관리합니다. 이를 통해 특정 이벤트(예: 여행, 회식)와 관련된 모든 비용을 먼저 등록하고, 나중에 원하는 영수증만 선택하여 정산할 수 있습니다.
    - **다양한 입력 방식**:
        - **수동 입력**: 영수증 정보를 직접 입력합니다.
        - **파일 OCR**: 영수증 이미지 파일을 업로드하여 정보를 자동으로 추출합니다.
        - **카메라 OCR**: 디바이스의 카메라로 직접 영수증을 촬영하여 정보를 추출합니다.
    - **항목별 참여자 지정**: 하나의 영수증을 여러 항목으로 나누고, 각 항목마다 비용을 분담할 참여자를 자유롭게 지정할 수 있습니다. (예: 술 안 마신 사람 제외)
- **고급 정산 계산**:
    - 그룹에 등록된 여러 영수증 중 정산을 원하는 것들만 **선택**하여 계산을 실행합니다.
    - 각 멤버가 최종적으로 내야 하거나 받아야 할 금액을 계산하고, 가장 간단한 형태의 송금 목록을 생성합니다.

## 🛠️ 기술 스택

- **Backend**: Java 21, Spring Boot 3, Spring Security
- **Database**: Google Firebase Realtime Database
- **API-Docs**: SpringDoc (OpenAPI, Swagger UI)
- **OCR**: Tesseract (Tess4J)
- **Build Tool**: Gradle
- **Test Page**: Vanilla JavaScript, Bootstrap 5

## 💾 데이터베이스 구조 (Database Schema)

Firebase 실시간 데이터베이스를 사용하며, 데이터는 JSON 트리 형태로 저장됩니다.

```json
{
  "users": { "{userId}": { "id": "string", "username": "string" } },
  "groups": { "{groupId}": { "id": "string", "name": "string", "members": { "{userId}": true } } },
  "settlements": { "{settlementId}": { "id": "string", "name": "string", "groupId": "string" } },
  "receipts": {
    "{receiptId}": {
      "id": "string",
      "groupId": "string",
      "settlementId": "string | null", // 정산 전에는 null, 정산 완료 후 ID가 채워짐
      "payerId": "string",
      "storeName": "string",
      "transactionDate": "string",
      "totalAmount": "long",
      "items": [
        { "name": "string", "price": "long", "participants": ["{userId1}", "{userId2}"] }
      ]
    }
  }
}
```
**주요 변경사항**: `receipts`의 `settlementId`는 처음 생성 시 `null`이며, 해당 영수증이 특정 정산 계산에 포함된 후에 ID가 채워집니다.

## ⚙️ 실행 방법

### 1. 사전 요구사항

- **Java 21** 설치
- **Gradle** 설치
- **Tesseract OCR Engine** 설치: [설치 가이드](https://tesseract-ocr.github.io/tessdoc/Installation.html)
- **Firebase 프로젝트 생성** 및 서비스 계정 키(`json` 파일) 발급

### 2. 환경설정

`src/main/resources/application.yml` 파일을 열어 자신의 환경에 맞게 수정합니다.

```yaml
firebase:
  database-url: "https://<YOUR-PROJECT-ID>.firebaseio.com"
  service-account-key-path: "classpath:<YOUR-SERVICE-ACCOUNT-KEY>.json"

tesseract:
  datapath: "C:/Program Files/Tesseract-OCR/tessdata" # Tesseract 학습 데이터 경로
```

### 3. 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
java -jar build/libs/app.jar
```

## 🚀 통합 테스트 페이지

애플리케이션을 실행한 후, 아래 URL로 접속하여 모든 기능을 테스트할 수 있는 웹 페이지를 사용할 수 있습니다.

- **테스트 페이지 URL**: `http://localhost:8080/index.html`
- **API 문서 (Swagger UI)**: `http://localhost:8080/swagger-ui/index.html`

#### 테스트 워크플로우

1.  **사용자 생성**: `1. 사용자 관리` 패널에서 정산에 참여할 사용자들을 미리 생성합니다.
2.  **그룹 생성 및 멤버 추가**:
    - `2. 그룹 관리` 패널에서 새 그룹을 생성합니다.
    - 생성한 그룹을 선택하고, `1. 사용자 관리`에서 만든 사용자들을 멤버로 추가합니다.
3.  **영수증 추가**:
    - `3. 영수증 관리` 패널에서 영수증을 추가할 그룹을 선택합니다.
    - **수동/파일/카메라** OCR 탭 중 하나를 선택하여 영수증 정보를 입력하고 '영수증 제출' 버튼을 누릅니다.
    - 필요한 만큼 영수증을 반복해서 추가합니다.
4.  **정산 생성 및 계산**:
    - `4. 정산 관리` 패널에서 정산할 그룹을 선택합니다.
    - '새 정산 이름'을 입력하고 '정산 생성' 버튼을 눌러 정산 이벤트를 만듭니다.
    - '정산에 포함할 영수증 선택' 목록에서 이번 계산에 포함할 영수증들을 체크합니다.
    - **'선택한 영수증으로 정산 계산'** 버튼을 누릅니다.
5.  **결과 확인**: '정산 결과' 섹션에서 사용자별 정산 금액과 최종 송금 목록을 확인합니다.

## 📝 API 엔드포인트

### User
- `POST /api/v1/users`: 새 사용자 생성 (또는 이름이 같으면 기존 사용자 반환)
- `GET /api/v1/users`: 모든 사용자 목록 조회
- `GET /api/v1/users/{userId}`: 특정 사용자 정보 조회
- `PUT /api/v1/users/{userId}`: 특정 사용자 이름 변경

### Group
- `POST /api/v1/groups`: 새 그룹 생성
- `GET /api/v1/groups`: 모든 그룹 목록 조회
- `GET /api/v1/groups/{groupId}`: 특정 그룹 정보 조회
- `PUT /api/v1/groups/{groupId}`: 특정 그룹 정보 수정
- `DELETE /api/v1/groups/{groupId}`: 특정 그룹 삭제
- `POST /api/v1/groups/{groupId}/members`: 그룹에 멤버 추가
- `GET /api/v1/groups/{groupId}/members`: 특정 그룹의 모든 멤버 조회
- `DELETE /api/v1/groups/{groupId}/members/{userId}`: 특정 그룹의 멤버 삭제

### Receipt
- `POST /api/v1/receipts`: 새 영수증을 특정 그룹에 추가
- `GET /api/v1/receipts/{receiptId}`: 특정 영수증 정보 조회
- `GET /api/v1/groups/{groupId}/receipts`: 특정 그룹에 속한 모든 영수증 조회

### Settlement
- `POST /api/v1/settlements`: 새 정산 이벤트 생성
- `GET /api/v1/settlements/{settlementId}`: 특정 정산 정보와 포함된 영수증 목록 조회
- `POST /api/v1/settlements/{settlementId}/calculate`: 특정 정산에 대해, 요청된 영수증 목록(`receiptIds`)을 바탕으로 계산 실행

### OCR
- `POST /api/v1/ocr/parse`: 영수증 이미지를 분석하여 DTO 형태로 반환 (multipart/form-data)
