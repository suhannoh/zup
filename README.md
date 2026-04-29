# Zup

## 공식 출처 자동 수집 E2E 검증

공식 출처 자동 수집 기능은 외부 사이트에 바로 요청하기 전에 로컬 fixture HTML로 검증할 수 있습니다.

자세한 절차는 [공식 출처 자동 수집 로컬 E2E 검증 가이드](docs/24-official-source-collection-e2e-guide.md)를 참고하세요.

PowerShell smoke test:

```powershell
.\scripts\smoke-test-collection-local.ps1
```

자동 수집 스케줄러는 기본 비활성화되어 있습니다. 운영자가 `COLLECTION_SCHEDULER_ENABLED=true`를 명시한 경우에만 `nextFetchAt` 기준 due SourceWatch를 Redis lock으로 중복 방지하며 수집합니다. 외부 사이트 대상 운영 전에는 robots.txt와 이용 조건, 요청 빈도 정책을 확인해야 합니다.

수집 실행 결과는 관리자 화면 `/admin/collection-runs`에서 확인할 수 있습니다. 수동 수집과 스케줄러 수집을 구분하며, `sameAsPrevious=true`는 실패가 아닌 정상 성공으로 기록됩니다.

관리자 대시보드 `/admin`에서는 SourceWatch 수, 활성 SourceWatch 수, 검수 대기 Candidate 수, 최근 24시간 수집 성공/실패/스킵 수와 최근 실패 SourceWatch를 요약해서 확인할 수 있습니다. 운영자는 실패 항목을 확인한 뒤 URL 수정 또는 비활성화를 검토합니다.

실제 공식 출처 URL을 SourceWatch에 등록하기 전에는 [실데이터 SourceWatch 등록 정책](docs/25-real-sourcewatch-registration-policy.md) 기준을 확인해야 합니다. 초기 운영 검증 단계에서는 스케줄러를 켜기 전에 3~5개 공식 URL만 수동 collect로 검증합니다.

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
- 관리자 JWT 인증 및 Admin API 보호
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

## MVP E2E 점검

로컬에서 백엔드와 프론트를 실행한 뒤 아래 문서에 따라 전체 흐름을 확인할 수 있습니다.

- [MVP E2E 체크리스트](docs/18-mvp-e2e-checklist.md)
- [로컬 Smoke Test 가이드](docs/19-local-smoke-test-guide.md)

PowerShell smoke test:

```powershell
.\scripts\smoke-test-local.ps1
```

백엔드 포트가 다르면 `-BaseUrl`로 지정합니다.

```powershell
.\scripts\smoke-test-local.ps1 -BaseUrl "http://localhost:18081"
```

## MVP 범위

- 브랜드/혜택 데이터 관리
- 공개 목록/상세 페이지
- 카테고리/태그 페이지
- 사용자 제보 접수
- 관리자 CRUD
- 관리자 인증
- 검수 상태와 VerificationLog
- sitemap/robots/metadata
- 로컬 E2E smoke test

## 하지 않는 것

- 일반 사용자 회원가입/로그인
- 사용자 생일 입력 기반 개인화
- AI 추천
- WebSocket
- 알림톡/이메일 알림
- 자동 크롤링과 자동 게시
- 브랜드 로고 무단 사용
- 결제/수익화 기능

## 운영 정책

Zup은 브랜드 공식 혜택 정보를 최소 요약해 제공하는 독립 정보 서비스입니다. 브랜드와 공식 제휴 관계가 아니며, 쿠폰을 직접 발급하거나 판매하지 않습니다.

- robots.txt 차단 사이트는 자동 수집하지 않습니다.
- robots.txt 허용은 충분조건이 아닌 최소조건입니다.
- 약관 확인(`termsCheckStatus`)은 관리자가 수동으로 확인하고 업데이트합니다.
- 불확실한 경우(`UNKNOWN_NEEDS_REVIEW`) 자동 수집을 허용하지 않습니다.
- public에는 브랜드 로고, 쿠폰 이미지, 배너 이미지, 외부 이미지 URL을 사용하지 않습니다.
- 공식 출처 URL과 최종 확인일을 표시하고, 사용 전 공식 앱/홈페이지에서 최종 확인하도록 안내합니다.
- robots-blocked 사이트의 기존 snapshot은 실제 삭제 전 `expiresAt` 30일 기준으로 정리 대상으로 관리합니다.

### CJ ONE 처리 정책

CJ ONE 생일축하쿠폰 안내 카드는 robots.txt에서 일반 봇(`User-agent: *`)에 대해 `Disallow: /` 정책이 확인되었으므로 Zup의 자동 수집 대상에서 제외합니다. SourceWatch는 삭제하지 않고 공식 출처 URL을 수동 검수 근거로 유지합니다.

해당 브랜드의 혜택 정보는 관리자가 공식 페이지를 직접 확인한 뒤 수동으로 등록/수정하며, public 화면에는 공식 출처 URL과 최종 확인일을 함께 표시합니다. 기존 스냅샷 기반 후보 재생성도 robots 차단 출처에서는 허용하지 않습니다.

### 추출 파이프라인

혜택 후보 추출은 OpenAI API 또는 외부 LLM API 없이 규칙 기반으로 동작합니다. HTML 전체 textContent를 그대로 분석하지 않고 DOM 블록 단위로 나눈 뒤, 생일 혜택 문맥과 일반 리워드/등급/푸터 문맥을 분리합니다.

후보에는 신뢰도, 경고, 생일 문맥 근거, 제외된 문구가 저장되며 관리자 검수 화면에서 확인할 수 있습니다. AI 정제기는 향후 API 키가 생겼을 때 optional normalizer로 붙이는 것을 전제로 합니다.
