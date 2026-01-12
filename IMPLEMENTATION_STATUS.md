# StateTrail 구현 현황 및 향후 계획

## 현재 구현 상태 (2025-01-12)

### ✅ 구현 완료된 기능

1. **프로젝트 관리**
   - `ProjectEntity`, `ProjectController`
   - 프로젝트 생성·조회·수정
   - allowlist 설정 저장 및 검증

2. **권한 프로필 관리**
   - `AuthProfileEntity`, `AuthProfileController`
   - 프로젝트별 AuthProfile 저장 (이름·타입·태그)

3. **크롤링 실행 및 이벤트 스트림**
   - `CrawlRunEntity` 및 크롤링 실행 등록
   - `WebCrawlerService`에서 BFS/MCS 기반 링크 크롤링
   - `CrawlRunEventsController` SSE를 통한 실시간 이벤트 전송
   - 브라우저 모드 지원 (Playwright)
   - URL 패턴 기반 노드 그룹화

4. **그래프 조회**
   - `GraphController`, `GraphInspectorController`
   - `CrawlPageEntity`/`CrawlLinkEntity`를 노드/엣지로 반환
   - HTML 스냅샷 및 기본 정보 제공

5. **기반 구조**
   - JPA 레포지토리
   - SSE 이벤트 허브
   - Docker Compose (PostgreSQL, Redis, MinIO)

### ⚠️ MVP 스펙 대비 누락된 기능

#### 1. 상태/행동 그래프 모델
- ❌ **현재**: URL 단위 노드, 하이퍼링크 엣지
- ✅ **필요**: 
  - State 개념 (UI 시그니처, DOM 해시, CTA 리스트, 폼 필드)
  - 다양한 Action 타입 (CLICK, INPUT, SUBMIT, NAVIGATE)
  - nodeKey 정규화 (URL + authContext + UI 시그니처)
  - 스크린샷/trace 저장

#### 2. 로그인 컨텍스트 처리
- ❌ **현재**: AuthProfile 타입만 저장, 스토리지 업로드/스크립트 등록 API 없음
- ✅ **필요**:
  - Storage state 업로드 API
  - 로그인 스크립트 등록/수정 API
  - 워커에서 auth 컨텍스트 주입
  - 권한별 그래프 분석

#### 3. 탐색 예산 및 커버리지
- ⚠️ **현재**: 기본 예산 파라미터 있음 (노드/엣지/시간/depth)
- ✅ **필요**:
  - 예산 파라미터 UI 설정
  - 상태 변화 기반 탐색 종료 조건
  - 커버리지 목표 설정

#### 4. 그래프 시각화 데이터
- ❌ **현재**: ID, URL, 제목, depth만 반환
- ✅ **필요**:
  - 스크린샷 썸네일 presigned URL
  - UI 시그니처 요약 (CTA, forms)
  - 리스크 태그
  - FE 필터링/검색용 메타데이터

#### 5. 플로우 추출 및 테스트 생성
- ❌ **현재**: Dummy 플로우만 생성, 단순 URL 검증 코드
- ✅ **필요**:
  - 그래프 마이닝 기반 플로우 자동 추출
  - 결정론적 테스트 코드 생성
  - 네트워크/UI 오라클 적용
  - 테스트 실행/리포트 엔티티

#### 6. 증거물 저장
- ❌ **현재**: HTML 스냅샷만 저장
- ✅ **필요**:
  - Playwright trace/HAR/console log 저장
  - Artifact 엔티티
  - 증거물 연결 구조

#### 7. 롤 그래프 diff 및 throttling
- ❌ **현재**: 없음
- ✅ **필요**:
  - 여러 권한의 그래프 diff 계산
  - throttle 태그 및 저속 네트워크 프로파일 적용

## 향후 구현 계획

### Phase 1: 상태·행동 그래프로 확장 (우선순위: 높음)

**목표**: 페이지 그래프 → 상태/행동 그래프로 전환

**작업 항목**:

1. **엔티티 재설계**
   - `CrawlPageEntity` → `StateNodeEntity`
     - `nodeKey`: URL + authContext + UI 시그니처 해시
     - `screenshotObjectKey`, `networkLogObjectKey` 추가
     - UI 시그니처 JSON 저장 (CTA 리스트, form 필드, DOM 해시)
   - `CrawlLinkEntity` → `ActionEdgeEntity`
     - `actionType`: CLICK/INPUT/SUBMIT/NAVIGATE
     - `locator`, `payload`, `riskTags` JSON 저장
     - `httpEvidence` JSON 저장

2. **Playwright 기반 탐색기 강화**
   - 사용자 행동 감지 (버튼 클릭, 폼 입력, 링크 클릭)
   - 네트워크 요청/응답 로그 수집
   - 스크린샷 자동 캡처
   - 예산 및 커버리지 목표에 따른 탐색 종료

3. **AuthProfile 확장**
   - Storage state 업로드 API
   - 로그인 스크립트 등록/수정 API
   - 워커에서 auth 컨텍스트 주입

**예상 작업 시간**: 2-3주

### Phase 2: 그래프 API 및 이벤트 개선 (우선순위: 높음)

**목표**: FE에서 그래프 시각화 및 상호작용을 위한 풍부한 데이터 제공

**작업 항목**:

1. **GraphDTO 확장**
   - UI 시그니처 요약 (CTA, forms, 네비게이션 키)
   - 스크린샷 썸네일 presigned URL
   - 리스크 태그
   - 메타데이터 (필터링/검색용)

2. **GraphInspectorDTO 확장**
   - 스크린샷 원본 presigned URL
   - trace/HAR 다운로드 링크
   - 네트워크 요약 정보

3. **이벤트 타입 확장**
   - NODE_CREATED, EDGE_CREATED 이벤트
   - 액션 타입 및 태그 정보 포함

**예상 작업 시간**: 1주

### Phase 3: 플로우 마이닝 및 테스트 관리 (우선순위: 중간)

**목표**: 그래프에서 플로우 자동 추출 및 테스트 코드 생성

**작업 항목**:

1. **FlowMiner 모듈 구현**
   - 최단경로 기반 smoke 플로우 추출
   - 엣지 커버리지 플로우 추출
   - `FlowEntity.steps`에 actionEdge ID 저장

2. **FlowController 확장**
   - 플로우 수정/삭제/조회 API
   - 플로우 편집 기능

3. **테스트 관리 엔티티**
   - `TestSuite`, `TestRun` 엔티티
   - 테스트 코드 생성 (템플릿 기반 결정론적)
   - 네트워크/UI 오라클 적용
   - 테스트 실행 및 결과 저장

4. **Playwright 테스트 실행 워커**
   - 플로우 기반 테스트 실행
   - 실패 시 스크린샷/trace 저장

**예상 작업 시간**: 3-4주

### Phase 4: 증거물 저장 및 객체 스토리지 연동 (우선순위: 중간)

**목표**: 스크린샷, trace, HAR 등 증거물 관리

**작업 항목**:

1. **Artifact 엔티티 추가**
   - object key 및 메타데이터 저장
   - presigned URL 발급 API

2. **MinIO/S3 클라이언트 구성**
   - 증거물 업로드/다운로드
   - 삭제/만료 정책

**예상 작업 시간**: 1주

### Phase 5: 권한 diff, throttling, triage (우선순위: 낮음)

**목표**: 권한별 그래프 비교 및 성능 테스트 지원

**작업 항목**:

1. **그래프 diff 기능**
   - 여러 AuthProfile의 run 결과 비교
   - 추가/누락된 state/edge 리포트

2. **Throttling 기능**
   - Flow/ActionEdge에 throttleCandidate 태그
   - 테스트 실행 시 네트워크 속도 인위적 감소

3. **향후 MAFT/LLM 통합 준비**
   - Locator 개선 제안
   - Assertion 후보 제안
   - 리뷰 인터페이스

**예상 작업 시간**: 2주

### Phase 6: 프론트엔드 고도화 (우선순위: 높음)

**목표**: 그래프 시각화 및 플로우 관리 UI 완성

**작업 항목**:

1. **그래프 화면 강화**
   - 필터 (액션 타입, 리스크 태그, 권한 프로필)
   - 검색 (URL/타이틀/CTA)
   - 부분 보기 (k-hop)
   - 인스펙터 (스크린샷, UI 시그니처, 네트워크 요약)

2. **플로우 편집 UI**
   - 스텝 이동/삭제
   - 태그 설정
   - throttle 설정
   - 테스트 코드 미리보기
   - 테스트 스위트 등록

3. **테스트 실행 결과 화면**
   - 실패 스텝 시각화
   - trace/HAR 뷰어
   - 네트워크 요약

**예상 작업 시간**: 3-4주

## 구현 우선순위 요약

### 즉시 시작 (Phase 1)
1. 상태·행동 그래프 모델로 전환
2. Playwright 기반 탐색기 강화
3. AuthProfile 확장

### 단기 (Phase 2 + Phase 6 일부)
4. 그래프 API 확장
5. FE 그래프 시각화 강화

### 중기 (Phase 3 + Phase 4)
6. 플로우 마이닝 및 테스트 관리
7. 증거물 저장 시스템

### 장기 (Phase 5)
8. 권한 diff 및 throttling
9. MAFT/LLM 통합

## 기술 스택 확장 필요사항

- **객체 스토리지**: MinIO (로컬), S3 (프로덕션)
- **이미지 처리**: 썸네일 생성 라이브러리
- **네트워크 분석**: HAR 파서
- **테스트 프레임워크**: Playwright Test (이미 사용 중)

## 참고사항

- 현재 구현은 MVP 스펙의 약 30-40% 수준
- 가장 중요한 누락 기능: 상태/행동 그래프 모델
- 다음 주요 마일스톤: Phase 1 완료 후 MVP의 60-70% 달성 가능

