"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { api } from "@/lib/api";

export default function ProjectsPage() {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    baseUrl: "",
    allowlistDomains: "",
    allowlistPathPrefixes: "",
    allowlistDeny: ""
  });

  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: () => api.projects.list()
  });

  const handleCreate = async () => {
    const domains = formData.allowlistDomains
      .split(",")
      .map((d) => d.trim())
      .filter((d) => d.length > 0);
    const pathPrefixes = formData.allowlistPathPrefixes
      .split(",")
      .map((p) => p.trim())
      .filter((p) => p.length > 0);
    const deny = formData.allowlistDeny
      .split(",")
      .map((d) => d.trim())
      .filter((d) => d.length > 0);

    await api.projects.create({
      name: formData.name || `Project ${new Date().toISOString().slice(11, 19)}`,
      baseUrl: formData.baseUrl || "https://example.com",
      allowlistRules: {
        domains: domains,
        pathPrefixes: pathPrefixes,
        deny: deny
      }
    });
    setShowCreateForm(false);
    setFormData({ name: "", baseUrl: "", allowlistDomains: "", allowlistPathPrefixes: "", allowlistDeny: "" });
    projectsQuery.refetch();
  };

  return (
    <main className="space-y-4">
      <header className="flex items-end justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Projects</h1>
          <p className="text-sm text-slate-600">프로젝트 목록/생성</p>
        </div>
        <button
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          onClick={() => setShowCreateForm(!showCreateForm)}
        >
          {showCreateForm ? "취소" : "New Project"}
        </button>
      </header>

      {showCreateForm && (
        <section className="rounded-xl border border-slate-200 bg-white p-4">
          <h2 className="mb-4 font-semibold">새 프로젝트 생성</h2>
          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-slate-700">프로젝트 이름</label>
              <input
                type="text"
                className="mt-1 w-full rounded-md border border-slate-200 px-3 py-2 text-sm"
                placeholder="My Project"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">시작 URL (필수)</label>
              <input
                type="url"
                className="mt-1 w-full rounded-md border border-slate-200 px-3 py-2 text-sm"
                placeholder="https://example.com"
                value={formData.baseUrl}
                onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">
                허용 도메인 (쉼표로 구분, 비워두면 모든 도메인 허용)
              </label>
              <input
                type="text"
                className="mt-1 w-full rounded-md border border-slate-200 px-3 py-2 text-sm"
                placeholder="example.com, subdomain.example.com"
                value={formData.allowlistDomains}
                onChange={(e) => setFormData({ ...formData, allowlistDomains: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">
                허용 경로 접두사 (쉼표로 구분, 비워두면 모든 경로 허용)
              </label>
              <input
                type="text"
                className="mt-1 w-full rounded-md border border-slate-200 px-3 py-2 text-sm"
                placeholder="/, /docs, /api"
                value={formData.allowlistPathPrefixes}
                onChange={(e) => setFormData({ ...formData, allowlistPathPrefixes: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">
                차단 경로 (쉼표로 구분)
              </label>
              <input
                type="text"
                className="mt-1 w-full rounded-md border border-slate-200 px-3 py-2 text-sm"
                placeholder="/logout, /admin"
                value={formData.allowlistDeny}
                onChange={(e) => setFormData({ ...formData, allowlistDeny: e.target.value })}
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                className="rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50"
                onClick={() => setShowCreateForm(false)}
              >
                취소
              </button>
              <button
                className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
                onClick={handleCreate}
              >
                생성
              </button>
            </div>
          </div>
        </section>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        {projectsQuery.isLoading ? (
          <div className="text-sm text-slate-600">로딩 중…</div>
        ) : projectsQuery.isError ? (
          <div className="text-sm text-red-700">프로젝트 조회 실패</div>
        ) : (
          <ul className="divide-y divide-slate-200">
            {projectsQuery.data?.items.map((p) => (
              <li key={p.id} className="flex items-center justify-between py-3">
                <div className="min-w-0 flex-1">
                  <div className="truncate font-medium">{p.name}</div>
                  <div className="truncate text-sm text-slate-600">{p.baseUrl}</div>
                </div>
                <div className="flex items-center gap-2">
                  <Link
                    href={`/projects/${p.id}`}
                    className="rounded-md border border-slate-200 px-3 py-2 text-sm hover:bg-slate-50"
                  >
                    Open
                  </Link>
                  <button
                    className="rounded-md border border-red-200 px-3 py-2 text-sm text-red-700 hover:bg-red-50"
                    onClick={async () => {
                      if (confirm(`정말 ${p.name} 프로젝트를 삭제하시겠습니까?\n관련된 모든 Auth Profile, Crawl Run, Flow도 함께 삭제됩니다.`)) {
                        try {
                          const result = await api.projects.delete(p.id);
                          alert(result.message || "프로젝트가 삭제되었습니다.");
                          projectsQuery.refetch();
                        } catch (err: any) {
                          alert(`삭제 실패: ${err.message || err}`);
                        }
                      }
                    }}
                  >
                    삭제
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}


