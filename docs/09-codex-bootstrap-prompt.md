# 09. Codex 작업 지시서 01 — 프로젝트 기본 구조 생성

## 목표

`zup` 프로젝트의 기본 모노레포 구조를 생성한다.

이 프로젝트는 브랜드별 생일 혜택 정보를 공식 출처 기준으로 정리하고, 사용자가 조건별로 빠르게 필터링해서 볼 수 있는 검색 유입형 정보 큐레이션 웹서비스다.

이번 작업에서는 상세 기능을 구현하지 않는다.

이번 작업의 목표는 아래까지만 안정적으로 만드는 것이다.

- 루트 모노레포 구조
- Spring Boot 백엔드 기본 구조
- Next.js 프론트엔드 기본 구조
- PostgreSQL / Redis 개발용 Docker Compose
- 공통 응답 / 공통 예외 / Health API
- 프론트 기본 라우팅 placeholder
- README / .env.example / .gitignore

---

## 프로젝트명

루트 프로젝트 폴더명:

```text
zup
```

한글 서비스명:

```text
줍
```

서비스 카피:

```text
몰라서 못 받던 혜택, 오늘 줍자
```

Zup은 장기적으로 신규가입 혜택, 시즌 혜택, 멤버십 혜택까지 확장 가능한 혜택 큐레이션 브랜드다. 단, 이 작업과 1차 MVP 범위는 브랜드별 생일 혜택 큐레이션으로 유지한다.

백엔드 패키지명:

```text
com.noh.zup
```

---

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- PostgreSQL Driver
- Spring Data Redis
- Lombok 선택 가능

### Frontend

- Next.js
- React
- TypeScript
- Tailwind CSS
- Axios
- Zustand

주의:

이 프로젝트는 SEO가 중요하므로 React + Vite가 아니라 Next.js를 사용한다.

### Infra

- Docker Compose
- PostgreSQL
- Redis

---

## 생성할 루트 구조

```text
zup/
├── backend/
├── frontend/
├── infra/
├── docs/
├── scripts/
├── docker-compose.dev.yml
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

주의:

- 이미 `docs/` 폴더가 존재한다면 삭제하거나 덮어쓰지 말 것.
- 기존 문서 파일은 보존할 것.
- backend/frontend는 새로 생성한다.
- 불필요한 샘플 게시판, Todo, 예제 기능은 만들지 말 것.

---

# Backend 요구사항

## 1. Spring Boot 프로젝트 생성

`backend` 폴더에 Spring Boot 프로젝트를 생성한다.

패키지명:

```text
com.noh.zup
```

필수 의존성:

- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- PostgreSQL Driver
- Spring Data Redis
- Lombok 선택 가능

## 2. 기본 패키지 구조

```text
backend/src/main/java/com/noh/zup/
├── ZupApplication.java
├── common/
│   ├── response/
│   ├── exception/
│   └── config/
├── health/
├── domain/
│   ├── category/
│   ├── brand/
│   ├── benefit/
│   ├── source/
│   ├── tag/
│   ├── report/
│   ├── verification/
│   ├── pageview/
│   └── admin/
└── global/
```

이번 작업에서는 도메인 엔티티를 자세히 구현하지 않는다.

## 3. 공통 응답 객체

`ApiResponse<T>`를 만든다.

필드:

```text
success
data
message
```

성공 응답용 정적 메서드를 제공한다.

```text
ApiResponse.success(data, message)
```

## 4. 공통 예외 구조

기본 예외 구조를 만든다.

필수 구성:

```text
BusinessException
ErrorCode
GlobalExceptionHandler
```

`ErrorCode`에는 최소한 아래 정도만 둔다.

```text
INTERNAL_SERVER_ERROR
INVALID_REQUEST
NOT_FOUND
UNAUTHORIZED
FORBIDDEN
```

## 5. Health API 구현

Endpoint:

```text
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

## 6. CORS 설정

개발 환경 기준 허용 origin:

```text
http://localhost:3000
```

## 7. Security 기본 설정

이번 단계에서는 인증 기능을 구현하지 않는다.

요구사항:

- CSRF 비활성화
- CORS 활성화
- `/api/v1/**` permitAll
- 세션 stateless 설정
- 추후 관리자 인증을 붙일 수 있도록 SecurityConfig 분리

## 8. PostgreSQL / Redis 연결 설정

`application.yml`을 작성한다.

환경 변수 기반으로 설정한다.

필요한 환경 변수 예시:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
CORS_ALLOWED_ORIGINS
```

## 9. Backend 테스트

기본 contextLoads 테스트 또는 HealthController 테스트를 하나 작성한다.

빌드가 성공해야 한다.

```bash
cd backend
./gradlew build
```

Windows 기준:

```powershell
cd backend
.\gradlew.bat build
```

---

# Frontend 요구사항

## 1. Next.js 프로젝트 생성

`frontend` 폴더에 Next.js + TypeScript + Tailwind CSS 프로젝트를 생성한다.

필수 설치:

```text
axios
zustand
```

## 2. 기본 구조

```text
frontend/src/
├── app/
│   ├── page.tsx
│   ├── brands/
│   │   ├── page.tsx
│   │   └── [slug]/
│   │       └── page.tsx
│   ├── categories/
│   │   └── [slug]/
│   │       └── page.tsx
│   ├── tags/
│   │   └── [slug]/
│   │       └── page.tsx
│   ├── reports/
│   │   └── new/
│   │       └── page.tsx
│   └── admin/
│       ├── page.tsx
│       ├── brands/
│       │   └── page.tsx
│       ├── benefits/
│       │   └── page.tsx
│       └── reports/
│           └── page.tsx
├── components/
│   ├── layout/
│   ├── common/
│   └── benefit/
├── lib/
│   └── api/
├── stores/
├── types/
└── styles/
```

## 3. 기본 라우트

아래 라우트를 placeholder 화면으로 만든다.

```text
/
 /brands
 /brands/[slug]
 /categories/[slug]
 /tags/[slug]
 /reports/new
 /admin
 /admin/brands
 /admin/benefits
 /admin/reports
```

## 4. 기본 레이아웃

필수 구성:

- Header
- Footer
- Main container

Header 링크:

```text
홈
브랜드
카테고리
제보하기
관리자
```

## 5. Axios API 클라이언트

파일 예시:

```text
src/lib/api/client.ts
```

환경 변수:

```text
NEXT_PUBLIC_API_BASE_URL
```

기본값:

```text
http://localhost:8080
```

Health API 호출 함수도 만든다.

```text
getHealth()
```

## 6. Zustand store 기본 구조

예:

```text
src/stores/useUiStore.ts
```

필드 예시:

```text
isMobileMenuOpen
openMobileMenu
closeMobileMenu
```

## 7. SEO 기본 설정

Next.js metadata 기본값을 설정한다.

메인 페이지 metadata 예시:

```text
title:
Zup - 브랜드 생일 쿠폰과 무료 혜택 모음

description:
카페, 외식, 영화관, 뷰티 브랜드의 생일 쿠폰과 무료 혜택을 조건별로 확인하세요. 앱 필요 여부, 멤버십 조건, 사용 기간, 공식 출처를 함께 정리합니다.
```

---

# Docker 요구사항

## docker-compose.dev.yml

서비스:

```text
postgres
redis
```

개발 시에는 postgres/redis만 Docker로 실행하고, backend는 IntelliJ 또는 Gradle bootRun, frontend는 npm run dev로 실행할 수 있도록 한다.

PostgreSQL 기본값 예시:

```text
POSTGRES_DB=zup
POSTGRES_USER=zup
POSTGRES_PASSWORD=zup
```

Redis:

```text
6379:6379
```

## docker-compose.yml

운영/통합 실행용 docker-compose 파일을 만든다.

서비스:

```text
postgres
redis
backend
frontend
```

단, backend/frontend Dockerfile은 이번 작업에서 만들 수 있으면 만들고, 시간이 부족하면 TODO로 남겨도 된다.

---

# .env.example 요구사항

루트에 `.env.example`을 만든다.

```env
# App
APP_NAME=zup
APP_TIMEZONE=Asia/Seoul

# Backend
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080

# Database
POSTGRES_DB=zup
POSTGRES_USER=zup
POSTGRES_PASSWORD=zup
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zup
SPRING_DATASOURCE_USERNAME=zup
SPRING_DATASOURCE_PASSWORD=zup

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Frontend
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

# .gitignore 요구사항

포함해야 할 예:

```text
.env
.env.local
.env.*.local
backend/build/
backend/.gradle/
frontend/.next/
frontend/node_modules/
.idea/
.vscode/
.DS_Store
```

---

# README 요구사항

README에는 아래 내용을 포함한다.

```text
프로젝트 소개
왜 만들었는가
기술 스택
로컬 실행 방법
개발/배포 방식 차이
주요 기능
폴더 구조
MVP 범위
하지 않는 것
```

---

# 완료 조건

작업 완료 후 아래를 보고한다.

1. 생성한 파일 목록
2. 구현한 Backend 구조
3. 구현한 Frontend 구조
4. 구현한 API
5. 로컬 실행 방법
6. 빌드/테스트 결과
7. 다음 작업 TODO

---

# 중요 주의사항

- 생일 혜택 정보를 자동 크롤링하지 마라.
- 실제 브랜드 로고를 무단 사용하지 마라.
- 사용자가 로그인해야 볼 수 있는 구조로 만들지 마라.
- AI 추천, WebSocket, 알림톡은 이번 작업에 넣지 마라.
- 불필요한 샘플 기능을 만들지 마라.
- 우선순위는 기본 구조, 공통 응답, 공통 예외, Health API, 프론트 라우팅이다.
