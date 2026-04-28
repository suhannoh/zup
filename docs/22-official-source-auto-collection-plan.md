# 공식 출처 기반 혜택 후보 자동 수집 MVP

## 1. 목적

관리자가 등록한 공식 출처 URL을 서버가 수동으로 수집해 생일, 쿠폰, 혜택 관련 문장을 감지하고 `BenefitCandidate` 검수 후보로 저장한다. 이 기능은 혜택 정보를 빠르게 발견하기 위한 보조 도구이며, 확정 데이터 생성 기능이 아니다.

## 2. 자동 게시를 하지 않는 이유

공식 페이지의 문구는 조건, 기간, 회원 등급, 앱 설치 여부 같은 맥락을 함께 해석해야 한다. 키워드 기반 MVP는 후보 탐지까지만 담당하고, 실제 사용자 화면에 노출할 혜택은 관리자가 근거 문장을 검수한 뒤 별도 단계에서 생성한다.

## 3. 수집 흐름

`SourceWatch`에 브랜드와 공식 URL을 등록한다.

관리자가 `POST /api/v1/admin/source-watches/{id}/collect`를 호출하면 서버는 URL을 fetch하고, HTML에서 본문 텍스트를 추출한다. 추출된 텍스트로 `contentHash`를 계산한 뒤 `PageSnapshot`을 저장한다. 이전 hash와 같으면 `sameAsPrevious=true`로 저장하고 후보 생성은 생략한다. 변경된 본문이면 키워드 기반 detector가 `BenefitCandidate`를 생성해 관리자 검수 큐에 쌓는다.

최종 흐름:

`SourceWatch -> Fetch -> Extract -> Snapshot -> Candidate -> Admin Review`

## 4. Candidate 상태

- `DETECTED`: 시스템 감지 상태로 예약된 값이며 관리자가 수동 지정하지 않는다.
- `NEEDS_REVIEW`: 기본 상태. 관리자 검수가 필요하다.
- `APPROVED`: 관리자가 후보를 승인했다.
- `REJECTED`: 관리자가 후보를 반려했다.

## 5. 수동 실행 API 예시

```http
POST /api/v1/admin/source-watches/1/collect
Authorization: Bearer {admin-jwt}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "sourceWatchId": 1,
    "fetched": true,
    "sameAsPrevious": false,
    "candidateCount": 1,
    "message": "collection completed"
  },
  "message": "source watch collected"
}
```

## 6. Candidate 승인 후 Benefit 전환

관리자는 `POST /api/v1/admin/benefit-candidates/{id}/approve`로 후보를 승인한다. 승인 요청에는 최종 `Benefit`에 들어갈 제목, 요약, 혜택 유형, 생일 시점, 사용 조건 등을 넣을 수 있다. 요청 값이 비어 있으면 Candidate의 추정값을 기본값으로 사용한다.

승인 시 처리 흐름:

`BenefitCandidate -> Benefit 생성 -> BenefitSource 생성 -> VerificationLog 생성 -> Candidate.approvedBenefit 연결`

승인된 Candidate는 `APPROVED` 상태가 되고 `approvedBenefitId`, `approvedAt`을 가진다. 이미 `approvedBenefit`이 연결된 후보는 다시 승인할 수 없으며, `REJECTED` 후보도 승인할 수 없다.

## 7. 승인해도 바로 공개하지 않는 이유

Candidate 승인은 자동 수집 후보를 내부 `Benefit` 도메인으로 옮기는 단계일 뿐 사용자 공개 단계가 아니다. 생성된 Benefit의 기본 검수 상태는 `VERIFIED`이며, Public Benefit API는 `PUBLISHED` 상태만 노출한다. 따라서 관리자가 별도로 게시 상태로 전환하기 전까지 사용자 화면에는 노출되지 않는다.

## 8. BenefitSource / VerificationLog 생성 규칙

승인 시 `SourceWatch`의 공식 출처 정보를 기반으로 `BenefitSource`를 만든다.

- `sourceType`: `sourceWatch.sourceType`
- `sourceUrl`: `sourceWatch.url`
- `sourceTitle`: `sourceWatch.title`
- `sourceCheckedAt`: 승인 당일
- `memo`: 자동 수집 후보 승인과 evidence 일부

공식 출처 URL이 없거나 SourceWatch가 비활성 상태면 승인하지 않는다.

`VerificationLog`는 새 Benefit 생성 사실을 남긴다. `beforeStatus`는 `DRAFT`, `afterStatus`는 생성된 Benefit의 검수 상태로 기록하고, memo에는 candidateId를 포함한다.

## 9. 중복 승인 방지

다음 경우 승인 API는 실패해야 한다.

- Candidate가 존재하지 않음
- Candidate가 `REJECTED`
- Candidate에 이미 `approvedBenefit`이 연결됨
- SourceWatch가 비활성 상태
- 공식 출처 URL이 비어 있음

## 10. CollectionRun 실행 이력

수동 수집과 스케줄러 수집은 실행될 때마다 `CollectionRun` 이력을 남긴다. SourceWatch에는 마지막 상태만 저장되므로, 운영자는 CollectionRun으로 최근 실행 시각, 실행 주체, 성공/실패/스킵 여부, 후보 생성 수, 실패 원인, 실행 시간을 확인한다.

기록 항목:

- `triggerType`: `MANUAL` 또는 `SCHEDULED`
- `status`: `RUNNING`, `SUCCESS`, `FAILED`, `SKIPPED`
- `startedAt`, `endedAt`, `durationMillis`
- `fetched`
- `sameAsPrevious`
- `candidateCount`
- `failureReason`
- `errorMessage`

`sameAsPrevious=true`는 실패가 아니다. 본문이 이전 수집과 같아서 후보 생성을 생략한 정상 성공으로 기록한다.

비활성 SourceWatch 수집은 `SKIPPED`로 기록하고 `failureReason=SOURCE_WATCH_INACTIVE`를 남긴다. fetch 실패는 `FETCH_FAILED`, 본문 추출 실패는 `EXTRACT_FAILED`, 예외성 실패는 `UNKNOWN`으로 기록한다. `errorMessage`에는 HTML 전체나 긴 본문을 저장하지 않고 짧은 메시지만 저장한다.

운영 중 실패가 반복될 때는 CollectionRun의 `failureReason/errorMessage`와 SourceWatch의 `nextFetchAt/failureCount`를 함께 확인한다.

관리자 대시보드(`/admin`)에서는 자동 수집 운영 요약을 확인할 수 있다. 요약 기준은 최근 24시간이다.

- 전체 SourceWatch 수
- 활성 SourceWatch 수
- 검수 대기 Candidate 수
- 최근 24시간 수집 성공/실패/스킵 수
- 최근 24시간 내 최근 실패 SourceWatch 목록

운영자는 최근 실패 SourceWatch를 확인한 뒤 URL을 수정하거나, 필요하면 SourceWatch를 비활성화해 과도한 재시도를 막는다.

## 11. 향후 스케줄러 확장

1차 스케줄러는 구현되어 있지만 기본값은 비활성화되어 있다.

```text
COLLECTION_SCHEDULER_ENABLED=false
```

운영자가 `COLLECTION_SCHEDULER_ENABLED=true`를 명시한 경우에만 동작한다. 스케줄러는 `isActive=true`이고 `nextFetchAt is null` 또는 `nextFetchAt <= now`인 SourceWatch만 batch-size만큼 조회한다.

여러 인스턴스가 동시에 실행될 수 있으므로 Redis lock을 사용한다.

```text
collection:source-watch:lock:{sourceWatchId}
```

lock 획득에 실패한 SourceWatch는 해당 tick에서 건너뛴다. 수집 성공 또는 sameAsPrevious는 성공으로 보고 `nextFetchAt = now + defaultFetchIntervalMinutes`로 갱신한다. fetch/extract 실패는 실패로 보고 `failureCount`를 증가시킨 뒤 `nextFetchAt = now + failureRetryMinutes`로 갱신한다.

스케줄러를 운영 환경에서 켜기 전에는 robots.txt, 사이트 이용 조건, 도메인별 요청 빈도 제한을 확인해야 한다. 기본 interval은 하루 단위로 두고, 과도한 요청을 피한다.

## 12. 수집 주의사항

`robots.txt`, 사이트 이용 조건, 요청 빈도 제한을 확인해야 한다. timeout 없는 외부 요청은 금지하며, 과도한 반복 요청을 피해야 한다. 원문 HTML 전체는 저장하지 않고 본문 추출 텍스트만 저장한다.

HTML 추출 단계에서는 `script`, `style`, `noscript`, `header`, `footer`, `nav`, `aside`, `form`, `button`, `svg`, `iframe` 등 후보 검수에 불필요한 영역을 제거한다. `menu`, `gnb`, `lnb`, `breadcrumb`, `sidebar`, `sns`, `social` 같은 class/id를 가진 메뉴성 영역도 최대한 제거한다.

`BenefitCandidate`는 추출된 본문 전체를 그대로 `summary`나 `evidenceText`에 저장하지 않는다. 생일/birthday 키워드가 있는 문장을 기준으로 앞뒤 1문장까지 살피고, 메뉴/푸터/개인정보처리방침/전체메뉴/닫기 등 노이즈 문장은 제외한 뒤 최대 3문장, 500자 이내 근거만 저장한다.

자동 수집은 생일 혜택 존재 여부만 판단하지 않고, 구체 혜택 목록과 이용안내를 분리해 Candidate에 저장한다.

- `benefitDetailText`: 쿠폰별 혜택 추정 목록. 할인, 무료, 증정, 쿠폰, 포인트, 금액, %, 콤보, 세트, 케이크, 샐러드, 타임캡슐, 자물쇠 및 `free`, `discount`, `coupon`, `point`, `gift`, `combo` 키워드가 있는 문장을 최대 10개 저장한다.
- `benefitDetailImageSources`: 쿠폰 row 주변 로고/이미지의 `img src`, `alt`, `title`을 검수 참고용으로 저장한다. 상대경로 이미지는 SourceWatch URL 기준 절대경로로 변환한다.
- `usageGuideText`: 이용안내/주의사항 추정 문장. 회원만, 현금 교환, 양도, 발급, 지급, 1년에 1번, 추가 발급, 생년월일, 회원정보, 사용 조건, 최소 구매, 유효기간, 제외 등 조건성 문장을 700자 이내로 저장한다.
- `summary`는 구체 혜택이 있으면 대표 혜택 3~5개가 드러나도록 생성한다.

브랜드명이 이미지 로고로 제공되는 쿠폰 목록은 자동으로 브랜드명을 단정하지 않는다. 이미지 OCR은 사용하지 않고, 이미지 파일명만 보고 브랜드명을 확정하지 않는다. `alt` 또는 `title`이 명확한 경우에도 검수 참고값으로만 저장하며 최종 브랜드 판단은 운영자가 한다.

운영자는 `benefitDetailText`와 `usageGuideText`를 검수한 뒤 승인 폼의 `summary`와 `usageCondition`을 정리해 Benefit으로 승인한다. Candidate는 자동으로 PUBLISHED 되지 않으며, 승인 없이 Benefit을 생성하지 않는다.

Candidate 승인으로 생성된 Benefit은 `VERIFIED` 상태다. Public 화면에는 `PUBLISHED + isActive=true` 혜택만 노출된다. 공개 전 관리자는 Benefit 상세에서 요약, 이용 조건, 구매 조건, 사용 가능 기간, 공식 출처를 검수한 뒤 `PUBLISHED`로 전환한다. 공식 출처가 없는 혜택은 공개 전환하지 않는다.

Candidate 승인 전 관리자는 `benefitDetailText`를 기반으로 혜택 상세 리스트를 편집할 수 있다. 브랜드명이 이미지 로고로만 제공되는 경우 자동 확정하지 않고 비워두며, 관리자가 수동 입력한다. 승인 요청의 상세 리스트는 `BenefitDetailItem`으로 저장되고, 최종 공개 혜택 카드의 대표 혜택 목록으로 사용된다. Public 화면에는 `PUBLISHED` Benefit의 active detail item만 표시한다.

추출 로직을 개선한 뒤에는 기존 `PageSnapshot`의 `extractedText`를 재분석해 Candidate를 다시 만들 수 있다. `POST /api/v1/admin/source-watches/{id}/regenerate-candidates`는 최신 스냅샷을 사용하며 외부 URL fetch를 수행하지 않는다. 생성된 후보는 `NEEDS_REVIEW` 상태로 검수 대기하고, 기존 후보는 자동 삭제 또는 자동 반려하지 않는다. 품질이 낮은 기존 후보는 운영자가 직접 `REJECTED` 처리한다.

## 13. 공식 출처 원칙

등록 대상은 브랜드 공식 홈페이지, 공식 앱 안내, 공식 멤버십, 공식 FAQ, 공식 공지, 공식 SNS, 고객센터 등 확인 가능한 공식 출처 URL로 제한한다. 블로그, 커뮤니티, 인터넷 전체 검색 크롤링은 수집 대상이 아니다.

## 14. 이번 단계에서 하지 않는 것

- 인터넷 전체 검색 크롤러 구현
- 블로그/커뮤니티 자동 수집
- 수집 결과 자동 게시
- `BenefitCandidate`를 사용자 화면에 노출
- 기존 `Benefit` API 응답에 후보 데이터 혼합
- Candidate 승인 시 자동 `PUBLISHED` 전환
- AI, Gemini, OpenAI 연동
- Playwright 기반 수집
- 대규모 스케줄러 구현
## 관리자 UI 운영 흐름

- 공식 출처 자동 수집 운영은 `/admin` 대시보드에서 전체 수집 URL, 활성 수집 URL, 검수 대기 후보, 최근 성공/실패/스킵 현황을 확인하는 방식으로 정리했다.
- `/admin/source-watches`에서는 `수집 실행`과 `후보 재생성`을 구분한다. 수집 실행은 공식 URL을 다시 가져오고, 후보 재생성은 저장된 최신 스냅샷을 다시 분석한다.
- `/admin/benefit-candidates/{id}`에서는 수집 근거, 구체 혜택 추정, 이용안내 추정, 쿠폰 이미지 소스, 혜택 상세 리스트, 승인 폼을 분리해 검수한다.
- 후보 승인으로 생성된 Benefit은 `VERIFIED` 상태이며 Public 화면에는 노출되지 않는다. 관리자는 `/admin/benefits/{id}`에서 상세 리스트와 공식 출처를 확인한 뒤 `PUBLISHED`로 전환한다.
- 공개 화면에는 `PUBLISHED + isActive=true` Benefit과 active detail item만 표시한다. Candidate는 Public API에 노출하지 않는다.
