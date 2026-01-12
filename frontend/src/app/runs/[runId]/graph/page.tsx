"use client";

import { useParams } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState, useEffect, useRef } from "react";
import { api } from "@/lib/api";
import type { GraphDTO } from "@/lib/contracts";
import { GraphCanvas } from "@/components/graph/GraphCanvas";
import { InspectorPanel } from "@/components/graph/InspectorPanel";

export default function RunGraphPage() {
  const params = useParams<{ runId: string }>();
  const runId = params.runId;
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const eventSourceCleanupRef = useRef<(() => void) | null>(null);

  const graphQuery = useQuery({
    queryKey: ["graph", runId],
    queryFn: () => api.graph.get(runId),
    refetchInterval: false
  });

  // SSE 이벤트 구독
  useEffect(() => {
    const cleanup = api.graph.subscribeEvents(runId, (event) => {
      if (event.type === "NODE_CREATED" || event.type === "EDGE_CREATED") {
        // 그래프 데이터 무효화하여 재조회
        queryClient.invalidateQueries({ queryKey: ["graph", runId] });
      } else if (event.type === "STATUS") {
        const status = (event.data as { status?: string })?.status;
        if (status === "SUCCEEDED" || status === "FAILED") {
          // 크롤 완료 시 최종 그래프 데이터 재조회
          queryClient.invalidateQueries({ queryKey: ["graph", runId] });
        }
      }
    });
    eventSourceCleanupRef.current = cleanup;
    return () => {
      if (eventSourceCleanupRef.current) {
        eventSourceCleanupRef.current();
      }
    };
  }, [runId, queryClient]);

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
          <GraphCanvas
            graph={graphQuery.data ?? { nodes: [], edges: [] }}
            selectedNodeId={selectedNodeId}
            selectedEdgeId={selectedEdgeId}
            onNodeSelect={(id) => {
              setSelectedNodeId(id);
              setSelectedEdgeId(null);
            }}
            onEdgeSelect={(id) => {
              setSelectedEdgeId(id);
              setSelectedNodeId(null);
            }}
            onClearSelection={() => {
              setSelectedNodeId(null);
              setSelectedEdgeId(null);
            }}
          />
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-3">
          <InspectorPanel
            runId={runId}
            selectedNodeId={selectedNodeId}
            selectedEdgeId={selectedEdgeId}
            onClearSelection={() => {
              setSelectedNodeId(null);
              setSelectedEdgeId(null);
            }}
          />
        </div>
      </section>
    </main>
  );
}


