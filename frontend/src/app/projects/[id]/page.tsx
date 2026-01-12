"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { api } from "@/lib/api";

export default function ProjectDashboardPage() {
  const params = useParams<{ id: string }>();
  const projectId = params.id;
  const [crawlStrategy, setCrawlStrategy] = useState<"BFS" | "MCS" | "BROWSER_BFS" | "BROWSER_MCS">("BROWSER_MCS");
  const [capturingAuthProfileId, setCapturingAuthProfileId] = useState<string | null>(null);

  const projectQuery = useQuery({
    queryKey: ["projects", projectId],
    queryFn: () => api.projects.get(projectId)
  });

  const authProfilesQuery = useQuery({
    queryKey: ["auth-profiles", projectId],
    queryFn: () => api.authProfiles.list(projectId)
  });

  const runsQuery = useQuery({
    queryKey: ["crawl-runs", projectId],
    queryFn: () => api.crawlRuns.list(projectId)
  });

  return (
    <main className="space-y-6">
      <header className="space-y-2">
        <Link href="/projects" className="text-sm text-slate-600 hover:underline">
          ← Projects
        </Link>
        <h1 className="text-xl font-semibold">
          {projectQuery.data?.name ?? "Project"} <span className="text-slate-400">Dashboard</span>
        </h1>
        <p className="text-sm text-slate-600">
          Auth Profiles / Crawl Runs (MVP 스캐폴딩). 실제 UX는 `key_idea.md` 5.2를 기준으로 확장합니다.
        </p>
      </header>

      <section className="grid gap-4 md:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Auth Profiles</h2>
            <button
              className="rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50"
              onClick={async () => {
                await api.authProfiles.create(projectId, {
                  name: `ADMIN-${new Date().toISOString().slice(11, 19)}`,
                  type: "STORAGE_STATE",
                  tags: { role: "ADMIN" }
                });
                authProfilesQuery.refetch();
              }}
            >
              Add(임시)
            </button>
          </div>
          <div className="mt-3 space-y-2">
            {authProfilesQuery.data?.items.map((a) => (
              <div key={a.id} className="rounded-lg border border-slate-200 p-3">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-medium">{a.name}</div>
                    <div className="text-sm text-slate-600">{a.type}</div>
                  </div>
                  <button
                    className="rounded-md border border-red-200 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
                    onClick={async () => {
                      if (confirm(`정말 ${a.name}을(를) 삭제하시겠습니까?\n관련된 모든 Crawl Run도 함께 삭제됩니다.`)) {
                        try {
                          const result = await api.authProfiles.delete(projectId, a.id);
                          alert(result.message || "Auth Profile이 삭제되었습니다.");
                          authProfilesQuery.refetch();
                        } catch (err: any) {
                          alert(`삭제 실패: ${err.message || err}`);
                        }
                      }
                    }}
                  >
                    삭제
                  </button>
                </div>
                {a.type === "STORAGE_STATE" && (
                  <div className="mt-2 space-y-2">
                    {a.storageStateObjectKey ? (
                      <div className="rounded-md bg-green-50 border border-green-200 px-2 py-1.5 text-xs">
                        <div className="text-green-800 font-medium">✓ Storage State 저장됨</div>
                        <div className="text-green-600 mt-0.5">Object Key: {a.storageStateObjectKey}</div>
                      </div>
                    ) : (
                      <div className="rounded-md bg-yellow-50 border border-yellow-200 px-2 py-1.5 text-xs text-yellow-800">
                        ⚠ Storage State가 설정되지 않았습니다
                      </div>
                    )}
                    {capturingAuthProfileId === a.id ? (
                      <div>
                        <div className="mb-2 text-xs text-slate-600">
                          브라우저에서 로그인을 완료한 후 아래 버튼을 눌러주세요.
                        </div>
                        <button
                          className="rounded-md bg-green-600 px-3 py-1.5 text-xs text-white hover:bg-green-700"
                          onClick={async () => {
                            try {
                              const result = await api.authProfiles.completeCaptureStorageState(projectId, a.id);
                              alert(result.message);
                              setCapturingAuthProfileId(null);
                              authProfilesQuery.refetch();
                            } catch (err: any) {
                              alert(`완료 실패: ${err.message || err}`);
                            }
                          }}
                        >
                          완료 및 저장
                        </button>
                      </div>
                    ) : (
                      <>
                        <div>
                          <label className="block text-xs text-slate-600 mb-1">
                            Storage State 자동 캡처 (권장)
                          </label>
                          <button
                            className="rounded-md bg-blue-600 px-3 py-1.5 text-xs text-white hover:bg-blue-700"
                            onClick={async () => {
                              const loginUrl = projectQuery.data?.baseUrl ?? "https://teds-roasting.netlify.app/";
                              try {
                                const result = await api.authProfiles.captureStorageState(
                                  projectId,
                                  a.id,
                                  loginUrl
                                );
                                alert(result.message);
                                setCapturingAuthProfileId(a.id);
                              } catch (err: any) {
                                alert(`캡처 실패: ${err.message || err}`);
                              }
                            }}
                          >
                            브라우저 열기
                          </button>
                        </div>
                        <div>
                          <label className="block text-xs text-slate-600 mb-1">
                            또는 수동 업로드 (Playwright에서 추출한 .json 파일)
                          </label>
                          <input
                            type="file"
                            accept=".json"
                            className="text-xs"
                            onChange={async (e) => {
                              const file = e.target.files?.[0];
                              if (file) {
                                try {
                                  await api.authProfiles.uploadStorageState(projectId, a.id, file);
                                  authProfilesQuery.refetch();
                                  alert("Storage state 업로드 완료!");
                                } catch (err) {
                                  alert(`업로드 실패: ${err}`);
                                }
                                e.target.value = ""; // Reset input
                              }
                            }}
                          />
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Crawl Runs</h2>
          </div>
          <div className="mt-3 grid gap-2 md:grid-cols-[1fr_auto]">
            <div className="flex items-center gap-2">
              <label className="text-xs text-slate-600">Strategy</label>
              <select
                className="rounded-md border border-slate-200 px-2 py-2 text-sm"
                value={crawlStrategy}
                onChange={(e) => setCrawlStrategy(e.target.value as any)}
              >
                <option value="BROWSER_MCS">BROWSER_MCS (권장: 깊은 탐색)</option>
                <option value="BROWSER_BFS">BROWSER_BFS</option>
                <option value="MCS">MCS</option>
                <option value="BFS">BFS</option>
              </select>
            </div>
            <button
              className="rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50"
              onClick={async () => {
                const firstAuth = authProfilesQuery.data?.items[0];
                if (!firstAuth) {
                  alert("Auth Profile을 먼저 생성해주세요.");
                  return;
                }
                if (firstAuth.type === "STORAGE_STATE" && !firstAuth.storageStateObjectKey) {
                  const proceed = confirm(
                    `Warning: Auth Profile "${firstAuth.name}"에 Storage State가 설정되지 않았습니다.\n` +
                    `로그인이 필요한 페이지는 크롤링할 수 없습니다.\n\n계속하시겠습니까?`
                  );
                  if (!proceed) return;
                }
                await api.crawlRuns.create(projectId, {
                  authProfileId: firstAuth.id,
                  startUrl: projectQuery.data?.baseUrl ?? "https://example.com",
                  budget: { maxNodes: 300, maxEdges: 1200, maxDepth: 8, maxMinutes: 10, maxActionsPerState: 20 },
                  strategy: crawlStrategy
                });
                runsQuery.refetch();
              }}
            >
              New Run
            </button>
          </div>
          <div className="mt-3 space-y-2">
            {runsQuery.data?.items.map((r) => (
              <div key={r.id} className="rounded-lg border border-slate-200 p-3">
                <div className="flex items-center justify-between">
                  <div className="font-medium">{r.status}</div>
                  <Link
                    href={`/runs/${r.id}/graph`}
                    className="text-sm text-slate-700 hover:underline"
                  >
                    Open Graph
                  </Link>
                </div>
                <div className="text-sm text-slate-600">{r.startUrl}</div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}


