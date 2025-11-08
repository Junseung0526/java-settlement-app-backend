# N빵(Nppang) 정산 백엔드 애플리케이션

이 프로젝트는 복잡한 정산 시나리오를 처리할 수 있는 N빵(더치페이) 백엔드 서버입니다. 그룹 관리, 정산 생성, 영수증 관리 및 고급 정산 계산 기능을 제공합니다.

## ✨ 주요 기능

- **그룹 관리**: 정산에 참여할 그룹을 생성하고 멤버를 추가, 수정, 삭제할 수 있습니다.
- **정산 관리**: 각 그룹별로 여러 정산을 생성하고 관리할 수 있습니다.
- **영수증 관리**:
    - **OCR 기반 정보 추출**: 영수증 이미지를 업로드하면 Tesseract OCR을 통해 가게 이름, 거래 날짜, 총액 등의 정보를 자동으로 분석하고 제안합니다.
    - **항목별 참여자 지정**: 하나의 영수증을 여러 개의 항목(예: "음식", "주류")으로 나눌 수 있습니다.
    - 각 항목마다 실제로 비용을 분담해야 할 **참여자**를 자유롭게 지정할 수 있어, "술 안 마신 사람"이나 "중간에 합류한 사람" 등 특수 상황에 대한 공정한 정산이 가능합니다.
    - **결제자(Payer) 지정**: 각 영수증을 누가 결제했는지 기록하여, 최종 정산 시 주고받을 금액을 정확히 계산합니다.
- **고급 정산 계산**:
    - 위 모든 정보를 종합하여, 각 멤버가 최종적으로 내야 하거나 받아야 할 금액(잔액)을 계산합니다.
    - 계산된 잔액을 바탕으로, "누가 누구에게 얼마를 보내야 하는지"에 대한 가장 간단한 형태의 송금 목록을 자동으로 생성합니다.

## 🛠️ 기술 스택

- **Backend**: Java 21, Spring Boot 3, Spring Security
- **Database**: Google Firebase Realtime Database
- **API-Docs**: SpringDoc (OpenAPI, Swagger UI)
- **OCR**: Tesseract (Tess4J)
- **Build Tool**: Gradle
- **Test Page**: Vanilla JavaScript, Bootstrap 5

## 💾 데이터베이스 구조 (Database Schema)

이 프로젝트는 Google Firebase의 실시간 데이터베이스(Realtime Database)를 사용하며, 데이터는 JSON 트리 형태로 저장됩니다. 주요 데이터 구조는 다음과 같습니다.

```json
{
  "users": {
    "{userId}": {
      "id": "string",
      "name": "string"
    }
  },
  "groups": {
    "{groupId}": {
      "id": "string",
      "name": "string",
      "members": {
        "{userId}": true
      }
    }
  },
  "settlements": {
    "{settlementId}": {
      "id": "string",
      "name": "string",
      "groupId": "string"
    }
  },
  "receipts": {
    "{receiptId}": {
      "id": "string",
      "settlementId": "string",
      "groupId": "string",
      "payerId": "string",
      "storeName": "string",
      "transactionDate": "string",
      "totalAmount": "long",
      "items": [
        {
          "name": "string",
          "price": "long",
          "participants": ["{userId1}", "{userId2}"]
        }
      ]
    }
  },
  "counters": {
    "groups": "long"
  }
}
```

## ⚙️ 실행 방법

### 1. 사전 요구사항

- **Java 21** 설치
- **Gradle** 설치
- **Tesseract OCR Engine** 설치: 백엔드 서버를 실행하는 머신에 Tesseract가 설치되어 있어야 합니다. [설치 가이드](https://tesseract-ocr.github.io/tessdoc/Installation.html)
- **Firebase 프로젝트 생성**:
    - Google Firebase에서 새 프로젝트를 생성합니다.
    - **실시간 데이터베이스(Realtime Database)**를 생성합니다.
    - 프로젝트 설정 > 서비스 계정에서 **새 비공개 키를 생성**하고, 다운로드한 `json` 파일을 프로젝트 내에 위치시킵니다. (예: `src/main/resources/`)

### 2. 환경설정

`src/main/resources/application.yml` 파일을 열어 아래 항목들을 자신의 환경에 맞게 수정합니다.

```yaml
firebase:
  database-url: "https://<YOUR-PROJECT-ID>.firebaseio.com" # 본인의 Firebase DB URL
  service-account-key-path: "classpath:<YOUR-SERVICE-ACCOUNT-KEY>.json" # 다운로드한 서비스 계정 키 파일 경로

tesseract:
  datapath: "C:/Program Files/Tesseract-OCR/tessdata" # Tesseract 학습 데이터(.traineddata)가 있는 경로
```

### 3. 빌드 및 실행

프로젝트 루트 디렉토리에서 아래 명령어를 실행합니다.

```bash
# 빌드
./gradlew build

# 실행
java -jar build/libs/app.jar

# 또는 Gradle로 바로 실행
./gradlew bootRun
```

## 🚀 API 문서 및 테스트

애플리케이션을 실행한 후, 아래 URL로 접속하여 API 문서를 확인하고 직접 테스트해볼 수 있습니다.

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

또한, 간단한 정산 흐름을 테스트할 수 있는 웹 페이지도 제공됩니다.

- **테스트 페이지 URL**: `http://localhost:8080/api/v1/ocr-test`

#### 테스트 워크플로우

1.  **그룹 생성**: `1. 그룹 관리` 패널에서 새 그룹을 생성합니다.
2.  **멤버 추가**: 생성한 그룹을 선택하고, 함께 정산할 멤버들의 이름을 추가합니다.
3.  **정산 생성**: `2. 정산 관리` 패널에서 현재 그룹에 대한 새 정산을 생성합니다.
4.  **영수증 추가**:
    1.  `3. 영수증 추가` 패널에서 영수증 이미지를 업로드하고 **OCR 분석** 버튼을 누릅니다.
    2.  자동으로 채워진 정보를 확인하고, **결제자**를 선택합니다.
    3.  **영수증 항목** 테이블에서 각 항목의 이름, 가격, 그리고 **참여자**를 정확히 선택/수정합니다. (항목 추가/삭제 가능)
    4.  **정산에 영수증 제출** 버튼을 눌러 최종 정보를 저장합니다.
    5.  필요한 만큼 영수증을 반복해서 추가합니다.
5.  **정산 계산**: `4. 정산 계산` 패널에서 **정산 계산하기** 버튼을 누릅니다.
6.  **결과 확인**: 계산된 사용자별 잔액과 추천 송금 목록을 확인합니다.

## 💡 정산 케이스 예시 (Use Cases)

새로운 정산 로직을 통해 처리할 수 있는 다양한 시나리오 예시입니다.

### Case 1: 기본적인 1/N 정산
- **상황**: 3명의 멤버(A, B, C)가 식사 후, A가 60,000원을 모두 결제.
- **입력 방법**:
    1. `payerId`를 "A"로 설정합니다.
    2. 영수증 `items`에 하나의 항목만 추가합니다.
        - `name`: "저녁 식사"
        - `price`: 60000
        - `participants`: ["A", "B", "C"]
- **계산 결과**: A, B, C 모두 20,000원씩 분담. B와 C는 A에게 20,000원씩 송금해야 합니다.

### Case 2: 술 마신 사람만 주류 비용 분담
- **상황**: 4명의 멤버(A, B, C, D)가 회식 후, B가 100,000원을 결제. 식사 비용은 70,000원, 주류 비용은 30,000원이며, C와 D는 술을 마시지 않았습니다.
- **입력 방법**:
    1. `payerId`를 "B"로 설정합니다.
    2. 영수증 `items`에 두 개의 항목을 추가합니다.
        - **항목 1**
            - `name`: "식사"
            - `price`: 70000
            - `participants`: ["A", "B", "C", "D"] (4명 모두 참여)
        - **항목 2**
            - `name`: "주류"
            - `price`: 30000
            - `participants`: ["A", "B"] (술 마신 2명만 참여)
- **계산 결과**: 
    - C, D는 식사 비용(70000 / 4 = 17,500원)만 분담합니다.
    - A, B는 식사 비용(17,500원) + 주류 비용(30000 / 2 = 15,000원) = 32,500원을 분담합니다.

### Case 3: 2차에 일부만 참여한 경우
- **상황**: 1차 회식에는 4명(A, B, C, D)이 참여하여 A가 80,000원을 결제. 2차 카페에는 2명(C, D)만 참여하여 C가 20,000원을 결제.
- **입력 방법**: **2개의 다른 영수증**으로 각각 등록합니다.
    - **영수증 1 (1차)**
        - `payerId`: "A"
        - `totalAmount`: 80000
        - `items`: [{ `name`: "1차 회식", `price`: 80000, `participants`: ["A", "B", "C", "D"] }]
    - **영수증 2 (2차)**
        - `payerId`: "C"
        - `totalAmount`: 20000
        - `items`: [{ `name`: "2차 카페", `price`: 20000, `participants`: ["C", "D"] }]
- **계산 결과**: 시스템이 모든 영수증과 항목을 종합하여 각 멤버의 최종 지출/분담액을 계산하고, 최종적으로 누가 누구에게 얼마를 보내야 할지 알려줍니다.

## 📝 API 엔드포인트

SpringDoc을 통해 생성된 **[Swagger UI](http://localhost:8080/swagger-ui/index.html)**에서 모든 API 명세를 확인하고 직접 테스트할 수 있습니다.

### Group
- `POST /api/v1/groups`: 새 그룹 생성
- `GET /api/v1/groups`: 모든 그룹 목록 조회
- `GET /api/v1/groups/{groupId}`: 특정 그룹 정보 조회
- `PUT /api/v1/groups/{groupId}`: 특정 그룹 정보 수정
- `DELETE /api/v1/groups/{groupId}`: 특정 그룹 삭제
- `POST /api/v1/groups/{groupId}/members`: 그룹에 멤버 추가
- `GET /api/v1/groups/{groupId}/members`: 특정 그룹의 모든 멤버 조회
- `DELETE /api/v1/groups/{groupId}/members/{userId}`: 특정 그룹의 멤버 삭제

### Settlement
- `POST /api/v1/settlements`: 새 정산 생성
- `GET /api/v1/settlements/{settlementId}`: 특정 정산 정보 조회 (포함된 영수증 포함)
- `POST /api/v1/settlements/{settlementId}/receipts`: 정산에 영수증 추가
- `POST /api/v1/settlements/{settlementId}/calculate`: 정산 계산 실행

### OCR
- `POST /api/v1/ocr/parse`: 영수증 이미지를 분석하여 DTO 형태로 반환 (multipart/form-data)
