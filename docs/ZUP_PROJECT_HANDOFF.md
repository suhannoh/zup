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
- 쿠폰 브랜드명이 이미지 로고로 제공되면 `benefitDetailImageSources`에 `img src`, `alt`, `title`을 저장하고 브랜드명은 자동 확정하지 않는다.
- 이미지 OCR은 사용하지 않고 이미지 파일명만 보고 브랜드명을 추정하지 않는다.
- 운영자는 `benefitDetailText`와 `usageGuideText`를 검수한 뒤 승인 폼의 `summary`와 `usageCondition`을 정리해 Benefit으로 승인한다.
- 승인 폼의 `usageCondition` 기본값은 `usageGuideText` 우선, 없으면 `evidenceText` fallback이다.
- Candidate 승인으로 생성된 Benefit은 `VERIFIED` 상태다. Public 화면에는 `PUBLISHED + isActive=true` 혜택만 노출된다.
- 공개 전 관리자는 Benefit 상세에서 요약, 이용 조건, 공식 출처를 검수한 뒤 `PUBLISHED`로 전환한다. 공식 출처가 없는 혜택은 공개하지 않는다.
- Candidate 승인 전 관리자는 혜택 상세 리스트를 편집할 수 있고, 승인 시 `BenefitDetailItem`으로 저장된다.
- `BenefitDetailItem`은 Public 카드의 대표 혜택 목록으로 사용되며, Public 화면에는 active item만 표시된다.
- 브랜드명이 이미지 로고로만 제공되는 상세 항목은 자동 확정하지 않고 비워둔 뒤 관리자가 수동 입력한다.
- 추출 로직 개선 후 기존 스냅샷을 재분석하려면 `/admin/source-watches`에서 `후보 재생성`을 실행한다.
- 후보 재생성은 최신 `PageSnapshot.extractedText`를 사용하며 외부 URL fetch는 하지 않는다.
- 새 수집으로 생성된 `BenefitCandidate`는 `collectionRunId`를 직접 저장한다. 과거 데이터나 재생성 후보처럼 `collectionRunId`가 없는 경우에는 `snapshotId` 기반 fallback으로 추적한다.
- `/admin/source-watches/{id}/regenerate-candidates`도 `CollectionRun`을 남긴다. 이력에는 `triggerType=MANUAL_REGENERATE_CANDIDATES`, `fetched=false`, 사용한 `snapshotId`, 생성된 후보 수가 기록된다.
- 재생성된 후보는 `NEEDS_REVIEW` 상태이고, 기존 후보는 자동 삭제/자동 반려하지 않는다.
- 기존에 길게 생성된 Candidate는 필요하면 `REJECTED` 처리 후 재수집한다.

운영 DB 반영 절차:

1. [20260429_add_collection_run_id_to_benefit_candidates.sql](/Users/suhannoh/Downloads/zup/docs/sql/20260429_add_collection_run_id_to_benefit_candidates.sql)로 `collection_run_id` 컬럼과 인덱스를 반영한다.
2. 같은 SQL 파일의 `snapshot_id` 중복 점검 쿼리로 자동 backfill 가능 여부를 확인한다.
3. 중복이 없거나 유일한 `snapshot_id`만 대상으로 safe backfill SQL을 실행한다.
4. `/admin/benefit-candidates?collectionRunId={runId}` 와 `/admin/collection-runs`에서 `collectionRunId` 추적이 정상인지 확인한다.
5. 검증 SQL로 null 잔여 건수와 `collection_run_id` 분포를 확인한 뒤 운영 적용을 마무리한다.

주의사항:

- 운영 DB에서는 `ddl-auto=update`에 의존하지 않는다.
- `snapshot_id` 중복 run이 있으면 자동 backfill하지 말고 수동 검토한다.
- 후보 재생성 이력까지 기록한 이후에는 동일 `snapshot_id`를 여러 `CollectionRun`이 공유할 수 있다.
- 기존 데이터는 `snapshotId` fallback이 있으므로 backfill 전에도 화면은 깨지지 않는다.

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
## 관리자 UI 정리 메모

- 관리자 화면은 대시보드 중심의 넓은 데스크톱 레이아웃으로 정리했다. 좌측 고정 사이드바, 상단 헤더, `max-width: 1440px` 메인 영역을 사용한다.
- 사이드바 메뉴는 대시보드, 브랜드 관리, 공식 출처 수집, 혜택 후보 검수, 혜택 관리, 수집 이력, 제보 관리 순서로 구성하며 현재 경로는 파란색 active 상태로 표시한다.
- 후보 검수 화면은 2컬럼 구조로 정리했다. 좌측에는 수집 출처, 요약, 구체 혜택 추정, 이용안내 추정, 근거 원문, 쿠폰 이미지 소스, 혜택 상세 리스트 편집을 배치하고 우측에는 승인 전 체크와 혜택 승인 폼을 배치한다.
- 혜택 상세 리스트는 공개 화면에 표시될 대표 혜택 목록이므로 후보 승인 전 반드시 확인한다. 브랜드명이 이미지 로고로만 제공되는 경우 자동 확정하지 않고 운영자가 직접 입력한다.
- 혜택 관리 상세 화면은 승인된 혜택의 상세 리스트를 수정하고 공식 출처를 확인한 뒤 `PUBLISHED`로 전환하는 흐름을 강조한다.
- `VERIFIED` 혜택은 관리자 검수 완료 상태이며 사용자 화면에는 노출되지 않는다. Public 화면에는 `PUBLISHED + isActive=true` 혜택만 노출된다.

## CJ ONE 추출 잔여 이슈 처리 메모

- CJ ONE 샘플 HTML에는 쿠폰 row 로고 이미지 `<img>`가 포함되어 있다. 다만 기존 `PageSnapshot`은 원본 HTML을 저장하지 않으므로, 과거 스냅샷에 이미지 메타데이터가 없으면 후보 재생성만으로 `benefitDetailImageSources`를 복구할 수 없다.
- 이미지 소스 검수가 필요하면 개선된 추출기 적용 후 SourceWatch에서 `수집 실행`을 다시 수행한다. 그래도 이미지가 없으면 해당 운영 페이지가 JavaScript 렌더링 후 이미지를 삽입하는지 확인해야 한다.
- 할인 금액만 있는 `50% 할인`, `10,000원 할인`, `3,000원 할인`은 단독 공개 혜택 항목으로 만들지 않는다. 같은 row의 조건과 결합 가능한 경우에만 `혜택명 (조건: ...)` 형태로 후보 상세 리스트 기본값을 만든다.
- 생일축하쿠폰 후보의 이용안내에는 가입축하쿠폰 섹션 문구를 섞지 않는다. `가입축하쿠폰`, `최초 가입`, `제휴가입`, `아이디 등록`, `회원 전환` 문구는 제외한다.

## 수집 정책 메모

- SourceWatch 수집 실행 전 대상 도메인의 `robots.txt`를 확인한다. `Disallow`에 매칭되는 경로는 자동 수집하지 않는다.
- `robots.txt`가 없거나 404이면 수집 가능으로 보지만, 조회 실패나 파싱 실패는 보수적으로 수집 보류 처리한다.
- robots 차단/조회 실패/파싱 실패는 CollectionRun의 `failureReason`에 각각 `ROBOTS_TXT_DISALLOWED`, `ROBOTS_TXT_FETCH_FAILED`, `ROBOTS_TXT_PARSE_FAILED`로 남긴다.
- 같은 `host`의 최근 `SUCCESS`/`FAILED`/`SKIPPED` 실행이 `app.crawler.min-domain-interval-seconds` 이내이면 `RATE_LIMITED_BY_DOMAIN`으로 `SKIPPED` 처리한다.
- 수동 수집은 SourceWatch 단위 lock으로 중복 실행을 막고, 이미 진행 중이면 `COLLECTION_ALREADY_RUNNING`으로 `SKIPPED` 처리한다.
- `/admin/source-watches` 카드에서 최근 CollectionRun 상태, 시각, 후보 생성 수, 실패/스킵 사유, robots.txt 확인 결과를 함께 확인한다.
- 반복 실패가 누적되면 `failureCount`, `nextFetchAt` 기반 backoff를 다음 단계 개선 후보로 본다.
- 외부 이미지 URL은 관리자 검수 참고용이다. Public 화면에는 기본적으로 외부 브랜드 로고/쿠폰 이미지를 직접 노출하지 않고 텍스트 정보와 공식 출처 링크를 우선한다.
