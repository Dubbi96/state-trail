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
                <div className="font-medium">{a.name}</div>
                <div className="text-sm text-slate-600">{a.type}</div>
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
                if (!firstAuth) return;
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


