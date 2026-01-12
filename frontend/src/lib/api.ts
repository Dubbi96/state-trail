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
      http<ProjectDTO>(`/api/projects`, { method: "POST", body: JSON.stringify(body) }),
    delete: (projectId: string) => http<{ ok: boolean; message: string }>(`/api/projects/${projectId}`, { method: "DELETE" })
  },
  authProfiles: {
    list: (projectId: string) => http<ListResponse<AuthProfileDTO>>(`/api/projects/${projectId}/auth-profiles`),
    create: (projectId: string, body: { name: string; type: "STORAGE_STATE" | "SCRIPT_LOGIN"; tags: Record<string, unknown> }) =>
      http<AuthProfileDTO>(`/api/projects/${projectId}/auth-profiles`, { method: "POST", body: JSON.stringify(body) }),
    uploadStorageState: (projectId: string, authProfileId: string, file: File) => {
      const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
      const formData = new FormData();
      formData.append("file", file);
      return fetch(`${API_BASE}/api/projects/${projectId}/auth-profiles/${authProfileId}/storage-state`, {
        method: "PUT",
        body: formData
      }).then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
        return res.json() as Promise<AuthProfileDTO>;
      });
    },
    captureStorageState: (projectId: string, authProfileId: string, loginUrl: string) => {
      return http<{ ok: boolean; message: string }>(
        `/api/projects/${projectId}/auth-profiles/${authProfileId}/capture-storage-state`,
        {
          method: "POST",
          body: JSON.stringify({ loginUrl })
        }
      );
    },
    completeCaptureStorageState: (projectId: string, authProfileId: string) => {
      return http<{ ok: boolean; message: string; objectKey?: string }>(
        `/api/projects/${projectId}/auth-profiles/${authProfileId}/complete-capture-storage-state`,
        { method: "POST" }
      );
    },
    delete: (projectId: string, authProfileId: string) => {
      return http<{ ok: boolean; message: string }>(
        `/api/projects/${projectId}/auth-profiles/${authProfileId}`,
        { method: "DELETE" }
      );
    }
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
    getEdge: (runId: string, edgeId: string) => http<GraphEdgeDetailDTO>(`/api/crawl-runs/${runId}/edges/${edgeId}`),
    subscribeEvents: (runId: string, onEvent: (event: { type: string; data: unknown }) => void) => {
      const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
      const eventSource = new EventSource(`${API_BASE}/api/crawl-runs/${runId}/events`);
      eventSource.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data);
          onEvent({ type: e.type, data });
        } catch (err) {
          console.error("Failed to parse SSE event:", err);
        }
      };
      eventSource.addEventListener("NODE_CREATED", (e) => {
        try {
          const data = JSON.parse((e as MessageEvent).data);
          onEvent({ type: "NODE_CREATED", data });
        } catch (err) {
          console.error("Failed to parse NODE_CREATED event:", err);
        }
      });
      eventSource.addEventListener("EDGE_CREATED", (e) => {
        try {
          const data = JSON.parse((e as MessageEvent).data);
          onEvent({ type: "EDGE_CREATED", data });
        } catch (err) {
          console.error("Failed to parse EDGE_CREATED event:", err);
        }
      });
      eventSource.addEventListener("STATUS", (e) => {
        try {
          const data = JSON.parse((e as MessageEvent).data);
          onEvent({ type: "STATUS", data });
        } catch (err) {
          console.error("Failed to parse STATUS event:", err);
        }
      });
      eventSource.addEventListener("STATS", (e) => {
        try {
          const data = JSON.parse((e as MessageEvent).data);
          onEvent({ type: "STATS", data });
        } catch (err) {
          console.error("Failed to parse STATS event:", err);
        }
      });
      return () => eventSource.close();
    }
  },
  flows: {
    listByRun: (runId: string) => http<ListResponse<FlowDTO>>(`/api/crawl-runs/${runId}/flows`),
    generateAutoSmoke: (runId: string) => http<{ ok: true }>(`/api/crawl-runs/${runId}/flows/auto-smoke`, { method: "POST" }),
    generateTest: (flowId: string) => http<{ code: string }>(`/api/flows/${flowId}/generate-test`, { method: "POST" })
  }
};


