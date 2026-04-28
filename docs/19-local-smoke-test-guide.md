# 19. 로컬 Smoke Test 가이드

## 1. 목적

`scripts/smoke-test-local.ps1`은 Zup MVP 핵심 API 흐름을 로컬 개발 환경에서 한 번에 확인하는 PowerShell 스크립트다.

이 스크립트는 API 레벨에서 다음 흐름을 검증한다.

```text
health 확인
→ public API 확인
→ admin API 인증 차단 확인
→ 관리자 로그인
→ 브랜드 생성
→ 혜택 생성
→ 출처 등록
→ 태그 연결
→ PUBLISHED 상태 변경
→ 사용자 상세 노출 확인
→ 사용자 제보 생성
→ NEEDS_CHECK 전환 확인
→ VerificationLog 확인
→ 제보 RESOLVED 처리
```

## 2. 사전 조건

- Docker 개발 인프라가 실행 중이어야 한다.
- 백엔드가 실행 중이어야 한다.
- local 관리자 계정이 생성되어 있어야 한다.

기본 계정:

```text
admin@zup.local / admin1234
```

기본 백엔드 URL:

```text
http://localhost:8080
```

## 3. 실행 명령

기본 실행:

```powershell
.\scripts\smoke-test-local.ps1
```

백엔드 URL 또는 관리자 계정 override:

```powershell
.\scripts\smoke-test-local.ps1 `
  -BaseUrl "http://localhost:8080" `
  -AdminEmail "admin@zup.local" `
  -AdminPassword "admin1234"
```

## 4. 검증되는 API 목록

Public API:

- `GET /api/v1/health`
- `GET /api/v1/categories`
- `GET /api/v1/tags`
- `GET /api/v1/brands`
- `GET /api/v1/brands/{slug}`
- `POST /api/v1/reports`

Admin Auth API:

- `POST /api/v1/admin/auth/login`

Admin API:

- `GET /api/v1/admin/dashboard`
- `POST /api/v1/admin/brands`
- `POST /api/v1/admin/benefits`
- `GET /api/v1/admin/benefits/{id}`
- `PATCH /api/v1/admin/benefits/{id}/status`
- `POST /api/v1/admin/benefits/{id}/sources`
- `POST /api/v1/admin/benefits/{id}/tags`
- `GET /api/v1/admin/benefits/{id}/verification-logs`
- `GET /api/v1/admin/reports`
- `PATCH /api/v1/admin/reports/{id}/status`

## 5. 스크립트가 생성하는 데이터

스크립트는 로컬 DB에 다음 테스트 데이터를 생성한다.

- 브랜드
  - name: `Smoke Test Brand {timestamp}`
  - slug: `smoke-brand-{timestamp}`
- 혜택
  - title: `Smoke Test Birthday Benefit {timestamp}`
  - status: `DRAFT → PUBLISHED → NEEDS_CHECK`
- 출처
  - sourceType: `OFFICIAL_HOME`
  - sourceUrl: `https://example.com`
- 태그 연결
  - `/api/v1/tags`의 첫 번째 tag 사용
- 사용자 제보
  - reportType: `WRONG_INFO`
  - status: `RECEIVED → RESOLVED`

## 6. 주의사항

- 이 스크립트는 로컬 개발 DB에 테스트 데이터를 생성한다.
- 운영 DB에서 실행하지 않는다.
- 반복 실행 시 slug 중복을 피하기 위해 timestamp suffix를 사용한다.
- 스크립트는 데이터 삭제를 수행하지 않는다.
- 테스트 데이터가 많아지면 로컬 DB를 초기화하거나 관리자 화면에서 비활성화한다.

## 7. 성공 출력 예시

```text
[OK] health
[OK] public categories count=6
[OK] public tags count=14
[OK] public brands count=8
[OK] admin dashboard blocked without token
[OK] admin login
[OK] admin dashboard with token
[OK] created brand id=...
[OK] created benefit id=...
[OK] created source id=...
[OK] added tag id=...
[OK] published benefit
[OK] public brand detail includes benefit
[OK] report created id=...
[OK] benefit moved to NEEDS_CHECK
[OK] verification logs count=2
[OK] admin reports include report
[OK] report resolved
SMOKE_TEST_OK
```

## 8. 실패 시 확인할 것

### health 실패

- 백엔드가 실행 중인지 확인한다.
- `BaseUrl`이 맞는지 확인한다.
- `SERVER_PORT` 설정을 확인한다.

### public API 실패

- local seed 데이터가 정상 생성되었는지 확인한다.
- DB 연결과 Hibernate ddl 설정을 확인한다.

### admin login 실패

- `SPRING_PROFILES_ACTIVE=local`인지 확인한다.
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`가 실행 환경과 일치하는지 확인한다.
- `admin_users` 테이블에 초기 계정이 생성되었는지 확인한다.

### admin API가 token 없이 401이 아닌 경우

- 최신 백엔드 코드가 실행 중인지 확인한다.
- 이전 bootRun 프로세스가 8080 포트를 점유하고 있지 않은지 확인한다.

### 사용자 브랜드 상세에 혜택이 보이지 않는 경우

- 혜택 상태가 `PUBLISHED`인지 확인한다.
- 혜택 `isActive=true`인지 확인한다.
- 브랜드가 활성 상태인지 확인한다.

### 제보 후 NEEDS_CHECK 전환 실패

- reportType이 자동 전환 대상인지 확인한다.
- request에 `benefitId`가 포함되었는지 확인한다.
- 대상 혜택이 이미 `NEEDS_CHECK`인지 확인한다.
