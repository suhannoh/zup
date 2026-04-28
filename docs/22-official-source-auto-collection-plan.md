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
- `APPROVED`: 관리자가 후보를 승인했다. 이번 MVP에서는 실제 `Benefit`으로 전환하지 않는다.
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

## 6. 향후 스케줄러 확장

이번 MVP는 관리자 수동 실행만 제공한다. 이후에는 `isActive=true`이고 `nextFetchAt`이 지난 `SourceWatch`만 제한된 빈도로 실행하는 스케줄러를 추가할 수 있다. 실패 횟수에 따른 backoff, 도메인별 요청 간격, 관리자 알림은 스케줄러 단계에서 확장한다.

## 7. 수집 주의사항

`robots.txt`, 사이트 이용 조건, 요청 빈도 제한을 확인해야 한다. timeout 없는 외부 요청은 금지하며, 과도한 반복 요청을 피해야 한다. 원문 HTML 전체는 저장하지 않고 본문 추출 텍스트만 저장한다.

## 8. 공식 출처 원칙

등록 대상은 브랜드 공식 홈페이지, 공식 앱 안내, 공식 멤버십, 공식 FAQ, 공식 공지, 공식 SNS, 고객센터 등 확인 가능한 공식 출처 URL로 제한한다. 블로그, 커뮤니티, 인터넷 전체 검색 크롤링은 수집 대상이 아니다.

## 9. 이번 단계에서 하지 않는 것

- 인터넷 전체 검색 크롤러 구현
- 블로그/커뮤니티 자동 수집
- 수집 결과 자동 게시
- `BenefitCandidate`를 사용자 화면에 노출
- 기존 `Benefit` API 응답에 후보 데이터 혼합
- AI, Gemini, OpenAI 연동
- Playwright 기반 수집
- 대규모 스케줄러 구현
