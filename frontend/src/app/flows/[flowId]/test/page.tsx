"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { api } from "@/lib/api";

export default function FlowTestPage() {
  const params = useParams<{ flowId: string }>();
  const flowId = params.flowId;

  const codeQuery = useQuery({
    queryKey: ["flow-test-code", flowId],
    queryFn: () => api.flows.generateTest(flowId)
  });

  return (
    <main className="space-y-4">
      <header className="space-y-2">
        <Link href="/projects" className="text-sm text-slate-600 hover:underline">
          ← Projects
        </Link>
        <h1 className="text-xl font-semibold">Test Code Preview</h1>
        <p className="text-sm text-slate-600">템플릿 기반(결정론적) 코드 생성 결과를 미리보기 (MVP 뼈대)</p>
      </header>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        {codeQuery.isLoading ? (
          <div className="text-sm text-slate-600">생성 중…</div>
        ) : codeQuery.isError ? (
          <div className="text-sm text-red-700">코드 생성 실패</div>
        ) : (
          <pre className="overflow-auto rounded-lg bg-slate-950 p-4 text-xs text-slate-100">
            {codeQuery.data.code}
          </pre>
        )}
      </section>
    </main>
  );
}


