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

수집 실행 전에는 SourceWatch 단위 중복 실행을 먼저 막고, 같은 `host`의 최근 수집 시각이 최소 간격 안에 있는지 확인한 뒤, SourceWatch URL의 `scheme + host` 기준으로 `robots.txt`를 확인한다.

```text
수집 대상: https://m.cjone.com/cjmmobile/guide/guidePrsCpnInfo.do
확인 대상: https://m.cjone.com/robots.txt
```

지원 규칙은 `User-agent`, `Allow`, `Disallow`이다. `Disallow`가 대상 path에 매칭되면 자동 수집하지 않는다. 더 구체적인 `Allow` 규칙이 있으면 허용한다. `robots.txt`가 없거나 404이면 수집 가능으로 보되, 요청 속도 제한과 출처 표시 정책을 따른다. `robots.txt` 조회 실패나 파싱 실패는 보수적으로 수집을 보류하고 CollectionRun에 사유를 남긴다.

도메인별 최소 수집 간격은 `app.crawler.min-domain-interval-seconds`로 관리한다. 기본값은 60초이며, 같은 host의 최근 `SUCCESS`/`FAILED`/`SKIPPED` 실행이 이 간격 안에 있으면 수집을 `SKIPPED` 처리한다.

수집 보류 사유:

- `ROBOTS_TXT_DISALLOWED`: robots.txt 정책에 의해 차단
- `ROBOTS_TXT_FETCH_FAILED`: robots.txt 조회 실패
- `ROBOTS_TXT_PARSE_FAILED`: robots.txt 파싱 실패
- `RATE_LIMITED_BY_DOMAIN`: 같은 도메인의 최근 수집 이후 최소 수집 간격 미도달
- `COLLECTION_ALREADY_RUNNING`: 같은 SourceWatch 수집이 이미 진행 중

Crawler User-Agent는 설정값으로 관리한다.

```yaml
app:
  crawler:
    user-agent: "ZupOfficialSourceCollector/1.0"
    min-domain-interval-seconds: 60
```

### CJ ONE 수동 관리 전환

CJ ONE 생일축하쿠폰 안내 카드(`https://m.cjone.com/cjmmobile/guide/guidePrsCpnInfo.do`)는 일반 봇(`User-agent: *`)에 대해 `Disallow: /` 정책이 확인되었으므로 자동 수집 대상에서 제외한다.

- robots 차단을 우회하지 않는다.
- User-Agent를 Googlebot, NaverBot 등으로 위장하지 않는다.
- SourceWatch는 삭제하지 않고 공식 출처 URL을 수동 검수 근거로 유지한다.
- 해당 브랜드 혜택은 관리자가 공식 페이지를 직접 확인한 뒤 수동으로 등록/수정한다.
- robots 차단 SourceWatch에서는 저장된 스냅샷 기반 후보 재생성도 허용하지 않는다.
- 기존 미승인 후보는 `needsManualReview=true`로 전환하고 관리자 재검토 필요 뱃지를 표시한다.
- public 화면에는 공식 출처 URL과 최종 확인일을 표시하되, 외부 로고/쿠폰 이미지/배너 이미지는 직접 노출하지 않는다.
- 원본 HTML 전문은 장기 보관하지 않는다. 기존 CJ ONE snapshot은 `isForReviewOnly=true`, `expiresAt=30일` 기준으로 정리 대상으로 관리한다.
- DB 전환 SQL 초안은 `docs/sql/20260429_cjone_robots_blocked_manual_transition.sql`에 둔다. 실제 삭제는 별도 운영 작업으로 수행한다.

운영 배포 URL이 확정되면 `ZupBot/1.0 (+https://...)`처럼 연락 가능한 URL을 포함한 User-Agent로 변경한다. 공식 사이트를 속이기 위한 User-Agent는 사용하지 않는다.

`Crawl-delay`와 `Sitemap`은 현재 자동 처리하지 않는다. Crawl-delay는 후속 작업에서 도메인별 rate limit과 함께 검토한다.

요청 속도 정책은 보수적으로 운용한다.

- 같은 SourceWatch 수동 수집 버튼 연타 방지
- 도메인별 최소 요청 간격 적용
- 4xx/5xx 반복 시 backoff
- robots.txt Disallow 경로 자동 수집 금지
- 로그인, 우회, 비정상 접근 금지

HTML 추출 단계에서는 `script`, `style`, `noscript`, `header`, `footer`, `nav`, `aside`, `form`, `button`, `svg`, `iframe` 등 후보 검수에 불필요한 영역을 제거한다. `menu`, `gnb`, `lnb`, `breadcrumb`, `sidebar`, `sns`, `social` 같은 class/id를 가진 메뉴성 영역도 최대한 제거한다.

`BenefitCandidate`는 추출된 본문 전체를 그대로 `summary`나 `evidenceText`에 저장하지 않는다. 생일/birthday 키워드가 있는 문장을 기준으로 앞뒤 1문장까지 살피고, 메뉴/푸터/개인정보처리방침/전체메뉴/닫기 등 노이즈 문장은 제외한 뒤 최대 3문장, 500자 이내 근거만 저장한다.

자동 수집은 생일 혜택 존재 여부만 판단하지 않고, 구체 혜택 목록과 이용안내를 분리해 Candidate에 저장한다.

- `benefitDetailText`: 쿠폰별 혜택 추정 목록. 할인, 무료, 증정, 쿠폰, 포인트, 금액, %, 콤보, 세트, 케이크, 샐러드, 타임캡슐, 자물쇠 및 `free`, `discount`, `coupon`, `point`, `gift`, `combo` 키워드가 있는 문장을 최대 10개 저장한다.
- `benefitDetailImageSources`: 쿠폰 row 주변 로고/이미지의 `img src`, `alt`, `title`을 검수 참고용으로 저장한다. 상대경로 이미지는 SourceWatch URL 기준 절대경로로 변환한다.
- `usageGuideText`: 이용안내/주의사항 추정 문장. 회원만, 현금 교환, 양도, 발급, 지급, 1년에 1번, 추가 발급, 생년월일, 회원정보, 사용 조건, 최소 구매, 유효기간, 제외 등 조건성 문장을 700자 이내로 저장한다.
- `summary`는 구체 혜택이 있으면 대표 혜택 3~5개가 드러나도록 생성한다.

브랜드명이 이미지 로고로 제공되는 쿠폰 목록은 자동으로 브랜드명을 단정하지 않는다. 이미지 OCR은 사용하지 않고, 이미지 파일명만 보고 브랜드명을 확정하지 않는다. `alt` 또는 `title`이 명확한 경우에도 검수 참고값으로만 저장하며 최종 브랜드 판단은 운영자가 한다.

외부 이미지 URL은 관리자 검수 참고용으로만 사용한다. Public 화면에는 기본적으로 외부 브랜드 로고나 쿠폰 이미지를 직접 노출하지 않는다. Public 이미지 노출은 브랜드의 명시적 허락 또는 공식 배포 가이드가 확인된 경우에만 검토한다. 로고/상표 이미지는 저작권·상표권 이슈가 있을 수 있으므로 텍스트 정보와 공식 출처 링크를 우선한다. 출처 표시는 필수지만, 출처 표시만으로 모든 사용 허락을 대체하지 않는다.

운영자는 `benefitDetailText`와 `usageGuideText`를 검수한 뒤 승인 폼의 `summary`와 `usageCondition`을 정리해 Benefit으로 승인한다. Candidate는 자동으로 PUBLISHED 되지 않으며, 승인 없이 Benefit을 생성하지 않는다.

Candidate 승인으로 생성된 Benefit은 `VERIFIED` 상태다. Public 화면에는 `PUBLISHED + isActive=true` 혜택만 노출된다. 공개 전 관리자는 Benefit 상세에서 요약, 이용 조건, 구매 조건, 사용 가능 기간, 공식 출처를 검수한 뒤 `PUBLISHED`로 전환한다. 공식 출처가 없는 혜택은 공개 전환하지 않는다.

Candidate 승인 전 관리자는 `benefitDetailText`를 기반으로 혜택 상세 리스트를 편집할 수 있다. 브랜드명이 이미지 로고로만 제공되는 경우 자동 확정하지 않고 비워두며, 관리자가 수동 입력한다. 승인 요청의 상세 리스트는 `BenefitDetailItem`으로 저장되고, 최종 공개 혜택 카드의 대표 혜택 목록으로 사용된다. Public 화면에는 `PUBLISHED` Benefit의 active detail item만 표시한다.

추출 로직을 개선한 뒤에는 기존 `PageSnapshot`의 `extractedText`를 재분석해 Candidate를 다시 만들 수 있다. `POST /api/v1/admin/source-watches/{id}/regenerate-candidates`는 최신 스냅샷을 사용하며 외부 URL fetch를 수행하지 않는다. 생성된 후보는 `NEEDS_REVIEW` 상태로 검수 대기하고, 기존 후보는 자동 삭제 또는 자동 반려하지 않는다. 품질이 낮은 기존 후보는 운영자가 직접 `REJECTED` 처리한다.

단, 기존 `PageSnapshot`은 원본 HTML 전체를 보관하지 않는다. 따라서 과거 스냅샷에 `benefitDetailImageSources`가 비어 있으면 후보 재생성만으로 새 이미지 메타데이터를 복구할 수 없다. 이미지 로고 단서가 필요한 SourceWatch는 개선된 `HtmlTextExtractor`가 적용된 뒤 `수집 실행`으로 최신 HTML을 다시 가져와야 한다.

CJ ONE 생일축하쿠폰 샘플 HTML 기준으로 서버 HTML 안에는 쿠폰 로고 `<img>`가 존재한다. 현재 정적 HTML fetch에서 이미지가 계속 비어 있다면 기존 스냅샷이 이미지 메타데이터를 저장하기 전 생성되었거나, 운영 시점의 페이지가 JavaScript 렌더링 후 이미지를 삽입하는 구조일 수 있다. Playwright/OCR은 이번 범위에 포함하지 않으며, 필요 시 추후 SourceWatch별 `renderMode` 설계 후보로 검토한다.

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

## 혜택 상세 추출 품질 보강

- 혜택 상세 후보는 실제 사용자가 받는 혜택 문구 위주로 추출한다. `30,000원 이상 구매 시`, `CJ ONE 쿠폰으로`, `생일축하쿠폰선택됨`처럼 조건문, 불완전 문구, UI 잔여 텍스트는 단독 혜택명으로 저장하지 않는다.
- `50% 할인`, `10,000원 할인`, `3,000원 할인`처럼 금액/퍼센트 할인만 있는 문구는 단독 detail item으로 저장하지 않는다. 같은 row의 조건 문구와 결합할 수 있을 때만 `10,000원 할인 (조건: 4만원 이상 주문 시)`처럼 검수 가능한 형태로 저장한다.
- 생일축하쿠폰 후보의 이용안내에서는 `가입축하쿠폰`, `최초 가입`, `제휴가입`, `아이디 등록`, `회원 전환` 등 다른 쿠폰 탭/섹션 문구를 제외한다.
- `무료% 할인`처럼 무료와 할인율이 잘못 결합된 문구는 `무료` 중심으로 정리한다.
- 자동 생성되는 대표 혜택 목록은 기본 7개 이내로 제한해 운영자 검수 부담을 줄인다.
- 이미지 로고 기반 쿠폰 row는 `img src`, `alt`, `title`, `aria-label`, 부모 링크, 부모 title/aria-label, 주변 텍스트를 함께 저장한다.
- `alt`, `title`, `aria-label`처럼 접근성 속성에 명확한 브랜드명이 있을 때만 브랜드명 후보로 신뢰한다. 이미지 파일명이나 CDN URL만 보고 detail item의 브랜드명을 자동 확정하지 않는다.
- Public 브랜드 페이지에는 `PUBLISHED + isActive=true` 혜택만 노출한다. detail item이 비어 있어도 혜택 카드 자체는 표시하고, 상세 혜택은 공식 안내 확인 문구로 fallback한다.

## 15. 보수적 수집 허용 기준

- SourceWatch 자동 수집은 robots.txt 확인, 공개 페이지 여부, 약관 수동 확인, 로그인 필요 여부, 요청 간격 제한을 모두 통과한 경우에만 허용한다.
- `termsCheckStatus`는 자동 판단하지 않는다. 관리자가 약관 페이지를 직접 열어 크롤링, 스크래핑, 재배포, 자동 수집 금지 조항을 확인한 뒤 입력한다.
- `termsCheckStatus=NOT_CHECKED`는 allowed가 아니다. 신규 SourceWatch 기본값은 `NOT_CHECKED`와 `UNKNOWN_NEEDS_REVIEW`이며 자동 수집 버튼은 비활성화한다.
- `robotsCheckStatus=DISALLOWED/FETCH_FAILED/PARSE_FAILED`, `termsCheckStatus=RESTRICTION_FOUND/NOT_CHECKED/NEEDS_REVIEW`, `loginRequired=true`, `collectionPermissionStatus != ALLOWED_TO_COLLECT`는 모두 `SKIPPED` 처리한다.
- `collectionPermissionStatus`는 규칙으로 계산한다. 관리자가 수동으로 `ALLOWED_TO_COLLECT`로 올릴 수 없고, `MANUAL_REVIEW_ONLY`로 낮추는 것만 허용한다.
- robots-blocked 사이트의 기존 `PageSnapshot`은 30일 후 삭제 대상으로 `expiresAt`을 관리한다. 이번 단계에서 실제 DB 삭제는 하지 않는다.
- robots-blocked 또는 terms-blocked 출처의 미승인 후보는 `needsManualReview=true`로 전환해 관리자 재검토 대상으로 표시한다.
- 이미 public인 Benefit은 유지하되, `lastVerifiedDate` 또는 `lastVerifiedAt`이 30일 이상 지난 경우 대시보드에서 재확인 대상으로 본다.

## 16. Public 고지와 이미지 정책

- Zup은 브랜드와 공식 제휴 또는 파트너십 관계가 아니며 쿠폰을 직접 발급하거나 판매하지 않는다.
- Public 혜택 상세 하단에는 출처, 최종 확인일, 확인 방식, 변경 가능성, 사용 전 공식 앱/홈페이지 확인 안내를 표시한다.
- 수동 검수 혜택은 자동 수집 정보가 아니라 관리자가 공식 페이지를 직접 확인해 정리한 정보라고 구분한다.
- Public DTO에는 `BenefitDetailItem.imageUrl`을 포함하지 않는다. 외부 이미지, 브랜드 로고, 쿠폰 이미지, 배너 이미지는 admin 검수 참고용으로만 사용한다.
- 공개 전 금지어가 있으면 공개 버튼을 비활성화하고 백엔드에서도 `PUBLISHED` 전환을 거부한다.

## 17. API 없이 동작하는 규칙 기반 추출 파이프라인

- 이번 단계에서는 OpenAI API 또는 외부 LLM API를 사용하지 않는다. ChatGPT Plus는 API 사용권이 아니며, 키/비용 관리 없이도 1차 품질 개선은 규칙 기반으로 수행한다.
- 현재 흐름은 `HtmlTextExtractor`가 DOM을 블록 단위로 나누고, `BenefitCandidateDetector`가 블록 문맥을 분류한 뒤 후보를 만든다.
- 향후 API 키가 생기면 `RuleBasedBenefitExtractor -> Optional AiBenefitNormalizer -> BenefitExtractionValidator -> BenefitCandidate` 구조로 확장할 수 있다. 이번 구현에는 AI normalizer를 넣지 않는다.
- 전체 페이지 textContent를 그대로 후보 생성에 사용하지 않는다. `section`, `article`, `li`, `tr`, card/coupon/benefit/item/tab/accordion 계열 블록을 우선 추출하고, `--- ZUP_BLOCK ---` 구분자로 스냅샷 텍스트에 보존한다.
- 후보 `evidenceText`는 최대 200자 이내 발췌로 제한한다. 원문 블록 전체를 장기 보관하지 않는다.
- 블록 문맥은 `BIRTHDAY_BENEFIT`, `MEMBERSHIP_REWARD`, `GRADE_BENEFIT`, `COUPON_USAGE_GUIDE`, `CONDITION_NOTICE`, `FAQ`, `FOOTER_OR_NAV`, `UNKNOWN`으로 분류한다.
- 생일 후보는 생일, birthday, birth, 생년월일, 무료 음료 e-쿠폰, 생일 쿠폰과 가까운 블록 및 주변 조건/이용안내 블록만 사용한다.
- 별 적립, 별 15개/50개, 푸드/MD 바우처, 등급 전용 혜택, Gold/Welcome/Green, 포인트 적립, 이벤트 문맥은 생일 후보에서 제외한다.
- 조건문 단독 문구와 UI 잔여 텍스트는 혜택명으로 만들지 않는다. 제외된 문구는 후보의 `excludedTexts`에 남겨 관리자 검수 화면에서 확인한다.
- 후보에는 `extractionWarnings`, `contextEvidence`, `excludedTexts`를 저장한다. 경고가 있으면 `needsManualReview=true`로 표시한다.
- 스타벅스/CJ ONE 케이스는 live 수집이 아니라 단위 테스트용 최소 HTML 조각으로 검증한다. CJ ONE 실제 URL은 robots 차단 상태이므로 다시 자동 수집하지 않는다.
