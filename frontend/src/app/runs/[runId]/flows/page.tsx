"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

export default function RunFlowsPage() {
  const params = useParams<{ runId: string }>();
  const runId = params.runId;

  const flowsQuery = useQuery({
    queryKey: ["flows", runId],
    queryFn: () => api.flows.listByRun(runId)
  });

  return (
    <main className="space-y-4">
      <header className="flex items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="text-sm text-slate-600">
            <Link href={`/runs/${runId}/graph`} className="hover:underline">
              ← Graph
            </Link>
          </div>
          <h1 className="text-xl font-semibold">Flows</h1>
          <p className="text-sm text-slate-600">Auto Smoke 추천/편집/테스트 생성 (MVP 뼈대)</p>
        </div>
        <button
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          onClick={async () => {
            await api.flows.generateAutoSmoke(runId);
            flowsQuery.refetch();
          }}
        >
          Generate Auto Smoke(임시)
        </button>
      </header>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="space-y-2">
          {flowsQuery.data?.items?.map((f) => (
            <div key={f.id} className="rounded-lg border border-slate-200 p-3">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="font-medium">{f.name}</div>
                  <div className="text-sm text-slate-600">steps: {f.steps.length}</div>
                </div>
                <div className="flex items-center gap-2">
                  <Link href={`/flows/${f.id}/test`} className="text-sm hover:underline">
                    Generate Test
                  </Link>
                  <button
                    className="rounded-md border border-red-200 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
                    onClick={async () => {
                      if (confirm(`정말 ${f.name} 플로우를 삭제하시겠습니까?`)) {
                        try {
                          await api.flows.delete(f.id);
                          flowsQuery.refetch();
                        } catch (err: any) {
                          alert(`삭제 실패: ${err.message || err}`);
                        }
                      }
                    }}
                  >
                    삭제
                  </button>
                </div>
              </div>
            </div>
          ))}
          {!flowsQuery.data?.items?.length && (
            <div className="text-sm text-slate-600">아직 생성된 플로우가 없습니다.</div>
          )}
        </div>
      </section>
    </main>
  );
}


