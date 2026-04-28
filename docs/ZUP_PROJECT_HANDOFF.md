# ZUP_PROJECT_HANDOFF

## 2026-04 공식 출처 자동 수집 handoff

완료:

- 공식 출처 자동 수집 Backend API
- SourceWatch 수동 collect
- PageSnapshot 저장
- BenefitCandidate 생성
- Candidate 승인 시 Benefit, BenefitSource, VerificationLog 생성
- 공식 출처 자동 수집 관리자 화면 연결
- 로컬 fixture HTML 및 E2E smoke guide 추가
- 공식 출처 자동 수집 스케줄러 1차 구현
- CollectionRun 수집 실행 이력 및 관리자 모니터링 화면
- 관리자 대시보드 자동 수집 운영 요약 카드

다음 검증:

- 로컬 fixture 기반 자동 수집 E2E 확인
- Candidate 승인 후 Benefit 생성 확인
- VERIFIED 상태 Benefit이 Public 화면에 바로 노출되지 않는지 확인
- 운영 전 robots.txt, 사이트 이용 조건, 요청 빈도 정책 확인
- CollectionRun의 failureReason/errorMessage와 SourceWatch의 nextFetchAt/failureCount를 함께 확인
- 최근 실패 SourceWatch는 URL 수정 또는 비활성화 여부 검토

관련 문서:

- `docs/22-official-source-auto-collection-plan.md`
- `docs/24-official-source-collection-e2e-guide.md`
- `docs/25-real-sourcewatch-registration-policy.md`
- `scripts/smoke-test-collection-local.ps1`

스케줄러 메모:

- 기본값은 `COLLECTION_SCHEDULER_ENABLED=false`
- `COLLECTION_SCHEDULER_ENABLED=true`일 때만 동작
- `nextFetchAt is null` 또는 `nextFetchAt <= now`인 active SourceWatch만 batch 처리
- Redis lock key: `collection:source-watch:lock:{sourceWatchId}`
- 성공 시 다음 수집은 기본 1440분 뒤, 실패 시 기본 180분 뒤

CollectionRun 메모:

- `/admin/collection-runs`에서 최근 50개 실행 이력 확인
- `/admin` 대시보드에서 최근 24시간 기준 운영 요약 확인
- 수동 수집은 `MANUAL`, 스케줄러 수집은 `SCHEDULED`
- `sameAsPrevious=true`는 정상 성공
- 실패 원인은 `failureReason`과 `errorMessage`로 확인

Candidate 정제 메모:

- HTML 전체를 후보 summary/evidenceText로 저장하지 않는다.
- 메뉴, 네비게이션, 푸터, SNS, 약관성 영역을 제거한 뒤 생일 키워드 주변 근거 문장만 저장한다.
- `evidenceText`는 최대 3문장, 500자 이내를 목표로 한다.
- 자동 수집은 생일 혜택 존재 여부뿐 아니라 구체 혜택 목록과 이용안내를 분리해 `benefitDetailText`, `usageGuideText`에 저장한다.
- 운영자는 `benefitDetailText`와 `usageGuideText`를 검수한 뒤 승인 폼의 `summary`와 `usageCondition`을 정리해 Benefit으로 승인한다.
- 승인 폼의 `usageCondition` 기본값은 `usageGuideText` 우선, 없으면 `evidenceText` fallback이다.
- 기존에 길게 생성된 Candidate는 필요하면 `REJECTED` 처리 후 재수집한다.

다음 단계:

- `docs/25-real-sourcewatch-registration-policy.md` 기준으로 실제 공식 URL 3~5개 선정
- 관리자 화면에서 SourceWatch 수동 등록
- 수동 collect 실행
- Candidate evidence 확인
- 승인 시 VERIFIED Benefit 생성 확인
- 필요 시 관리자가 PUBLISHED 전환

## 프로젝트명

Zup

루트 폴더명:

```text
zup
```

백엔드 패키지명:

```text
com.noh.zup
```

## 프로젝트 정의

Zup은 브랜드별 생일 쿠폰, 무료 혜택, 할인, 멤버십 혜택을 공식 출처 기준으로 수집하고, 사용자가 조건별로 빠르게 필터링해서 볼 수 있게 하는 검색 유입형 정보 큐레이션 서비스다.

서비스 카피:

```text
몰라서 못 받던 혜택, 오늘 줍자
```

## 핵심 원칙

- 사용자는 로그인하지 않는다.
- 개인 생일 입력은 MVP에서 받지 않는다.
- 브랜드 혜택은 공식 출처 기준으로 검수한다.
- 블로그/커뮤니티는 수집 힌트로만 사용한다.
- 공식 출처가 없는 혜택은 게시하지 않는다.
- 혜택마다 최근 확인일과 검수 상태를 관리한다.
- 사용자 제보는 운영자가 재검수하는 흐름으로 연결한다.
- 자동 크롤링으로 바로 게시하지 않는다.
- 브랜드 로고 무단 사용은 피한다.
- MVP는 브랜드별 생일 혜택에 집중한다.

## 현재 구현 상태

완료됨:

- Public 조회 API
- Admin CRUD API
- VerificationLog
- UserReport
- 제보 기반 `NEEDS_CHECK` 전환
- 공개 화면 API 연결
- 관리자 대시보드 화면
- 관리자 제보 화면
- 관리자 브랜드 관리 화면
- 관리자 혜택 관리 화면
- 관리자 혜택 상세 운영 화면
- 관리자 출처 관리 화면
- 관리자 태그 연결 관리 화면
- 관리자 검수 이력 조회 화면
- SEO sitemap/robots/metadata
- 관리자 JWT 인증 및 Admin API 보호
- MVP E2E 체크리스트
- 로컬 smoke test 스크립트

## 주요 도메인

- Category
- Brand
- Benefit
- BenefitSource
- Tag
- BenefitTag
- UserReport
- VerificationLog
- PageViewDaily
- AdminUser

## 상태 값

VerificationStatus:

```text
DRAFT
NEEDS_CHECK
VERIFIED
PUBLISHED
EXPIRED
HIDDEN
```

ReportStatus:

```text
RECEIVED
REVIEWING
RESOLVED
REJECTED
```

## SEO 구조

```text
/
/brands
/brands/{brandSlug}
/categories/{categorySlug}
/tags/{tagSlug}
/reports/new
```

예시:

```text
/brands/starbucks
/brands/oliveyoung
/categories/cafe
/tags/free
/tags/no-app-required
/tags/birthday-month
```

## 운영 데이터 입력 준비 문서

- `docs/14-data-entry-guide.md`
- `docs/15-official-source-verification-guide.md`
- `docs/16-initial-brand-collection-template.md`
- `docs/17-admin-operation-scenario.md`

## E2E 점검 문서와 스크립트

- `docs/18-mvp-e2e-checklist.md`
- `docs/19-local-smoke-test-guide.md`
- `scripts/smoke-test-local.ps1`

## 다음 단계

- 초기 공식 출처 기반 데이터 입력
- 관리자 인증 UX 개선
- 배포 환경 구성
- Search Console 등록
- Naver Search Advisor 등록

## SourceWatch 운영 입력 메모

- `/admin/source-watches`에서 실제 브랜드가 드롭다운에 없으면 `/admin/brands`에서 브랜드를 먼저 등록한다.
- 로컬 fixture SourceWatch는 E2E 검증용이며 운영 목록에서는 기본 숨김 처리한다.
- fixture SourceWatch는 삭제하지 않고 필요 시 비활성화한다.
- 실제 공식 URL 등록 전에는 `docs/25-real-sourcewatch-registration-policy.md` 기준을 확인한다.

## 기술 스택

Backend:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- PostgreSQL
- Redis

Frontend:

- Next.js
- React
- TypeScript
- Tailwind CSS
- Axios
- Zustand

Infra:

- Docker Compose
- Nginx
- AWS EC2 예정

## 금지 사항

- 일반 사용자 회원가입/로그인 MVP 추가 금지
- 사용자 생일 입력 기반 개인화 MVP 추가 금지
- AI 추천 추가 금지
- WebSocket 추가 금지
- 알림 기능 추가 금지
- 자동 크롤링과 자동 게시 추가 금지
- 브랜드 로고 무단 사용 금지
- 공식 확인되지 않은 혜택 게시 금지
