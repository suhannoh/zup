# 18. MVP E2E 체크리스트

## 1. 목적

이 문서는 Zup MVP 핵심 흐름을 로컬에서 끝까지 점검하기 위한 체크리스트다.

검증 대상 흐름:

```text
관리자 로그인
→ 브랜드 생성
→ 혜택 생성
→ 공식 출처 등록
→ 태그 연결
→ PUBLISHED 상태 변경
→ 사용자 브랜드 상세에서 혜택 노출 확인
→ 사용자 제보 생성
→ 관련 혜택 NEEDS_CHECK 자동 전환
→ VerificationLog 생성 확인
→ 관리자 제보 상태 RESOLVED 처리
```

## 2. 사전 조건

- `.env` 또는 실행 환경에 관리자 계정이 설정되어 있다.
- local profile 기준 기본 계정:
  - email: `admin@zup.local`
  - password: `admin1234`
- PostgreSQL, Redis가 실행 중이다.
- backend와 frontend가 로컬에서 실행 가능하다.

## 3. 시나리오 1: 기본 실행 확인

1. 개발용 인프라를 실행한다.

```powershell
docker compose -f docker-compose.dev.yml up -d
```

2. 백엔드를 실행한다.

```powershell
cd backend
.\gradlew.bat bootRun
```

3. 프론트엔드를 실행한다.

```powershell
cd frontend
npm run dev
```

4. Health API를 확인한다.

```http
GET http://localhost:8080/api/v1/health
```

기대 결과:

- HTTP 200
- `success=true`
- `message=healthy`

5. 사용자 메인 화면에 접속한다.

```text
http://localhost:3000
```

기대 결과:

- 메인 화면이 깨지지 않는다.
- 브랜드, 카테고리, 태그 영역이 표시된다.

## 4. 시나리오 2: 관리자 로그인

1. 관리자 로그인 화면에 접속한다.

```text
http://localhost:3000/admin/login
```

2. local 관리자 계정으로 로그인한다.

```text
admin@zup.local / admin1234
```

3. 로그인 성공 후 `/admin`으로 이동하는지 확인한다.
4. 관리자 대시보드 카운트가 표시되는지 확인한다.

기대 결과:

- 로그인 실패 시 오류 메시지가 표시된다.
- 로그인 성공 시 Admin API 요청에 `Authorization: Bearer {token}`이 포함된다.
- token 없이 `/api/v1/admin/**`를 호출하면 401이 반환된다.

## 5. 시나리오 3: 브랜드 등록

1. `/admin/brands`에 접속한다.
2. 브랜드 등록 폼을 연다.
3. 카테고리를 선택한다.
4. 브랜드명을 입력한다.
5. 영문 소문자, 숫자, 하이픈으로 된 slug를 입력한다.
6. 공식 홈페이지 URL을 입력한다.
7. 활성 상태를 확인한다.
8. 저장한다.
9. 브랜드 목록에 표시되는지 확인한다.

체크 포인트:

- 브랜드명과 slug가 중복되지 않는다.
- 카테고리가 정확하다.
- 공식 URL이 실제 공식 채널이다.
- 활성 상태가 의도한 값이다.

## 6. 시나리오 4: 혜택 등록

1. `/admin/benefits`에 접속한다.
2. 혜택 등록 폼을 연다.
3. 브랜드를 선택한다.
4. 혜택명과 요약을 입력한다.
5. 혜택 유형을 선택한다.
6. `occasionType`은 MVP 기준 `BIRTHDAY`로 둔다.
7. 생일 사용 기간 유형을 선택한다.
8. 앱 필요 여부, 멤버십 필요 여부, 구매 조건 여부를 입력한다.
9. 공식 출처 확인 전이면 `DRAFT` 상태로 저장한다.
10. 혜택 목록에 표시되는지 확인한다.

체크 포인트:

- 공식 출처가 없으면 `PUBLISHED`로 저장하지 않는다.
- 조건이 모호하면 `DRAFT` 또는 `NEEDS_CHECK`로 둔다.

## 7. 시나리오 5: 출처/태그/검수 이력

1. 혜택 카드에서 `/admin/benefits/{id}` 상세 운영 화면으로 이동한다.
2. 공식 출처를 등록한다.
   - 출처 유형: 공식 홈페이지, 공식 멤버십, 공식 FAQ 등
   - URL
   - 확인일
   - 메모
3. 태그를 연결한다.
   - 무료
   - 앱 필요
   - 생일월
   - 공식 확인 등
4. `/admin/benefits`로 돌아간다.
5. 혜택 검수 상태를 `PUBLISHED`로 변경한다.
6. `lastVerifiedAt`과 검수 메모를 입력한다.
7. `/admin/benefits/{id}`에서 VerificationLog가 생성되었는지 확인한다.

체크 포인트:

- 공식 출처가 최소 1개 이상 있다.
- 태그가 실제 혜택 조건과 일치한다.
- `DRAFT → PUBLISHED` 상태 변경 로그가 남는다.

## 8. 시나리오 6: 사용자 화면 노출

1. `/brands/{slug}` 사용자 화면에 접속한다.
2. `PUBLISHED + isActive=true` 혜택이 노출되는지 확인한다.
3. 공식 출처 링크가 표시되는지 확인한다.
4. 최근 확인일이 표시되는지 확인한다.

체크 포인트:

- `PUBLISHED` 상태가 아닌 혜택은 사용자 화면에 노출되지 않는다.
- 비활성 혜택은 사용자 화면에 노출되지 않는다.
- 브랜드가 비활성 상태라면 사용자 노출 여부를 별도로 확인한다.

## 9. 시나리오 7: 사용자 제보

1. 제보 화면에 접속한다.

```text
/reports/new?brandId={brandId}&benefitId={benefitId}
```

2. `WRONG_INFO` 유형으로 제보를 제출한다.
3. `/admin/reports`에서 제보가 표시되는지 확인한다.
4. 관련 혜택이 `NEEDS_CHECK`로 전환되었는지 확인한다.
5. `/admin/benefits/{id}`에서 VerificationLog를 확인한다.
6. 사용자 제보 기반 전환 이력이 남았는지 확인한다.
7. `/admin/reports`에서 제보 상태를 `RESOLVED`로 변경한다.
8. `adminMemo`가 저장되는지 확인한다.

체크 포인트:

- `WRONG_INFO`, `BENEFIT_ENDED`, `CONDITION_CHANGED`, `OFFICIAL_LINK_FOUND`는 관련 혜택을 `NEEDS_CHECK`로 전환한다.
- 이미 `NEEDS_CHECK` 상태인 혜택은 중복 VerificationLog를 생성하지 않는다.
- 제보 상태를 `RESOLVED`로 바꾸면 `resolvedAt`이 세팅된다.

## 10. 완료 기준

아래 항목이 모두 통과하면 MVP 핵심 흐름이 정상 동작한다고 판단한다.

- 관리자 로그인이 된다.
- Admin API가 token 없이 401을 반환한다.
- 브랜드가 생성된다.
- 혜택이 생성된다.
- 공식 출처가 등록된다.
- 태그가 연결된다.
- 혜택이 `PUBLISHED`로 변경된다.
- 사용자 브랜드 상세에서 혜택이 보인다.
- 사용자 제보가 생성된다.
- 제보 후 혜택이 `NEEDS_CHECK`로 전환된다.
- VerificationLog가 생성된다.
- 관리자 제보 상태가 `RESOLVED`로 변경된다.
