# 04. API 명세 초안

## 1. 공통 응답 형식

모든 API는 아래 형식을 기본으로 한다.

```json
{
  "success": true,
  "data": {},
  "message": "ok"
}
```

에러 응답은 아래 형식을 따른다.

```json
{
  "success": false,
  "data": null,
  "message": "요청이 올바르지 않습니다."
}
```

## 2. Public API

### Health

```http
GET /api/v1/health
```

응답:

```json
{
  "success": true,
  "data": {
    "status": "OK"
  },
  "message": "healthy"
}
```

---

### 카테고리 목록

```http
GET /api/v1/categories
```

응답 데이터 예시:

```json
[
  {
    "id": 1,
    "name": "카페",
    "slug": "cafe",
    "displayOrder": 1
  }
]
```

---

### 태그 목록

```http
GET /api/v1/tags
```

응답 데이터 예시:

```json
[
  {
    "id": 1,
    "name": "무료",
    "slug": "free",
    "displayOrder": 1
  }
]
```

---

### 브랜드 목록

```http
GET /api/v1/brands
```

Query parameters:

- keyword
- category
- tag
- benefitType
- birthdayTimingType
- requiredApp
- requiredMembership
- sort
- page
- size

sort:

- popular
- recent
- name
- updated

응답 데이터 예시:

```json
{
  "items": [
    {
      "id": 1,
      "name": "스타벅스",
      "slug": "starbucks",
      "categoryName": "카페",
      "summary": "생일 무료 음료 쿠폰",
      "tags": ["무료", "앱 필요", "생일월"],
      "lastVerifiedAt": "2026-04-01"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 브랜드 상세

```http
GET /api/v1/brands/{slug}
```

응답 데이터 예시:

```json
{
  "id": 1,
  "name": "스타벅스",
  "slug": "starbucks",
  "description": "스타벅스 리워드 생일 혜택",
  "category": {
    "id": 1,
    "name": "카페",
    "slug": "cafe"
  },
  "benefits": [
    {
      "id": 10,
      "title": "생일 무료 음료 쿠폰",
      "summary": "생일월에 발급되는 무료 음료 쿠폰",
      "benefitType": "FREE_ITEM",
      "birthdayTimingType": "BIRTHDAY_MONTH",
      "requiredApp": true,
      "requiredMembership": true,
      "requiredPurchase": false,
      "conditionSummary": "회원 등급 및 발급 조건 확인 필요",
      "usagePeriodDescription": "공식 앱에서 발급 및 사용 기간 확인",
      "lastVerifiedAt": "2026-04-01",
      "sources": [
        {
          "sourceType": "OFFICIAL_MEMBERSHIP",
          "sourceUrl": "https://example.com",
          "sourceTitle": "공식 멤버십 안내"
        }
      ],
      "tags": ["무료", "앱 필요", "멤버십 필요", "생일월"]
    }
  ]
}
```

---

### 카테고리별 혜택

```http
GET /api/v1/categories/{slug}/benefits
```

Query parameters:

- tag
- sort
- page
- size

---

### 태그별 혜택

```http
GET /api/v1/tags/{slug}/benefits
```

Query parameters:

- category
- sort
- page
- size

---

### 정보 수정 제보

```http
POST /api/v1/reports
```

Request:

```json
{
  "brandId": 1,
  "benefitId": 10,
  "reportType": "WRONG_INFO",
  "content": "혜택 조건이 바뀐 것 같습니다.",
  "referenceUrl": "https://example.com",
  "email": "optional@example.com"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "reportId": 1
  },
  "message": "제보가 접수되었습니다."
}
```

---

### 조회수 기록

```http
POST /api/v1/page-views
```

Request:

```json
{
  "targetType": "BRAND",
  "targetId": 1
}
```

MVP에서는 중복 방지 고도화 없이 일 단위 집계만 한다.

## 3. Admin API

관리자 인증은 별도 작업에서 구현한다.  
초기 스켈레톤에서는 permitAll 상태일 수 있다.

### 관리자 로그인

```http
POST /api/v1/admin/auth/login
```

Request:

```json
{
  "email": "admin@example.com",
  "password": "password"
}
```

---

### 브랜드 관리

```http
GET /api/v1/admin/brands
POST /api/v1/admin/brands
PATCH /api/v1/admin/brands/{id}
PATCH /api/v1/admin/brands/{id}/status
```

---

### 혜택 관리

```http
GET /api/v1/admin/benefits
POST /api/v1/admin/benefits
PATCH /api/v1/admin/benefits/{id}
PATCH /api/v1/admin/benefits/{id}/status
```

혜택 상태 변경 시 VerificationLog를 생성한다.

---

### 출처 관리

```http
POST /api/v1/admin/benefits/{benefitId}/sources
PATCH /api/v1/admin/sources/{sourceId}
DELETE /api/v1/admin/sources/{sourceId}
```

---

### 태그 연결

```http
PUT /api/v1/admin/benefits/{benefitId}/tags
```

Request:

```json
{
  "tagIds": [1, 2, 3]
}
```

---

### 제보 관리

```http
GET /api/v1/admin/reports
PATCH /api/v1/admin/reports/{id}/status
```

---

### 검수 필요 목록

```http
GET /api/v1/admin/review-needed
```

조회 기준:

- verificationStatus = NEEDS_CHECK
- 사용자 제보가 있는 혜택
- 공식 출처 변경 감지된 혜택

---

### 오래된 데이터 목록

```http
GET /api/v1/admin/stale-benefits
```

기준:

- lastVerifiedAt이 90일 이상 지난 혜택

---

### 관리자 대시보드

```http
GET /api/v1/admin/dashboard
```

응답 데이터 예시:

```json
{
  "publishedBenefitCount": 50,
  "needsCheckBenefitCount": 5,
  "staleBenefitCount": 10,
  "receivedReportCount": 3,
  "topBrands": []
}
```

## 4. API 구현 우선순위

### 1차

- Health
- Categories
- Tags
- Brands list/detail
- Reports create
- Admin Brand CRUD
- Admin Benefit CRUD

### 2차

- Admin reports
- Review-needed
- Stale benefits
- Page views
- Dashboard

### 3차

- Auth
- Source hash change detection
- Popular ranking
