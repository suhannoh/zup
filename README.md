# Zup

몰라서 못 받던 혜택, 오늘 줍자.

Zup은 브랜드별 생일 혜택을 공식 출처 기준으로 정리하고, 사용자가 앱 필요 여부, 멤버십 조건, 사용 가능 기간, 무료/할인 여부를 빠르게 확인할 수 있게 돕는 검색 유입형 정보 큐레이션 서비스입니다.

## 프로젝트 정보

- 프로젝트명: Zup
- MVP: 브랜드별 생일 혜택 큐레이션
- 장기 확장: 신규 가입 혜택, 제휴 혜택, 멤버십 혜택
- 핵심 차별점: 공식 출처, 최근 확인일, 조건별 필터, 사용자 제보, 검수 이력

## 기술 스택

- Backend: Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Validation, Security, PostgreSQL, Redis, Gradle
- Frontend: Next.js, React, TypeScript, Tailwind CSS, Axios, Zustand
- Infra: Docker Compose, PostgreSQL, Redis

## 폴더 구조

```text
zup/
  backend/
  frontend/
  infra/
  scripts/
  docs/
  docker-compose.dev.yml
  docker-compose.yml
  .env.example
  .gitignore
  README.md
```

## 로컬 실행 방법

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.dev.yml up -d

cd backend
.\gradlew.bat bootRun

cd ..\frontend
npm install
npm run dev
```

Health API:

```http
GET http://localhost:8080/api/v1/health
```

Frontend:

```text
http://localhost:3000
```

## 주요 기능

- 브랜드별 생일 혜택 목록과 상세 조회
- 카테고리, 태그, 조건 기반 탐색
- 공식 출처와 최근 확인일 표시
- 사용자 정보 수정 제보
- 관리자 브랜드/혜택/출처/태그/검수 이력 관리
- SEO sitemap, robots, metadata

## 운영 데이터 입력 순서

실제 브랜드 생일 혜택 데이터는 공식 출처 확인 후 아래 순서로 입력한다.

1. 브랜드 등록
2. 혜택 등록
3. 공식 출처 등록
4. 태그 연결
5. 검수 상태 변경
6. 사용자 화면 노출 확인
7. 사용자 제보 처리

관련 문서:

- [운영 데이터 입력 가이드](docs/14-data-entry-guide.md)
- [공식 출처 검수 가이드](docs/15-official-source-verification-guide.md)
- [초기 브랜드 수집 스프레드시트 템플릿](docs/16-initial-brand-collection-template.md)
- [관리자 운영 시나리오](docs/17-admin-operation-scenario.md)

## MVP 범위

- 브랜드/혜택 데이터 관리
- 공개 목록/상세 페이지
- 카테고리/태그 페이지
- 사용자 제보 접수
- 관리자 CRUD
- 검수 상태와 VerificationLog
- sitemap/robots/metadata

## 하지 않는 것

- 사용자 회원가입/로그인
- 사용자 생일 입력 기반 개인화
- AI 추천
- WebSocket
- 알림톡/이메일 알림
- 자동 크롤링과 자동 게시
- 브랜드 로고 무단 사용
- 결제/수익화 기능
