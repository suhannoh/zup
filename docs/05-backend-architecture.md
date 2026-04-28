# 05. 백엔드 아키텍처

## 1. 백엔드 목표

백엔드는 단순 CRUD 서버가 아니라 **혜택 정보의 신뢰도와 최신성을 운영할 수 있는 구조**를 목표로 한다.

핵심은 다음이다.

- 브랜드/혜택 데이터 구조화
- 공식 출처 URL 관리
- 최근 확인일 관리
- 검수 상태 관리
- 사용자 제보 처리
- 조회수 기반 인기 데이터 준비
- SEO 페이지를 위한 조회 API 제공
- 추후 공식 출처 변경 감지 기능 확장

## 2. 기술 스택

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- PostgreSQL
- Redis
- Gradle

## 3. 패키지 구조

```text
com.noh.zup
├── ZupApplication.java
├── common
│   ├── response
│   ├── exception
│   └── config
├── health
├── domain
│   ├── category
│   ├── brand
│   ├── benefit
│   ├── source
│   ├── tag
│   ├── report
│   ├── verification
│   ├── pageview
│   └── admin
└── global
```

## 4. 계층 구조

기본 계층:

```text
Controller
→ Service
→ Repository
→ Entity
```

DTO는 요청/응답을 분리한다.

권장 구조:

```text
brand/
├── Brand.java
├── BrandRepository.java
├── BrandService.java
├── BrandController.java
├── dto/
└── admin/
```

관리자 API와 공개 API는 controller를 분리한다.

예:

```text
BrandController
AdminBrandController
```

## 5. 공통 응답

모든 응답은 ApiResponse로 감싼다.

```json
{
  "success": true,
  "data": {},
  "message": "ok"
}
```

## 6. 공통 예외

기본 구성:

- BusinessException
- ErrorCode
- GlobalExceptionHandler

기본 ErrorCode:

- INTERNAL_SERVER_ERROR
- INVALID_REQUEST
- NOT_FOUND
- UNAUTHORIZED
- FORBIDDEN

## 7. 보안 구조

MVP 초기:

- 공개 API permitAll
- 관리자 API도 개발 중에는 임시 permitAll 가능
- 관리자 인증은 별도 작업에서 적용

최종 MVP:

- 관리자 로그인
- 관리자 API 인증 필요
- 일반 사용자는 로그인 없음

Security 기본:

- CSRF disabled
- CORS enabled
- Stateless session
- `/api/v1/**` 공개
- `/api/v1/admin/**` 추후 인증 적용

## 8. 데이터 신뢰도 관리

Benefit은 검수 상태를 가진다.

```text
DRAFT
NEEDS_CHECK
VERIFIED
PUBLISHED
EXPIRED
HIDDEN
```

상태 변경은 VerificationLog로 기록한다.

사용자에게 노출되는 데이터는 기본적으로 `PUBLISHED`만 사용한다.

단, 관리자에서는 모든 상태를 조회할 수 있다.

## 9. 공식 출처 관리

BenefitSource는 Benefit과 분리한다.

이유:

- 하나의 혜택이 여러 출처를 가질 수 있다.
- 공식 출처와 참고 출처를 구분해야 한다.
- sourceCheckedAt을 별도로 관리해야 한다.
- 추후 contentHash 기반 변경 감지에 활용할 수 있다.

## 10. 사용자 제보 처리

사용자 제보는 UserReport로 저장한다.

제보가 들어와도 자동 반영하지 않는다.

권장 흐름:

```text
사용자 제보 등록
→ ReportStatus RECEIVED
→ 관리자 확인
→ 관련 Benefit NEEDS_CHECK 전환 가능
→ 수정 또는 반려
→ RESOLVED / REJECTED
```

## 11. 조회수 집계

PageViewDaily로 일 단위 조회수를 저장한다.

대상:

- BRAND
- BENEFIT
- CATEGORY
- TAG

MVP에서는 단순 증가 방식으로 충분하다.

추후 Redis로 버퍼링 후 batch flush 가능.

## 12. Redis 사용 방향

MVP에서 Redis는 필수는 아니지만 아래에 사용 가능하다.

- 조회수 증가 버퍼
- 인기 브랜드 캐시
- 카테고리/태그 목록 캐시
- 관리자 세션 또는 JWT blocklist
- 추후 공식 출처 변경 감지 lock

과도한 Redis 사용은 피한다.

## 13. 배치/스케줄러 방향

MVP 이후 적용:

- 최근 확인일 90일 초과 혜택 목록 생성
- 조회수 기반 인기 브랜드 계산
- sitemap 갱신
- 공식 출처 변경 감지

기존 `놓치면 알려줘` 프로젝트의 구조를 추후 재사용 가능하다.

재사용 가능한 패턴:

- HtmlFetcher
- TextExtractor
- Snapshot hash
- Diff
- Scheduler
- Redis Lock
- failureCount/backoff

단, 이 프로젝트에서는 사용자 알림이 아니라 **관리자 검수 큐 생성**에 사용한다.

## 14. API 설계 원칙

- 공개 API는 SEO 페이지 렌더링에 필요한 데이터 중심
- 관리자 API는 운영 편의 중심
- 사용자 개인 정보 최소 수집
- 제보 이메일은 선택 입력
- 공개 API에서는 비공개/검수중 혜택 노출 금지

## 15. 초기 개발 우선순위

1. Health API
2. 공통 응답/예외
3. Category/Tag seed
4. Brand CRUD
5. Benefit CRUD
6. Brand public list/detail
7. UserReport create
8. Admin review list
9. PageViewDaily
10. Sitemap
