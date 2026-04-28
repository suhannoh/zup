# Zup

몰라서 못 받던 혜택, 오늘 줍자.

Zup은 브랜드별 생일 혜택을 공식 출처 기준으로 정리하고, 사용자가 앱 필요 여부·멤버십 조건·사용 가능 기간·무료/할인 여부로 빠르게 필터링할 수 있게 돕는 검색 유입형 정보 큐레이션 웹서비스입니다.

## 프로젝트 정보

- 프로젝트명: Zup
- MVP: 생일 혜택 큐레이션
- 장기 확장: 신규가입 혜택, 시즌 혜택, 멤버십 혜택
- 핵심 차별점: 공식 출처, 최근 확인일, 조건별 필터, 제보/검수 흐름

## 왜 만드는가

생일 쿠폰, 무료 혜택, 멤버십 조건, 사용 가능 기간은 브랜드마다 흩어져 있습니다. Zup의 1차 MVP는 블로그성 모음이 아니라 공식 출처와 최근 확인일을 기준으로 검증 가능한 생일 혜택 정보를 정리하는 것을 목표로 합니다.

Zup은 장기적으로 몰라서 못 받던 브랜드 혜택, 신규가입 혜택, 시즌 혜택, 멤버십 혜택까지 확장 가능한 브랜드입니다. 다만 현재 MVP 범위는 브랜드별 생일 혜택 큐레이션으로 유지합니다.

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

기존 `birthday_benefits` DB로 실행한 적이 있다면 Docker volume을 삭제하거나 새 DB명 `zup`으로 다시 초기화해야 합니다.

Health API:

```http
GET http://localhost:8080/api/v1/health
```

Frontend:

```text
http://localhost:3000
```

## 개발/배포 방식 차이

개발 환경에서는 PostgreSQL과 Redis만 Docker Compose로 실행하고, backend/frontend는 로컬에서 직접 실행합니다. `docker-compose.yml`은 통합 실행을 위한 골격이며 실제 운영 배포 설정은 이후 작업에서 보강합니다.

## 주요 기능

- 브랜드별 생일 혜택 목록과 상세 정보
- 조건, 카테고리, 태그 기반 탐색
- 공식 출처와 최근 확인일 표시
- 사용자 정보 수정 제보
- 관리자용 혜택 정보 관리

## MVP 범위

- 브랜드/혜택 데이터 관리
- 공개 목록/상세 페이지
- 카테고리/태그 페이지
- 제보 접수
- 관리자 CRUD
- sitemap/robots 기본 대응

## 하지 않는 것

- 사용자 회원가입/로그인
- 사용자 생일 입력 기반 개인화
- AI 추천
- WebSocket
- 알림톡
- 자동 크롤링과 자동 게시
- 브랜드 로고 무단 사용
- 결제/수익화 기능
