export type Id = string;

export type ProjectDTO = {
  id: Id;
  name: string;
  baseUrl: string;
  allowlistRules: Record<string, unknown>;
};

export type AuthProfileType = "STORAGE_STATE" | "SCRIPT_LOGIN";

export type AuthProfileDTO = {
  id: Id;
  projectId: Id;
  name: string;
  type: AuthProfileType;
  tags: Record<string, unknown>;
};

export type CrawlRunStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED";

export type CrawlRunDTO = {
  id: Id;
  projectId: Id;
  authProfileId: Id;
  status: CrawlRunStatus;
  startUrl: string;
  budget: Record<string, unknown>;
};

export type GraphNodeDTO = {
  id: Id;
  nodeKey?: string;
  url: string;
  title?: string;
  screenshotThumbUrl?: string;
  depth: number;
};

export type GraphEdgeDTO = {
  id: Id;
  from: Id;
  to: Id;
  actionType: string;
  tags?: Record<string, unknown>;
};

export type GraphDTO = {
  nodes: GraphNodeDTO[];
  edges: GraphEdgeDTO[];
};

export type GraphNodeDetailDTO = {
  id: Id;
  nodeKey: string;
  url: string;
  title: string | null;
  httpStatus: number | null;
  contentType: string | null;
  depth: number;
  discoveredAt: string;
  fetchedAt: string | null;
  htmlSize: number | null;
  htmlSnippet: string | null;
};

export type GraphEdgeDetailDTO = {
  id: Id;
  from: Id;
  to: Id;
  actionType: string;
  anchorText: string | null;
};

export type FlowStepDTO = {
  edgeId: Id;
};

export type FlowDTO = {
  id: Id;
  name: string;
  steps: FlowStepDTO[];
};

export type ListResponse<T> = { items: T[] };


