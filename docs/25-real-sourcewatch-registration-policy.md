# 실데이터 SourceWatch 등록 정책

## 1. SourceWatch 등록 가능 기준

SourceWatch에는 사람이 공식 출처임을 확인한 URL만 등록한다. 자동 수집은 후보 탐지 보조 기능이며, 등록 URL 자체의 신뢰도가 낮으면 잘못된 후보가 계속 생성될 수 있다.

등록 가능한 URL:

- 브랜드 공식 홈페이지
- 공식 멤버십 안내 페이지
- 공식 FAQ
- 공식 공지사항
- 공식 이벤트 안내 페이지
- 브랜드 공식 약관/혜택 안내 페이지

등록하면 안 되는 URL:

- 블로그
- 카페/커뮤니티
- 인스타그램 개인 계정
- 비공식 정리글
- 로그인 없이는 볼 수 없는 개인 쿠폰함
- 과도한 JS 렌더링이 필요한 페이지
- robots.txt 또는 이용 조건상 수집이 부적절한 페이지

## 2. 등록 전 확인 체크리스트

SourceWatch 등록 전 아래 항목을 확인한다.

- 공식 도메인인지 확인
- 페이지가 공개 접근 가능한지 확인
- 생일, 쿠폰, 혜택 관련 정보가 실제로 포함되어 있는지 확인
- robots.txt 또는 이용 조건상 자동 접근이 부적절하지 않은지 확인
- 너무 자주 변하지 않는 정적 안내 페이지인지 확인
- 로그인 또는 본인 인증이 필요한 페이지는 제외

## 3. 관리자 화면 등록 절차

1. `/admin/source-watches`에 접속한다.
2. 브랜드를 선택한다.
3. `sourceType`을 선택한다.
4. title을 입력한다.
5. 공식 URL을 입력한다.
6. SourceWatch를 등록한다.
7. `수집 실행`을 누른다.
8. `candidateCount`를 확인한다.
9. `/admin/benefit-candidates`에서 후보를 확인한다.
10. 근거 문장을 검토한 뒤 승인 또는 반려한다.

## 4. 수집 결과 판단 기준

### candidateCount > 0

- `evidenceText`를 확인한다.
- 실제 혜택 정보인지 검토한다.
- 잘못 감지된 문장이면 `REJECTED`로 처리한다.
- 의미 있는 후보면 `Benefit으로 승인`한다.
- 승인된 Benefit은 `VERIFIED` 상태로 생성되며, 관리자가 `PUBLISHED`로 전환하기 전까지 사용자 화면에는 노출되지 않는다.

### candidateCount = 0

- 페이지에 생일, 쿠폰, 혜택 키워드가 없는지 확인한다.
- 텍스트 추출이 제대로 되었는지 확인한다.
- 필요하면 SourceWatch URL을 더 적절한 공식 페이지로 변경한다.
- 후보가 없다는 결과만으로 수집 실패라고 판단하지 않는다.

### sameAsPrevious = true

- 이전 수집과 본문이 동일하다는 의미다.
- 실패가 아니다.
- 새 Candidate가 생성되지 않는 것이 정상이다.

## 5. 운영 수집 주기 기준

- 기본 수집 주기는 24시간 이상으로 둔다.
- 실패 재시도 간격은 3시간 이상으로 둔다.
- 동일 URL을 과도하게 반복 수집하지 않는다.
- 실제 운영 초기에는 스케줄러를 비활성화한 상태에서 수동 collect로 먼저 검증한다.
- 스케줄러를 켜기 전에는 robots.txt, 사이트 이용 조건, 도메인별 요청 빈도 정책을 다시 확인한다.

## 6. 실데이터 1차 등록 계획

처음에는 3~5개 URL만 등록한다.

추천 카테고리:

- 멤버십 안내 페이지 1개
- FAQ 페이지 1개
- 이벤트/혜택 안내 페이지 1개
- 브랜드 공식 혜택 페이지 1개

주의:

- 실제 URL은 Codex가 임의로 만들지 않는다.
- 공식 출처를 사람이 확인한 뒤 관리자 화면에서 직접 등록한다.
- 실데이터 URL은 seed 데이터에 넣지 않는다.
- 수동 collect 결과와 Candidate evidence를 확인한 뒤 운영 수집 주기와 스케줄러 활성화 여부를 결정한다.

## 7. 운영 검증 기록

실제 SourceWatch를 등록한 뒤 아래 내용을 운영 기록에 남긴다.

- 등록 일시
- 브랜드명
- SourceWatch title
- 공식 URL
- sourceType
- 수동 collect 결과
- candidateCount
- sameAsPrevious
- CollectionRun status
- Candidate 승인 또는 반려 결과
- 필요 조치: URL 수정, 비활성화, PUBLISHED 전환 등

## 8. 관리자 화면 브랜드 선택 기준

`/admin/source-watches`의 브랜드 드롭다운은 관리자 브랜드 목록을 기준으로 표시한다. 등록하려는 브랜드가 목록에 없다면 SourceWatch 화면 안에서 임시로 만들지 않고 `/admin/brands`로 이동해 브랜드를 먼저 등록한다.

권장 흐름:

1. `/admin/source-watches`에서 브랜드 존재 여부를 확인한다.
2. 브랜드가 없으면 `/admin/brands`로 이동한다.
3. 브랜드를 등록한다.
4. `/admin/source-watches`로 돌아와 공식 URL을 등록한다.

## 9. fixture SourceWatch 운영 처리

`Local collection fixture` 제목이나 `/collection-fixtures/` URL을 가진 SourceWatch는 로컬 E2E 검증용이다. 실제 운영 목록에서는 기본적으로 숨김 처리하고, 확인이 필요할 때만 `테스트 fixture 포함 보기`를 사용한다.

fixture SourceWatch를 정리할 때는 삭제하지 않는다. 필요하면 `fixture 비활성화` 또는 개별 활성 상태 변경으로 비활성화한다.
