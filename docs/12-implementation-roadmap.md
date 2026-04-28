# 12. 구현 로드맵

## Phase 0. 문서화 및 방향 확정

목표:

- 프로젝트 방향 확정
- MVP 범위 확정
- 도메인 스케치
- API 초안
- UI 계획
- Codex 작업 지시서 준비

산출물:

- docs 폴더
- PRD
- MVP scope
- domain sketch
- API spec
- Codex prompts

## Phase 1. 프로젝트 부트스트랩

목표:

- 루트 모노레포 구조 생성
- Spring Boot 기본 구조
- Next.js 기본 구조
- Docker 개발 환경
- Health API
- 공통 응답/예외
- 기본 라우팅

완료 기준:

- backend build 성공
- frontend build 성공
- postgres/redis 실행 가능
- `/api/v1/health` 응답
- 메인 페이지 접속 가능

## Phase 2. 백엔드 도메인 스켈레톤

목표:

- 엔티티 생성
- enum 생성
- repository 생성
- 기본 service/controller 골격
- seed 데이터

완료 기준:

- Category/Tag seed 등록
- Brand/Benefit 엔티티 생성
- UserReport 엔티티 생성
- 빌드 성공

## Phase 3. 공개 조회 API

목표:

- 브랜드 목록 API
- 브랜드 상세 API
- 카테고리별 혜택 API
- 태그별 혜택 API
- 제보 등록 API
- 조회수 기록 API

완료 기준:

- 더미 또는 seed 데이터로 공개 페이지 표시 가능
- PUBLISHED 상태만 공개
- 필터 동작

## Phase 4. 관리자 CRUD

목표:

- 관리자 브랜드 CRUD
- 관리자 혜택 CRUD
- 출처 관리
- 태그 연결
- 검수 상태 변경
- 제보 관리

완료 기준:

- 관리자에서 브랜드/혜택 등록 가능
- 혜택 상태 변경 가능
- VerificationLog 기록
- 제보 처리 가능

## Phase 5. 프론트 API 연결

목표:

- 메인 페이지 데이터 연결
- 브랜드 목록 데이터 연결
- 브랜드 상세 데이터 연결
- 카테고리/태그 페이지 연결
- 제보 폼 연결
- 관리자 화면 기본 연결

완료 기준:

- 실제 DB 데이터가 화면에 표시
- 제보 등록 가능
- 필터 UI 동작

## Phase 6. SEO 기본 적용

목표:

- 동적 metadata
- sitemap.xml
- robots.txt
- canonical URL
- OpenGraph
- Google Search Console 준비
- Naver Search Advisor 준비

완료 기준:

- 브랜드 상세 title/description 동적 적용
- 카테고리/태그 페이지 title/description 적용
- 사이트맵 생성
- 검색 엔진 등록 가능

## Phase 7. 초기 데이터 수집

목표:

- 공식 출처 기준 브랜드 30개 수집
- 스프레드시트 정리
- 관리자 등록
- 최근 확인일 입력
- 공식 출처 링크 입력

완료 기준:

- 브랜드 20개 이상 공개
- 공식 출처가 있는 혜택만 공개
- 불확실한 데이터는 NEEDS_CHECK 상태

## Phase 8. 배포

목표:

- EC2 배포
- Nginx 설정
- PostgreSQL/Redis 운영 설정
- 도메인 연결
- HTTPS
- Search Console 등록

완료 기준:

- 운영 URL 접속 가능
- Health check 가능
- sitemap 제출
- 기본 페이지 인덱싱 요청

## Phase 9. 운영 실험

목표:

- 3개월 운영
- 검색 유입 확인
- Search Console 분석
- 인기 키워드 확인
- 제보 처리
- 오래된 데이터 재검수

운영 루틴:

- 주 1회: 조회수 상위 브랜드 2~3개 재확인
- 월 1회: 오래된 데이터 10개 재검수
- 분기 1회: 브랜드 10개 추가
- 상시: 제보 처리

## Phase 10. 확장

후보:

- 신규 가입 혜택
- 앱 설치 혜택
- 첫 구매 혜택
- 멤버십 등급 혜택
- 시즌 혜택

확장 조건:

- 생일 혜택 페이지가 30개 이상 인덱싱됨
- Search Console 유입 데이터 확인
- 운영 피로도가 감당 가능함
- 기존 데이터 구조로 확장 가능함
