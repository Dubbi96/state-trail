import type {
  AuthProfileDTO,
  CrawlRunDTO,
  FlowDTO,
  GraphDTO,
  GraphEdgeDetailDTO,
  GraphNodeDetailDTO,
  ListResponse,
  ProjectDTO
} from "@/lib/contracts";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      "content-type": "application/json",
      ...(init?.headers ?? {})
    },
    cache: "no-store"
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} ${res.statusText}: ${body}`);
  }
  return (await res.json()) as T;
}

export const api = {
  projects: {
    list: () => http<ListResponse<ProjectDTO>>(`/api/projects`),
    get: (projectId: string) => http<ProjectDTO>(`/api/projects/${projectId}`),
    create: (body: { name: string; baseUrl: string; allowlistRules: Record<string, unknown> }) =>
      http<ProjectDTO>(`/api/projects`, { method: "POST", body: JSON.stringify(body) })
  },
  authProfiles: {
    list: (projectId: string) => http<ListResponse<AuthProfileDTO>>(`/api/projects/${projectId}/auth-profiles`),
    create: (projectId: string, body: { name: string; type: "STORAGE_STATE" | "SCRIPT_LOGIN"; tags: Record<string, unknown> }) =>
      http<AuthProfileDTO>(`/api/projects/${projectId}/auth-profiles`, { method: "POST", body: JSON.stringify(body) })
  },
  crawlRuns: {
    list: (projectId: string) => http<ListResponse<CrawlRunDTO>>(`/api/projects/${projectId}/crawl-runs`),
    create: (
      projectId: string,
      body: { authProfileId: string; startUrl: string; budget: Record<string, unknown>; strategy?: string }
    ) => http<CrawlRunDTO>(`/api/projects/${projectId}/crawl-runs`, { method: "POST", body: JSON.stringify(body) })
  },
  graph: {
    get: (runId: string) => http<GraphDTO>(`/api/crawl-runs/${runId}/graph`),
    getNode: (runId: string, nodeId: string) => http<GraphNodeDetailDTO>(`/api/crawl-runs/${runId}/nodes/${nodeId}`),
    getEdge: (runId: string, edgeId: string) => http<GraphEdgeDetailDTO>(`/api/crawl-runs/${runId}/edges/${edgeId}`)
  },
  flows: {
    listByRun: (runId: string) => http<ListResponse<FlowDTO>>(`/api/crawl-runs/${runId}/flows`),
    generateAutoSmoke: (runId: string) => http<{ ok: true }>(`/api/crawl-runs/${runId}/flows/auto-smoke`, { method: "POST" }),
    generateTest: (flowId: string) => http<{ code: string }>(`/api/flows/${flowId}/generate-test`, { method: "POST" })
  }
};


