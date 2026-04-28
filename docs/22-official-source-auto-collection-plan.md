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
