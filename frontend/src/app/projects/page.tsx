"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { api } from "@/lib/api";

export default function ProjectsPage() {
  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: () => api.projects.list()
  });

  return (
    <main className="space-y-4">
      <header className="flex items-end justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Projects</h1>
          <p className="text-sm text-slate-600">프로젝트 목록/생성 (MVP 스캐폴딩)</p>
        </div>
        <button
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          onClick={async () => {
            await api.projects.create({
              name: `Demo Project ${new Date().toISOString().slice(11, 19)}`,
              baseUrl: "https://example.com",
              allowlistRules: {
                domains: ["example.com"],
                pathPrefixes: ["/"],
                deny: ["/logout"]
              }
            });
            projectsQuery.refetch();
          }}
        >
          New Project(임시)
        </button>
      </header>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        {projectsQuery.isLoading ? (
          <div className="text-sm text-slate-600">로딩 중…</div>
        ) : projectsQuery.isError ? (
          <div className="text-sm text-red-700">프로젝트 조회 실패</div>
        ) : (
          <ul className="divide-y divide-slate-200">
            {projectsQuery.data?.items.map((p) => (
              <li key={p.id} className="flex items-center justify-between py-3">
                <div className="min-w-0">
                  <div className="truncate font-medium">{p.name}</div>
                  <div className="truncate text-sm text-slate-600">{p.baseUrl}</div>
                </div>
                <Link
                  href={`/projects/${p.id}`}
                  className="rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50"
                >
                  Open
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}


