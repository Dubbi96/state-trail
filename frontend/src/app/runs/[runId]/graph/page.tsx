"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { api } from "@/lib/api";
import { GraphCanvas } from "@/components/graph/GraphCanvas";
import { InspectorPanel } from "@/components/graph/InspectorPanel";

export default function RunGraphPage() {
  const params = useParams<{ runId: string }>();
  const runId = params.runId;

  const graphQuery = useQuery({
    queryKey: ["graph", runId],
    queryFn: () => api.graph.get(runId)
  });

  return (
    <main className="space-y-4">
      <header className="flex items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="text-sm text-slate-600">
            <Link href="/projects" className="hover:underline">
              Projects
            </Link>{" "}
            / Run / Graph
          </div>
          <h1 className="text-xl font-semibold">Graph Explorer</h1>
        </div>
        <Link
          href={`/runs/${runId}/flows`}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Flows
        </Link>
      </header>

      <section className="grid gap-4 lg:grid-cols-[1fr_380px]">
        <div className="rounded-xl border border-slate-200 bg-white p-3">
          <GraphCanvas graph={graphQuery.data ?? { nodes: [], edges: [] }} />
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-3">
          <InspectorPanel />
        </div>
      </section>
    </main>
  );
}


