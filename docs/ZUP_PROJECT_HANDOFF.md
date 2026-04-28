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

## 다음 단계

- 실제 공식 출처 기반 데이터 입력
- 관리자 인증
- 배포 환경 구성
- Search Console 등록
- Naver Search Advisor 등록

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

- 사용자 회원가입/로그인 MVP 추가 금지
- 사용자 생일 입력 기반 개인화 MVP 추가 금지
- AI 추천 추가 금지
- WebSocket 추가 금지
- 알림 기능 추가 금지
- 자동 크롤링과 자동 게시 추가 금지
- 브랜드 로고 무단 사용 금지
- 공식 확인되지 않은 혜택 게시 금지
