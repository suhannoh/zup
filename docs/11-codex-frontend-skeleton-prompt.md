# 11. Codex 작업 지시서 03 — 프론트엔드 화면 스켈레톤 생성

## 목표

`docs/06-frontend-ui-plan.md`와 `docs/02-final-wireframe-prompt.md`를 읽고 Next.js 프론트엔드 화면 스켈레톤을 생성한다.

이번 작업은 디자인 완성이 아니라, 화면 구조와 라우트, API 연결 준비를 만드는 것이다.

## 작업 범위

- 기본 레이아웃
- Header/Footer
- 메인 페이지
- 브랜드 목록 페이지
- 브랜드 상세 페이지
- 카테고리 페이지
- 태그 페이지
- 제보 페이지
- 관리자 placeholder 페이지
- Axios client
- 기본 타입
- 기본 UI store

## 라우트

```text
/
 /brands
 /brands/[slug]
 /categories/[slug]
 /tags/[slug]
 /reports/new
 /admin
 /admin/brands
 /admin/benefits
 /admin/reports
```

## 메인 페이지 요구사항

카피:

```text
생일에 받을 수 있는 브랜드 혜택, 조건별로 한 번에 보기
```

보조 카피:

```text
무료 쿠폰, 앱 필요 여부, 멤버십 조건, 사용 기간까지 공식 출처 기준으로 정리합니다.
```

구성:

- 검색창
- 빠른 필터 버튼
- 인기 생일 혜택 placeholder
- 카테고리 바로가기
- 최근 업데이트 placeholder
- 안내 문구

## 브랜드 목록 페이지

구성:

- 검색창
- 필터 영역
- 정렬 dropdown placeholder
- 브랜드 카드 리스트 placeholder

브랜드 카드 표시:

- 브랜드명
- 카테고리
- 혜택 요약
- 태그 배지
- 최근 확인일
- 공식 확인 배지

## 브랜드 상세 페이지

구성:

- 브랜드명
- 혜택 요약
- 조건 배지
- 받는 방법
- 사용 가능 기간
- 공식 출처
- 최근 확인일
- 주의사항
- 제보 버튼
- 면책 문구

## 카테고리/태그 페이지

SEO용 제목과 설명이 있어야 한다.

예시:

```text
카페 생일 혜택 모음
무료 생일 혜택 모음
```

## 제보 페이지

폼 필드:

- 브랜드명
- 제보 유형
- 제보 내용
- 참고 링크
- 이메일 선택 입력

제보 유형:

- 정보가 틀렸어요
- 혜택이 종료됐어요
- 조건이 달라졌어요
- 새 혜택이 있어요
- 공식 링크를 찾았어요

## 관리자 페이지

placeholder만 만든다.

- 관리자 대시보드
- 브랜드 관리
- 혜택 관리
- 제보 관리

## API client

파일:

```text
src/lib/api/client.ts
```

환경 변수:

```text
NEXT_PUBLIC_API_BASE_URL
```

함수:

- getHealth
- getBrands placeholder
- getBrandBySlug placeholder
- createReport placeholder

## Zustand

파일:

```text
src/stores/useUiStore.ts
```

기능:

- isMobileMenuOpen
- openMobileMenu
- closeMobileMenu

## 디자인 방향

- 모바일 우선
- 정보 사이트 느낌
- 깔끔한 카드형 UI
- 과한 애니메이션 금지
- 브랜드 로고 대량 사용 금지
- 회원가입 유도 금지
- 광고 느낌 금지

## 주의사항

- 실제 API가 아직 완성되지 않았으면 더미 데이터 사용 가능
- 불필요한 복잡한 컴포넌트 금지
- AI 추천 UI 금지
- WebSocket 금지
- 쿠폰 직접 발급처럼 보이는 표현 금지

## 완료 보고

작업 완료 후 아래를 보고한다.

1. 생성/수정 파일
2. 구현한 라우트
3. 구현한 컴포넌트
4. API client 구성
5. 실행 방법
6. 빌드 결과
7. 다음 TODO
