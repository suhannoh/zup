# 공식 출처 자동 수집 로컬 E2E 검증 가이드

## 1. 목적

공식 출처 자동 수집 기능은 외부 사이트에 요청을 보내기 전에 로컬 fixture HTML로 먼저 검증한다. 이렇게 하면 실제 브랜드 사이트에 부담을 주지 않고, robots.txt나 이용 조건 확인 전에도 SourceWatch 등록부터 Candidate 승인까지의 애플리케이션 흐름을 반복해서 확인할 수 있다.

## 2. Fixture URL

프론트엔드 dev 서버가 `http://localhost:3000`에서 실행 중일 때 아래 파일을 사용할 수 있다.

- Candidate가 생성되어야 하는 fixture: `http://localhost:3000/collection-fixtures/birthday-benefit.html`
- Candidate가 생성되지 않아야 하는 fixture: `http://localhost:3000/collection-fixtures/no-birthday-benefit.html`

fixture는 테스트 전용 문서이며 실제 브랜드명이나 실제 혜택 데이터를 포함하지 않는다.

## 3. 실행 순서

Backend를 실행한다.

```powershell
cd backend
.\gradlew.bat bootRun
```

Frontend를 실행한다.

```powershell
cd frontend
npm run dev
```

관리자 화면은 JWT 로그인이 필요하다. 로컬 seed 계정이 활성화되어 있다면 기본 계정은 다음 값을 사용한다.

```text
email: admin@zup.local
password: admin1234
```

## 4. 화면 검증 절차

1. `/admin/source-watches`에 접속한다.
2. 브랜드를 선택한다.
3. sourceType은 `OFFICIAL_HOME` 또는 다른 공식 출처 타입을 선택한다.
4. title에는 `Local birthday fixture` 같은 테스트 제목을 입력한다.
5. URL에는 `http://localhost:3000/collection-fixtures/birthday-benefit.html`을 입력한다.
6. SourceWatch를 등록한다.
7. 등록된 항목에서 `수집 실행`을 누른다.
8. 결과에서 `candidateCount`가 1 이상인지 확인한다.
9. `/admin/benefit-candidates`에 접속한다.
10. 방금 생성된 Candidate의 상세 페이지로 이동한다.
11. `evidenceText`에 테스트 fixture 문장이 포함되는지 확인한다.
12. 승인 폼의 title, summary, usageCondition 등을 필요에 맞게 수정한다.
13. `Benefit으로 승인`을 누른다.
14. `Benefit 생성 완료: #{benefitId}` 메시지를 확인한다.
15. `생성된 혜택 관리로 이동` 링크로 `/admin/benefits/{benefitId}`에 이동한다.
16. 생성된 Benefit의 `verificationStatus`가 `VERIFIED`인지 확인한다.
17. `/api/v1/benefits` 또는 공개 화면에서 해당 Benefit이 노출되지 않는지 확인한다. `PUBLISHED` 전환 전까지 Public API에는 노출되지 않아야 한다.

## 5. PowerShell Smoke Script

아래 스크립트는 API 기준으로 같은 흐름을 검증한다.

```powershell
.\scripts\smoke-test-collection-local.ps1
```

프론트엔드나 백엔드 포트가 다르면 파라미터로 지정한다.

```powershell
.\scripts\smoke-test-collection-local.ps1 `
  -BaseUrl "http://localhost:8080" `
  -FrontendUrl "http://localhost:3000"
```

성공 시 마지막 줄에 다음 메시지가 출력된다.

```text
COLLECTION_SMOKE_TEST_OK
```

## 6. 실패 시 체크리스트

- 프론트엔드 dev 서버가 3000에서 실행 중인지 확인한다.
- fixture URL을 브라우저에서 열 수 있는지 확인한다.
- 백엔드가 해당 fixture URL에 접근 가능한지 확인한다.
- 관리자 로그인 토큰이 유효한지 확인한다.
- SourceWatch가 active 상태인지 확인한다.
- HTML에 생일 + 쿠폰/혜택 계열 키워드가 포함되어 있는지 확인한다.
- 같은 contentHash라면 두 번째 수집부터는 Candidate가 생성되지 않을 수 있다.
- 승인된 Benefit은 VERIFIED 상태이므로 Public API에는 바로 노출되지 않는다.
- `/admin/collection-runs`에서 최근 CollectionRun의 `failureReason`과 `errorMessage`를 확인한다.
- 실패가 반복되면 SourceWatch의 `nextFetchAt`과 `failureCount`도 함께 확인한다.

## 7. 주의사항

- smoke test 기본값으로 외부 공식 사이트를 사용하지 않는다.
- 실제 브랜드 혜택 데이터를 fixture에 넣지 않는다.
- Candidate 승인 시 자동으로 PUBLISHED 전환하지 않는다.
- Candidate를 Public API에 직접 노출하지 않는다.
- 이 검증은 스케줄러를 추가하지 않고 수동 collect API만 사용한다.

## 8. 자동 수집 스케줄러 확인

자동 수집 스케줄러는 기본 비활성화 상태다.

```text
COLLECTION_SCHEDULER_ENABLED=false
```

로컬에서 스케줄러 동작을 별도로 확인하려면 Backend 실행 전에 명시적으로 켠다.

```powershell
$env:COLLECTION_SCHEDULER_ENABLED="true"
$env:COLLECTION_SCHEDULER_FIXED_DELAY_MS="60000"
cd backend
.\gradlew.bat bootRun
```

스케줄러는 `nextFetchAt`이 비어 있거나 현재 시각 이전인 active SourceWatch만 처리한다. 여러 인스턴스 중복 실행을 막기 위해 Redis lock을 사용하며, lock을 얻지 못한 SourceWatch는 skip된다.

운영 전에는 외부 사이트의 robots.txt와 이용 조건을 확인하고, 기본 fetch interval을 과도하게 줄이지 않는다.

## 9. 수집 실행 이력 확인

수동 collect와 스케줄러 collect는 모두 `CollectionRun` 이력을 남긴다. 관리자 화면 `/admin/collection-runs`에서 최근 50개 실행 기록을 확인한다.

확인할 항목:

- `triggerType`: 수동 실행은 `MANUAL`, 스케줄러 실행은 `SCHEDULED`
- `status`: `SUCCESS`, `FAILED`, `SKIPPED`
- `candidateCount`
- `sameAsPrevious`
- `failureReason`
- `errorMessage`
- `durationMillis`

`sameAsPrevious=true`는 실패가 아니라 정상 성공이다. 같은 본문이라 Candidate 생성을 생략했다는 뜻이다.

## 10. fixture SourceWatch 관리

로컬 fixture SourceWatch는 자동 수집 E2E 검증 전용 데이터다. 실제 운영 SourceWatch 목록에서는 기본적으로 숨김 처리되며, `/admin/source-watches` 화면에서 `테스트 fixture 포함 보기`를 체크해야 함께 확인할 수 있다.

fixture 데이터가 더 이상 필요하지 않으면 삭제하지 말고 `fixture 비활성화` 버튼 또는 개별 활성 상태 변경으로 비활성화한다. hard delete는 하지 않는다.

실제 브랜드가 브랜드 드롭다운에 없다면 `/admin/brands`에서 브랜드를 먼저 등록한 뒤 `/admin/source-watches`로 돌아와 SourceWatch를 등록한다.

## 11. Candidate 근거 문장 품질 확인

자동 수집은 HTML 전체를 후보로 저장하지 않는다. 추출 단계에서 메뉴, 네비게이션, 푸터, SNS, 약관성 영역을 최대한 제거하고, Candidate 생성 단계에서 생일/birthday 키워드 주변 문장만 `evidenceText`로 저장한다.

E2E 확인 시 Candidate 상세에서 아래를 확인한다.

- `summary`가 메뉴 목록이나 푸터 문구 전체를 포함하지 않는지 확인
- `evidenceText`가 생일 쿠폰/혜택과 직접 관련된 짧은 문장 위주인지 확인
- `evidenceText`가 너무 길면 SourceWatch URL이 실제 본문 페이지인지 다시 확인
- 기존에 길게 생성된 Candidate는 새 정제 규칙이 소급 적용되지 않으므로 필요하면 `REJECTED` 처리 후 재수집한다.

## 12. 구체 혜택/이용안내 확인

Candidate 상세 화면에서는 `summary` 아래에 `구체 혜택 추정`과 `이용안내 추정`이 표시된다. 자동 수집은 생일 혜택 존재 여부뿐 아니라 쿠폰별 상세 혜택과 공통 이용 조건을 분리해 Candidate에 저장한다.

검수자는 아래를 확인한 뒤 Benefit 승인 폼을 정리한다.

- `benefitDetailText`: 할인 금액, 할인율, 무료/증정 상품, 제휴 브랜드별 쿠폰 내용이 들어있는지 확인
- `usageGuideText`: 회원 전용, 현금 교환 불가, 타인 양도 불가, 발급 횟수, 생년월일 기준, 유효기간/사용 조건 안내가 들어있는지 확인
- 승인 폼의 `usageCondition`은 `usageGuideText`가 있으면 이를 기본값으로 사용하고, 없으면 `evidenceText`를 사용한다.
- `minimumPurchaseDescription` 기본값은 쿠폰별 최소 구매 금액과 사용 조건은 각 쿠폰 상세 안내를 확인해야 한다는 문장이다.

자동 수집은 AI/Gemini/OpenAI 연동, Playwright 추가, 블로그/커뮤니티 수집, Candidate 자동 PUBLISHED, Public API Candidate 노출, 승인 없는 Benefit 생성을 하지 않는다.

## 13. 기존 스냅샷 후보 재생성

추출 로직 개선 후 기존 SourceWatch를 다시 수집하면 본문 hash가 같아 `sameAsPrevious=true`가 될 수 있다. 이 경우 `/admin/source-watches`에서 `후보 재생성`을 실행한다.

재생성 동작:

- 최신 `PageSnapshot.extractedText`를 개선된 Candidate 추출기로 다시 분석한다.
- 외부 URL을 다시 fetch하지 않는다.
- `CollectionRun`을 남기지 않는다.
- 새 후보는 `NEEDS_REVIEW` 상태로 생성된다.
- 기존 후보는 자동 삭제하거나 자동 `REJECTED` 처리하지 않는다.
- 완전히 같은 후보는 중복으로 생성하지 않고 skip count로 표시한다.

재생성 후 `/admin/benefit-candidates`에서 새 후보를 확인하고, 기존 긴 Candidate는 필요하면 직접 `REJECTED` 처리한다.
