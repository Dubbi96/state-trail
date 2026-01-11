아래는 제가 생각하는 이 서비스의 “핵심 아이디어(Concept Pillars)”와, 사용자가 실제로 이 플랫폼을 어떻게 쓰게 되는지 상상 가능한 “사용 흐름(End-to-End Flow)”입니다. 앞서 합의한 핵심 정책(상태/행동 그래프, 예산 기반 탐색, 멀티 권한 그래프+diff, evidence-first, 결정론적 테스트 생성, MAFT는 보조, 구간 태깅 throttling)을 전제로 합니다.

⸻

1) 핵심 아이디어 나열 (서비스를 설명하는 ‘기둥’)

I1. URL 크롤러가 아니라 “UI 상태공간(State Space) 컴파일러”

이 서비스는 웹을 “페이지 리스트”로 수집하는 도구가 아니라, 특정 도메인의 UI를 상태(State)와 행동(Action)으로 컴파일해서 “사용 가능한 흐름(Flow)”을 구조화합니다.
결과물은 상태/행동 그래프 = 제품의 실제 동작 모델입니다.

I2. “권한별 제품 실체”를 드러내는 Role Graph + Diff

동일 제품이라도 권한/테넌트에 따라 “보이는 화면”과 “가능 행동”이 달라집니다.
이 서비스는 탐색을 권한 프로필로 분리하고, 어떤 기능이 누구에게만 열려 있는지를 그래프 차이로 보여줍니다.
즉, “권한이 바뀌면 제품이 어떻게 달라지는가”를 자동으로 가시화합니다.

I3. Evidence-first: 그래프/테스트의 모든 주장은 증거를 가진다

각 노드/엣지에는 스크린샷, 트레이스, HAR, 네트워크 요약이 붙습니다.
그래프는 “추측”이 아니라 재현 가능한 기록이며, 테스트 실패도 동일하게 재현 증거를 제공합니다.
(이 지점이 내부 설득/반박 방지에 실질적인 가치)

I4. 그래프에서 “테스트 가능한 플로우”를 뽑아내는 Flow Miner

그래프는 보기용이 아니라, 테스트 생산의 원천입니다.
서비스는 그래프에서 자동으로:
	•	smoke 플로우(최단 경로 기반)
	•	핵심 기능 플로우(리스크 태그 기반)
	•	엣지 커버리지 플로우(행동 다양성 확보)
를 추출해 **“테스트 후보 세트”**로 만듭니다.

I5. 테스트 코드는 “결정론적(재생산 가능)”으로 생성되고, AI는 보조로만 쓴다

핵심은 템플릿 기반 생성입니다. 같은 flow면 같은 코드가 나옵니다.
AI/MAFT는:
	•	locator가 불안정할 때 개선 제안
	•	오라클(판정 기준) 후보 제안
	•	실패 triage
	•	커버리지 갭 메우기
같은 “보강”에만 사용합니다.
즉, 테스트의 중심은 엔지니어링, AI는 생산성 레버입니다.

I6. “구간 지정” 네트워크 저하(Throttling) 검증을 1급 기능으로

결제, 로그인, 업로드 등 네트워크 민감 구간을 엣지/플로우에 태깅하고, 그 구간에만 느린 네트워크 프로파일을 적용합니다.
따라서 “전체 테스트가 느려지는 운영 지옥” 없이, 실제 리스크 구간만 탄력성 검증합니다.

I7. 일회성 스캐너가 아니라 “지속 운영”되는 품질 감시 시스템

그래프 기반 테스트는 계속 돌고, 결과는 시간축으로 쌓입니다.
변경이 들어오면:
	•	그래프 diff가 변화를 알려주고
	•	기존 테스트가 깨졌는지 증거와 함께 보여주며
	•	새로 생긴 경로를 테스트 후보로 추천합니다.
즉, 제품의 기능 변화와 품질 상태를 지속적으로 추적합니다.

⸻

2) 이 서비스를 사용하는 Flow (사용자가 상상할 수 있게)

아래는 실제 사용 시나리오를 “한 번의 도입 → 운영”까지 순서대로 그린 흐름입니다.

Flow A. 처음 도입(온보딩) — “제품 지도를 만든다”
	1.	사용자가 프로젝트 생성

	•	base URL, allowlist(도메인/경로), 탐색 예산 프리셋 선택

	2.	권한 프로필 등록

	•	ADMIN/USER 등 2개 이상
	•	storageState 업로드 또는 로그인 스크립트 등록

	3.	“탐색 실행(Crawl Run)” 클릭

	•	각 권한별로 탐색이 진행되고, 진행 상황이 실시간으로 올라옴

	4.	결과: 권한별 그래프 생성

	•	그래프 화면에서 노드/엣지를 클릭하면 증거물(스크린샷/trace/요청요약) 확인 가능

	5.	권한 diff 확인

	•	“ADMIN에서만 보이는 화면/버튼”
	•	“USER에서 막히는 전이(403/redirect)”
이런 것이 자동으로 리스트업

이 단계 종료 시점의 산출물:
	•	“이 제품이 실제로 어떻게 동작하는지”를 설명할 수 있는 상태/행동 그래프
	•	권한별 제품 차이 보고서
	•	그래프의 각 주장에 대한 증거물

⸻

Flow B. 테스트 생산 — “그래프에서 테스트를 뽑아낸다”
	6.	“추천 플로우 생성(Auto Smoke)” 버튼 클릭

	•	start URL에서 핵심 화면까지 최단 경로 3~5개 자동 생성

	7.	플로우 검토/간단 편집

	•	Step 리스트에서 불필요한 행동 제거
	•	불안정한 step을 flag 처리(향후 locator 개선 대상)

	8.	“테스트 코드 생성” 클릭

	•	Playwright 코드(결정론적 템플릿 기반) 생성
	•	UI + 네트워크 오라클(최소 2종) 자동 포함

	9.	Export(zip) 또는 “TestSuite로 등록”

	•	팀의 레포에 넣거나
	•	플랫폼 내부에서 바로 실행 가능

이 단계 종료 시점의 산출물:
	•	재생산 가능한 E2E 테스트 코드 세트
	•	테스트 스위트(스모크/핵심 기능) 초안

⸻

Flow C. 운영 — “매일 돌면서 깨짐을 증거로 보고한다”
	10.	스케줄 실행(야간, PR마다 등)

	•	TestRun이 자동 생성되고 실행됨

	11.	실패 발생 시 리포트 확인

	•	어느 플로우의 어느 step에서 깨졌는지
	•	스크린샷/trace/HAR/네트워크 에러 요약으로 즉시 재현 가능
	•	(선택) MAFT가 “locator 개선/오라클 개선/플레키 가능성” 제안

	12.	새 배포 후 변화 감지

	•	새 crawl run을 돌리거나(주기적으로)
	•	그래프 diff로 “새로 생긴 화면/전이” 또는 “사라진 경로”를 탐지

	13.	변화에 맞춘 테스트 갱신 추천

	•	새로 추가된 경로를 커버하는 flow 후보 자동 제안
	•	기존 실패 테스트의 locator/대기조건 개선안 제안

이 단계의 핵심 가치:
	•	기능 변화와 품질 상태가 “누적된 증거”로 관리됨
	•	QA 인력이 적어도, 변경의 영향을 자동으로 좁혀줌

⸻

Flow D. Throttling(결제 등 민감 구간) — “그 구간만 느리게”
	14.	그래프에서 결제 관련 엣지/플로우에 태그 지정

	•	risk=PAYMENT, throttleCandidate=true

	15.	Throttling Test Run 실행

	•	전체 테스트가 아니라 “태그 구간에서만” 네트워크 저하 적용

	16.	결과 비교

	•	정상 네트워크 vs 저속 네트워크에서
	•	타임아웃 정책
	•	사용자 메시지(“네트워크가 불안정합니다”)
	•	재시도 UX
	•	결제 완료/중복 결제 방지
가 적절히 동작하는지 검증

⸻

3) 서비스가 한 문장으로 전달하는 메시지(포지셔닝)

“권한별 UI를 상태/행동 그래프로 컴파일하고, 그 그래프에서 테스트를 자동 생산·운영하며, 실패는 항상 증거로 재현 가능하게 만드는 ‘제품 품질 지도 시스템’.”

⸻

4) 대표 사용자 스토리(실무에서 이렇게 쓴다)
	•	“새 릴리즈 후 결제 화면에서 오류가 난다”
→ 결제 태그 플로우만 throttling 포함 재실행
→ 실패 step의 trace/HAR로 원인(타임아웃/리다이렉트/400/500)을 즉시 특정
	•	“ADMIN만 보이는 메뉴가 USER에게도 노출됐다”
→ 권한 diff에서 노드/엣지 차이가 역전된 걸 확인
→ 증거(스크린샷+네트워크)로 바로 이슈 등록 가능
	•	“새로운 기능이 추가됐는데 테스트가 없다”
→ 그래프 diff로 신규 경로 발견
→ 자동 플로우 추천으로 테스트 초안 생성

⸻

원하시면, 위 내용을 기반으로 “서비스 데모 시나리오(5분/10분)” 형태로 정리해 드릴 수 있습니다. 예:
	•	1분: 프로젝트/권한 등록
	•	2분: 탐색 실행 및 그래프 확인
	•	2분: 추천 플로우 → 테스트 생성
	•	2분: 실패 리포트/trace 확인
	•	3분: throttling 구간 테스트 시연

------
***아래는 “상태/행동 기반 그래프 탐색 → 플로우 추출 → E2E 코드 생성/실행 → 증거물(Trace/HAR/Screenshot) 리포팅”까지를 명확히 구현 가능한 MVP 스펙으로 정리한 문서입니다. 백엔드는 Spring Boot 기준으로 작성하되, FE는 구현자가 바로 화면을 만들 수 있을 정도로 상세하게 썼습니다.

⸻

0. MVP 범위 정의

MVP에서 “반드시” 되는 것
	1.	특정 도메인(start URL + allowlist)에서 로그인 컨텍스트별로 탐색 실행
	2.	탐색 결과를 State(화면 상태) 노드 / Action(행동) 엣지 그래프로 저장
	3.	FE에서 그래프를 시각화하고, **노드/엣지 상세(스크린샷/요청/응답/콘솔 로그/Trace 링크)**를 조회
	4.	그래프에서 **추천 플로우(최단 경로 기반 smoke 3~5개)**를 자동 생성
	5.	추천 플로우를 **Playwright 테스트 코드(템플릿 기반)**로 내보내기(export)
	6.	테스트 실행 및 결과 리포트(성공/실패 + 증거물 링크)

MVP에서 “제외(Phase 2)” 권장
	•	완전 자동 assertion 생성(LLM 기반)
	•	본격적인 throttling/네트워크 시뮬레이션(단, 훅/설계는 MVP에 심어둠)
	•	대규모 클러스터링/자동 요약 그래프(단, 필터/검색/부분 보기로 MVP에서 충분)

⸻

1. 전체 아키텍처 (MVP 권장)

구성요소
	•	BE API 서버 (Spring Boot)
	•	프로젝트/런/그래프/플로우/테스트 메타데이터 관리
	•	런 요청 생성 및 워커에게 작업 지시
	•	증거물(artifact) 메타 관리 + 다운로드 URL 제공
	•	Worker (Playwright 실행기)
	•	실제 브라우저 자동화로 탐색/테스트 실행
	•	결과(노드/엣지/로그/증거물)를 저장소에 기록
	•	상태 진행 상황을 BE로 업데이트
	•	DB (PostgreSQL)
	•	프로젝트/런/그래프 메타데이터 저장
	•	Object Storage (S3/GCS/MinIO 중 택1)
	•	스크린샷, HAR, Playwright trace(zip), HTML snapshot 등 저장
	•	Queue (Redis + BullMQ/혹은 Redis Stream / 혹은 RabbitMQ)
	•	MVP에서는 Redis Stream 권장(단순)
	•	FE (Next.js/React)
	•	프로젝트/런 관리 UI
	•	그래프 시각화/탐색 UI
	•	플로우 선정/수정 및 코드 생성/실행 UI
	•	리포트 UI

기술 선택 (현실적 MVP 조합)
	•	FE: Next.js 14(App Router) + TypeScript + Tailwind + React Query
	•	Graph 렌더링: Cytoscape.js(대형 그래프에 유리) 또는 React Flow(편집 친화)
	•	MVP는 “보기/필터/선택” 위주이므로 Cytoscape.js 추천
	•	BE: Spring Boot 3 + JPA + PostgreSQL
	•	Worker: Node.js + Playwright (가장 성숙/생태계 풍부)
	•	Spring Boot 내부에서 Playwright(Java)로도 가능하지만, MVP 운영/확장성은 Node 워커 분리가 안전

⸻

2. 데이터 모델 (DB 스키마: 핵심 테이블)

2.1 Project
	•	id (UUID)
	•	name (string)
	•	base_url (string)
	•	allowlist_rules (jsonb)
예: {"domains":["hogak.live"],"pathPrefixes":["/teams","/main"],"deny":["/logout"]}
	•	created_at, updated_at

2.2 AuthProfile (로그인/권한 컨텍스트)
	•	id (UUID)
	•	project_id (FK)
	•	name (string) 예: “ADMIN”, “USER”, “READONLY”
	•	type (enum)
	•	STORAGE_STATE : Playwright storageState.json 업로드 방식
	•	SCRIPT_LOGIN : 로그인 스크립트를 워커가 수행
	•	storage_state_object_key (string, nullable)
	•	login_script (text, nullable)
예: “go to /login → fill → submit → wait /main”
	•	tags (jsonb) 예: {"role":"ADMIN","tenant":"A"}
	•	created_at, updated_at

2.3 CrawlRun (탐색 실행)
	•	id (UUID)
	•	project_id (FK)
	•	auth_profile_id (FK)
	•	status (enum: QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELED)
	•	start_url (string)
	•	budget (jsonb)
예: {"maxNodes":500,"maxEdges":2000,"maxDepth":8,"maxMinutes":20,"maxActionsPerState":25}
	•	stats (jsonb)
예: {"nodes":123,"edges":456,"errors":7}
	•	started_at, finished_at
	•	error_message (text)

2.4 StateNode (화면 상태 노드)
	•	id (UUID)
	•	crawl_run_id (FK)
	•	node_key (string, unique per run)
	•	정규화된 해시(권한/라우트/UI 시그니처 기반)
	•	url (string)
	•	title (string)
	•	ui_signature (jsonb)
예: { "ctaTexts":["저장","결제"], "forms":["email","password"], "nav":["팀관리","예약"] }
	•	screenshot_object_key (string)
	•	trace_object_key (string, nullable) (상태 진입 시점 trace)
	•	created_at

2.5 ActionEdge (행동 전이 엣지)
	•	id (UUID)
	•	crawl_run_id (FK)
	•	from_node_id (FK StateNode)
	•	to_node_id (FK StateNode)
	•	action_type (enum: CLICK/NAVIGATE/INPUT/SUBMIT/SELECT/SCROLL)
	•	locator (jsonb)
예: { "strategy":"data-testid","value":"saveBtn" } or { "strategy":"css","value":"button:has-text('저장')"}
	•	action_payload (jsonb)
	•	입력값(민감정보 마스킹), 선택값 등
	•	http_evidence (jsonb)
	•	핵심 요청 목록 요약(엔드포인트/상태코드/에러코드)
	•	tags (jsonb)
	•	예: {"risk":"PAYMENT","throttleCandidate":true}
	•	created_at

2.6 Artifact (증거물 인덱스)
	•	id (UUID)
	•	run_id (UUID)  (crawl_run 또는 test_run)
	•	type (enum: SCREENSHOT/TRACE/HAR/CONSOLE_LOG/NETWORK_LOG/HTML_SNAPSHOT)
	•	object_key (string)
	•	meta (jsonb)
	•	created_at

2.7 Flow (추천/저장 플로우)
	•	id (UUID)
	•	project_id (FK)
	•	auth_profile_id (FK)
	•	name (string)
	•	source (enum: AUTO_SMOKE/AUTO_EDGE_COVERAGE/MANUAL)
	•	steps (jsonb)
	•	엣지 id 배열 또는 (node_key + action descriptor) 배열
	•	tags (jsonb) 예: {"suite":"smoke","risk":"payment"}
	•	created_at, updated_at

2.8 TestSuite / TestRun
	•	TestSuite: (id, project_id, auth_profile_id, name, flows(jsonb), created_at)
	•	TestRun: (id, test_suite_id, status, started_at, finished_at, stats(jsonb), report_object_key, error_message)

⸻

3. BE API 스펙 (REST 기준)

3.1 Project
	•	POST /api/projects
	•	GET /api/projects
	•	GET /api/projects/{projectId}
	•	PUT /api/projects/{projectId}

3.2 AuthProfile
	•	POST /api/projects/{projectId}/auth-profiles
	•	GET /api/projects/{projectId}/auth-profiles
	•	POST /api/auth-profiles/{authProfileId}/storage-state (파일 업로드, multipart)
	•	PUT /api/auth-profiles/{authProfileId}

3.3 CrawlRun (탐색)
	•	POST /api/projects/{projectId}/crawl-runs
	•	body: authProfileId, startUrl(optional), budget, explorationOptions
	•	GET /api/projects/{projectId}/crawl-runs
	•	GET /api/crawl-runs/{runId}
	•	POST /api/crawl-runs/{runId}/cancel

진행 상태 스트리밍(필수)
	•	옵션 A(권장): GET /api/crawl-runs/{runId}/events (SSE)
	•	옵션 B: WebSocket /ws/runs (메시지: runId 기반)

3.4 Graph 조회
	•	GET /api/crawl-runs/{runId}/graph
	•	response:
	•	nodes: id, nodeKey, url, title, screenshotThumbUrl, uiSignature summary
	•	edges: id, from, to, actionType, locatorSummary, tags
	•	GET /api/crawl-runs/{runId}/nodes/{nodeId}
	•	GET /api/crawl-runs/{runId}/edges/{edgeId}

3.5 Artifacts
	•	GET /api/artifacts/{artifactId}/download-url
	•	Presigned URL 반환(스토리지 직접 다운로드)

3.6 Flow (플로우 추천/저장)
	•	POST /api/crawl-runs/{runId}/flows/auto-smoke
	•	결과: 추천 flow 3~5개 생성
	•	POST /api/projects/{projectId}/flows
	•	GET /api/projects/{projectId}/flows
	•	GET /api/flows/{flowId}
	•	PUT /api/flows/{flowId}

3.7 Test 코드 생성/실행
	•	POST /api/flows/{flowId}/generate-test
	•	response: code(text), files(manifest), metadata
	•	POST /api/test-suites
	•	POST /api/test-suites/{suiteId}/run
	•	GET /api/test-runs/{runId}
	•	GET /api/test-runs/{runId}/artifacts

⸻

4. Worker(Playwright) MVP 동작 스펙

4.1 탐색(Explorer) 기본 알고리즘
	•	입력:
	•	startUrl
	•	allowlist rules
	•	auth profile (storageState or login script)
	•	budget
	•	루프:
	1.	현재 state에서 가능한 actions 후보 수집
	2.	actions 우선순위(“새로움”) 기준으로 선택
	3.	action 실행 → next state 도달
	4.	stateKey 생성(정규화) 후 노드/엣지 기록
	5.	budget 소진 또는 novelty 하락 시 종료
	•	수집 대상 evidence(최소):
	•	각 노드 진입 스크린샷 1장
	•	각 엣지 수행 시 핵심 네트워크 로그 요약(엔드포인트/상태코드)
	•	run 단위로 trace 1개 또는 노드/엣지 단위 trace (MVP는 run 단위 1개 권장)

4.2 “가능 actions” 수집 규칙(결정론적)
	•	클릭 후보:
	•	a/button/input[type=submit], role=button, [data-testid], [aria-label]
	•	화면 내 visible + enabled 조건
	•	입력 후보:
	•	input/textarea/select 중 visible한 것
	•	입력값은 “기본 전략”만 MVP에서 제공:
	•	email/password는 마스킹 + 사전 정의 값
	•	일반 텍스트는 “test”, 숫자는 “1”
	•	파괴적 필드/결제 필드는 기본 차단(태그로만 표시)

4.3 안정성(Flake 최소화) 규칙
	•	action 후 wait 조건:
	•	네트워크 idle + 특정 selector 등장/변화 감지
	•	SPA에서 URL 변화가 없을 수 있으니:
	•	DOM 시그니처 변화 감지로 state 전환 판정

⸻

5. FE MVP 스펙 (가장 중요)

5.1 FE 전제(사용자 관점 핵심 UX)
	•	사용자는 “자동으로 뽑힌 그래프”를 보고:
	1.	어떤 화면들이 존재하는지
	2.	어떤 버튼/행동으로 어디로 이동하는지
	3.	각 행동에서 서버 호출이 어떻게 발생하는지
	4.	바로 테스트로 만들 수 있는 플로우가 무엇인지
를 빠르게 이해해야 합니다.

따라서 FE는 “그래프를 멋있게”보다 필터/검색/선택 시 정보가 명확히 뜨는 구조가 최우선입니다.

⸻

5.2 화면 구성(라우트 단위)

(1) /projects

목적: 프로젝트 목록/생성
	•	좌측: Project List
	•	우측: “New Project” 폼
	•	name
	•	base_url
	•	allowlist rules 편집(JSON editor + 간단 UI 토글)
	•	CTA:
	•	Create Project
	•	Open Project

Acceptance
	•	프로젝트 생성 후 상세로 이동

⸻

(2) /projects/[id] (Project Dashboard)

목적: AuthProfile 관리 + CrawlRun 생성/리스트
레이아웃: 2단 탭
	•	탭 A: Auth Profiles
	•	탭 B: Crawl Runs
	•	탭 C(선택): Flows / Test Suites

탭 A: Auth Profiles
	•	카드 리스트(ADMIN/USER 등)
	•	각 카드:
	•	타입(StorageState / ScriptLogin)
	•	태그(role/tenant)
	•	버튼: Upload storageState / Edit script / Validate(선택)

업로드 UX
	•	“storageState.json 업로드” 드롭존
	•	업로드 완료 시 “마지막 업로드 시각” 표기

탭 B: Crawl Runs
	•	“New Run” 패널
	•	auth profile 선택 드롭다운
	•	startUrl(옵션, 기본 base_url)
	•	budget 프리셋:
	•	Small(100 nodes/5m)
	•	Medium(300 nodes/15m)
	•	Large(500 nodes/20m)
	•	Run 버튼
	•	Run 리스트 테이블
	•	status badge
	•	nodes/edges
	•	started/finished
	•	Open Graph 버튼

Acceptance
	•	Run 생성 → status가 실시간으로 업데이트(SSE/WS)

⸻

(3) /runs/[runId]/graph (Graph Explorer)

MVP 핵심 화면
레이아웃(권장 3패널):
	•	상단: Run status bar (progress, nodes/edges, elapsed, stop)
	•	좌측: Filter/Search 패널
	•	중앙: Graph Canvas
	•	우측: Inspector 패널(노드/엣지 상세)

좌측 Filter/Search 패널
	•	Search box (URL/title/CTA text 검색)
	•	Toggles:
	•	show only new nodes (최근 추가)
	•	hide low-degree nodes(고립/말단 숨김)
	•	show risk=payment edges(태그 기반)
	•	Dropdown:
	•	actionType filter
	•	status filter(에러 발생 노드만)
	•	“Focus mode”
	•	selected node 기준 k-hop(1~3) 보기

중앙 Graph Canvas (Cytoscape)
	•	기본 동작:
	•	노드 클릭 → 우측 inspector 노드 탭 오픈
	•	엣지 클릭 → 우측 inspector 엣지 탭 오픈
	•	드래그/줌
	•	표시 규칙(MVP)
	•	노드 라벨: title (없으면 path)
	•	엣지 라벨: CLICK/INPUT 등 액션 타입 + 요약(예: “저장 버튼”)
	•	성능:
	•	노드 300~500까지 끊기지 않도록
	•	큰 그래프는 “Focus mode”로 부분만 렌더 가능

우측 Inspector 패널
탭 2개: Node / Edge

Node 탭 내용
	•	Title, URL
	•	Screenshot preview(thumb) + “Open full” 버튼
	•	UI signature 요약(CTA 목록/폼 필드 목록)
	•	Node artifacts 링크(Trace/HAR/HTML)
	•	“Find paths” 기능
	•	Start node를 선택하면:
	•	“From startUrl to this node” 최단경로
	•	“From this node to …” 검색 대상 노드 최단경로

Edge 탭 내용
	•	actionType
	•	locator 요약(전략, 값)
	•	action payload(입력값은 마스킹)
	•	http evidence 테이블
	•	endpoint, method, status, error
	•	edge artifacts(해당 action 직후 스크린샷/trace segment 링크가 있으면 제공)
	•	태그 편집(선택): risk=payment, throttleCandidate=true 등

Acceptance
	•	사용자가 그래프에서 클릭만으로 “왜 이 화면으로 갔는지”를 증거물과 함께 확인 가능

⸻

(4) /runs/[runId]/flows (Flow Builder / 추천 플로우)

목적: 자동 추천 + 최소 편집
구성:
	•	“Generate Auto Smoke Flows” 버튼(3~5개 생성)
	•	Flow 리스트(카드)
	•	Flow name
	•	step count
	•	start/end node
	•	Generate Test 버튼
	•	Save Flow 버튼

Flow 상세
	•	좌측: Steps 리스트 (1..N)
	•	각 step 클릭 시 그래프에서 해당 edge highlight
	•	우측: Step detail (edge inspector 요약)
	•	간단 편집:
	•	step 제거
	•	step 순서 변경(드래그)
	•	특정 step을 “불안정” 표시(flag) (향후 locator 개선 대상으로)

Acceptance
	•	추천 플로우 생성 → 하나 선택 → 테스트 코드 생성까지 2~3클릭

⸻

(5) /flows/[flowId]/test (Test Code Preview & Export)

목적: 템플릿 코드 생성 결과를 보고 바로 사용
구성:
	•	상단: Run with this Flow(선택, MVP에서 가능하면 좋음)
	•	좌측: 파일 트리
	•	tests/flow_<id>.spec.ts
	•	pages/*.ts (선택: Page Object 기반이면)
	•	utils/wait.ts
	•	중앙: 코드 에디터(모나코 에디터)
	•	우측: 설정 패널
	•	baseUrl
	•	auth profile 선택
	•	timeouts
	•	evidence on/off(trace/har/screenshot)

버튼:
	•	Export zip (코드 다운로드)
	•	Create TestSuite(플로우를 스위트로 등록)

Acceptance
	•	코드 생성이 “결정론적”으로 재생산(같은 flow면 같은 코드)

⸻

(6) /test-runs/[runId] (Test Run Report)

목적: 실패 원인 확인/증거물 확인
구성:
	•	Summary 카드:
	•	total/passed/failed
	•	duration
	•	failed steps
	•	Failures 리스트:
	•	test name
	•	failed step index
	•	error message
	•	screenshot thumb
	•	“Open trace” (Playwright trace zip 다운로드 링크)
	•	Network summary(선택):
	•	5xx 목록
	•	가장 느린 요청 TOP N

Acceptance
	•	실패 시 개발팀 반박 방지 수준의 evidence(스크린샷/trace/요청요약) 제공

⸻

5.3 FE 공통 컴포넌트 스펙(구현 단위)

GraphCanvas
	•	props:
	•	nodes[], edges[]
	•	selectedNodeId, selectedEdgeId
	•	onNodeSelect, onEdgeSelect
	•	filters(searchText, toggles, kHop)
	•	기능:
	•	layout: cose 또는 breadthfirst (기본)
	•	highlight path: edge 리스트 입력 시 강조

InspectorPanel
	•	mode: node | edge
	•	NodeView:
	•	ScreenshotViewer(thumb + modal)
	•	UISignatureChips(CTA/폼 필드)
	•	ArtifactsList(presigned url)
	•	EdgeView:
	•	LocatorBadge
	•	HttpEvidenceTable
	•	TagEditor(MVP optional)

RunStatusBar
	•	SSE/WS로 진행률 반영
	•	Cancel 버튼

FlowStepsList
	•	drag & drop reorder
	•	step 클릭 시 graph highlight

CodeViewer
	•	Monaco editor
	•	export zip 버튼

⸻

5.4 FE 상태관리/통신
	•	React Query:
	•	projects, runs, graph, flows, testRuns 캐싱
	•	SSE 구독:
	•	run events: 상태/노드수/엣지수 업데이트
	•	UI는 “진행 중에도 그래프가 계속 확장”되는 느낌 제공 가능(선택)

⸻

6. MVP 산출물(개발 완료 기준)

기능 완료(필수)
	•	Project 생성/수정
	•	AuthProfile 생성 + storageState 업로드
	•	CrawlRun 생성/상태 추적 + 결과 그래프 조회
	•	Graph Explorer 화면(필터/검색/인스펙터/증거물 링크)
	•	Auto Smoke Flow 생성(3~5개)
	•	Flow → Playwright 테스트 코드 생성 + Export
	•	TestRun 실행 + 리포트 화면(증거물 링크)

운영/품질(필수)
	•	워커 실패 시 run 상태가 FAILED로 정리
	•	object storage에 남은 artifact 정합성(메타와 실물 링크 정상)
	•	민감정보 마스킹(입력값/토큰 등)

⸻

7. 개발 순서(권장 2~4주 MVP)
	1.	BE: Project/AuthProfile CRUD + 파일 업로드(스토리지)
	2.	Worker: storageState 기반 브라우저 띄우기 + 단순 탐색(노드/엣지 50개만) + 스크린샷 저장
	3.	BE: Graph 저장/조회 API + Artifact 다운로드 URL
	4.	FE: Project Dashboard + Run 생성/목록 + Run 상태 스트리밍
	5.	FE: Graph Explorer(선택/인스펙터/스크린샷)
	6.	BE/Worker: Auto smoke flow 생성(최단경로 기반)
	7.	Worker: flow 기반 Playwright 코드 생성 + export
	8.	FE: Flow/Code/Report 화면

⸻

8. Throttling(네트워크 저속) MVP에 심는 최소 설계 훅
	•	Edge.tags에 throttleCandidate=true를 저장 가능
	•	Test 실행 API에 옵션만 미리 정의:
	•	throttleProfile: "NONE" | "SLOW_3G" | "CUSTOM"
	•	CUSTOM은 Phase 2에서 bandwidth/latency/loss를 추가
	•	FE에서는 우측 Edge Inspector에 “Throttle candidate” 체크박스만 제공(실제 적용은 Phase 2)

⸻

9. 바로 착수 가능한 “MVP 화면 우선순위” (FE 기준)
	1.	Project Dashboard(프로젝트/런 생성/상태)
	2.	Graph Explorer(필터 + 인스펙터 + 스크린샷/trace 링크)
	3.	Flows(추천 생성 + step 리스트 + 테스트 생성 버튼)
	4.	Test Report(실패 증거물)

⸻

원하시면, 다음 단계로 **(1) 화면 와이어프레임(텍스트 기반) + (2) FE 컴포넌트 트리 + (3) GraphCanvas에서 사용할 nodes/edges DTO 샘플 + (4) Playwright 코드***


