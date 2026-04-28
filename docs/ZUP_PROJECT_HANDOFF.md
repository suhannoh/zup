# ZUP_PROJECT_HANDOFF

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

브랜드별 생일 쿠폰, 무료 혜택, 할인, 멤버십 혜택을 공식 출처 기준으로 수집하고, 사용자가 조건별로 빠르게 필터링해서 볼 수 있게 하는 검색 유입형 정보 큐레이션 웹서비스.

## 핵심 목표

- 개인 프로젝트 경험
- 실제 검색 유입 실험
- 운영형 백엔드 포트폴리오
- 애드센스/제휴 링크 수익화 가능성 실험
- 블로그보다 구조화된 생일 혜택 DB 구축

## 핵심 원칙

- 사용자는 로그인하지 않는다.
- 생일 입력을 강요하지 않는다.
- 브랜드 혜택은 공식 출처 기준으로 검수한다.
- 블로그/커뮤니티는 힌트로만 사용한다.
- 혜택마다 최근 확인일을 표시한다.
- 혜택마다 공식 출처 링크를 관리한다.
- 불확실한 정보는 게시하지 않는다.
- 자동 크롤링으로 바로 게시하지 않는다.
- 브랜드 로고는 MVP에서 사용하지 않는다.
- 생일 혜택에 집중하고, 확장은 나중에 한다.

## MVP 핵심 기능

### 사용자

- 브랜드 목록 조회
- 브랜드 상세 조회
- 카테고리별 혜택 조회
- 태그별 혜택 조회
- 조건 필터
- 정보 수정 제보
- 공식 출처 확인
- 최근 확인일 확인

### 관리자

- 브랜드 관리
- 혜택 관리
- 출처 관리
- 태그 관리
- 검수 상태 관리
- 제보 관리
- 오래된 데이터 확인
- 조회수 확인

## 핵심 도메인

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

## 핵심 상태

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

## 이전 프로젝트 재사용 가능성

기존 `놓치면 알려줘` 프로젝트의 변화 감지 구조는 추후 재사용 가능하다.

재사용 대상:

- HtmlFetcher
- TextExtractor
- Snapshot hash
- Diff
- Scheduler
- Redis Lock
- failureCount/backoff

단, 이 프로젝트에서는 사용자 알림이 아니라 **공식 출처 변경 감지와 관리자 검수 큐 생성**에 사용한다.

적용 시점:

- 1차 MVP 이후
- 브랜드/혜택/출처 CRUD 완료 후
- 공식 출처 URL이 충분히 쌓인 후

## Codex 작업 방식

Codex에게 긴 지시문을 매번 보내지 않는다.

매번 아래처럼 요청한다.

```md
docs 폴더의 문서를 먼저 읽어라.

이번 작업은 `docs/09-codex-bootstrap-prompt.md`만 수행한다.

MVP 범위를 벗어나지 말고, 작업 후 생성/수정 파일 목록과 빌드 결과를 보고하라.
```

다음 작업 문서:

- `docs/09-codex-bootstrap-prompt.md`
- `docs/10-codex-backend-skeleton-prompt.md`
- `docs/11-codex-frontend-skeleton-prompt.md`

## 금지 사항

- 사용자 회원가입부터 만들지 말 것
- AI 추천 넣지 말 것
- WebSocket 넣지 말 것
- 알림톡 넣지 말 것
- 자동 크롤링부터 만들지 말 것
- 브랜드 로고 대량 사용하지 말 것
- 처음부터 모든 혜택 포털로 확장하지 말 것
- 불필요한 샘플 게시판/Todo 만들지 말 것
