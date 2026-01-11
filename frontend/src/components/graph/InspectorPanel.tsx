"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

export function InspectorPanel({
  runId,
  selectedNodeId,
  selectedEdgeId,
  onClearSelection
}: {
  runId: string;
  selectedNodeId: string | null;
  selectedEdgeId: string | null;
  onClearSelection: () => void;
}) {
  const nodeQuery = useQuery({
    queryKey: ["graph-node", runId, selectedNodeId],
    queryFn: () => api.graph.getNode(runId, selectedNodeId!),
    enabled: !!selectedNodeId
  });

  const edgeQuery = useQuery({
    queryKey: ["graph-edge", runId, selectedEdgeId],
    queryFn: () => api.graph.getEdge(runId, selectedEdgeId!),
    enabled: !!selectedEdgeId
  });

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <div className="font-semibold">Inspector</div>
        <button
          className="rounded-md border border-slate-200 px-2 py-1 text-xs hover:bg-slate-50"
          onClick={onClearSelection}
        >
          Clear
        </button>
      </div>

      {!selectedNodeId && !selectedEdgeId && (
        <div className="text-sm text-slate-600">노드/엣지를 클릭하면 상세 정보가 여기에 표시됩니다.</div>
      )}

      {selectedNodeId && (
        <div className="space-y-2">
          <div className="text-xs font-medium text-slate-500">NODE</div>
          {nodeQuery.isLoading ? (
            <div className="text-sm text-slate-600">로딩 중…</div>
          ) : nodeQuery.isError ? (
            <div className="text-sm text-red-700">노드 상세 조회 실패</div>
          ) : (
            <>
              <div className="rounded-lg border border-slate-200 p-3">
                <div className="text-sm font-medium">{nodeQuery.data.title ?? "(no title)"}</div>
                <div className="mt-1 break-all text-xs text-slate-600">{nodeQuery.data.url}</div>
                <div className="mt-2 grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <div className="text-slate-500">HTTP</div>
                    <div className="font-medium">{nodeQuery.data.httpStatus ?? "-"}</div>
                  </div>
                  <div>
                    <div className="text-slate-500">Depth</div>
                    <div className="font-medium">{nodeQuery.data.depth}</div>
                  </div>
                  <div className="col-span-2">
                    <div className="text-slate-500">Content-Type</div>
                    <div className="break-all font-medium">{nodeQuery.data.contentType ?? "-"}</div>
                  </div>
                </div>
              </div>

              <div className="rounded-lg border border-slate-200 p-3">
                <div className="flex items-center justify-between">
                  <div className="text-xs font-medium text-slate-700">HTML Snapshot (snippet)</div>
                  <div className="text-xs text-slate-500">{nodeQuery.data.htmlSize ?? 0} chars</div>
                </div>
                {nodeQuery.data.htmlSnippet ? (
                  <pre className="mt-2 max-h-[40vh] overflow-auto rounded bg-slate-950 p-2 text-[10px] text-slate-100">
                    {nodeQuery.data.htmlSnippet}
                  </pre>
                ) : (
                  <div className="mt-2 text-xs text-slate-600">HTML 스냅샷이 없습니다(비-HTML 응답일 수 있음).</div>
                )}
              </div>
            </>
          )}
        </div>
      )}

      {selectedEdgeId && (
        <div className="space-y-2">
          <div className="text-xs font-medium text-slate-500">EDGE</div>
          {edgeQuery.isLoading ? (
            <div className="text-sm text-slate-600">로딩 중…</div>
          ) : edgeQuery.isError ? (
            <div className="text-sm text-red-700">엣지 상세 조회 실패</div>
          ) : (
            <div className="rounded-lg border border-slate-200 p-3 text-xs">
              <div>
                <span className="text-slate-500">Type</span>{" "}
                <span className="font-medium">{edgeQuery.data.actionType}</span>
              </div>
              <div className="mt-2">
                <div className="text-slate-500">Anchor Text</div>
                <div className="mt-1 whitespace-pre-wrap font-medium">{edgeQuery.data.anchorText ?? "-"}</div>
              </div>
              <div className="mt-2 break-all text-slate-500">
                {edgeQuery.data.from} → {edgeQuery.data.to}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}


