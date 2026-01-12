"use client";

import { useEffect, useRef } from "react";
import cytoscape, { Core } from "cytoscape";
import dagre from "cytoscape-dagre";
import type { GraphDTO } from "@/lib/contracts";

cytoscape.use(dagre);

function getNodeLabel(url: string, title: string | undefined): string {
  if (title && title !== "StateTrail") {
    return title;
  }
  try {
    const urlObj = new URL(url);
    const path = urlObj.pathname;
    if (path === "/" || path === "") {
      return urlObj.hostname;
    }
    const segments = path.split("/").filter(Boolean);
    if (segments.length === 0) return urlObj.hostname;
    const lastSegment = segments[segments.length - 1];
    if (segments.length === 1) return lastSegment;
    return `${segments[0]}/${lastSegment}`;
  } catch {
    return url;
  }
}

export function GraphCanvas({
  graph,
  selectedNodeId,
  selectedEdgeId,
  onNodeSelect,
  onEdgeSelect,
  onClearSelection
}: {
  graph: GraphDTO;
  selectedNodeId: string | null;
  selectedEdgeId: string | null;
  onNodeSelect: (id: string) => void;
  onEdgeSelect: (id: string) => void;
  onClearSelection: () => void;
}) {
  const hostRef = useRef<HTMLDivElement | null>(null);
  const cyRef = useRef<Core | null>(null);
  const handlersRef = useRef({ onNodeSelect, onEdgeSelect, onClearSelection });

  useEffect(() => {
    handlersRef.current = { onNodeSelect, onEdgeSelect, onClearSelection };
  }, [onNodeSelect, onEdgeSelect, onClearSelection]);

  useEffect(() => {
    if (!hostRef.current) return;
    if (cyRef.current) return;

    cyRef.current = cytoscape({
      container: hostRef.current,
      elements: [],
      style: [
        {
          selector: "node",
          style: {
            "background-color": "#0f172a",
            label: "data(label)",
            color: "#0f172a",
            "text-background-color": "#ffffff",
            "text-background-opacity": 1,
            "text-background-padding": "2px",
            "font-size": "10px",
            "text-wrap": "ellipsis",
            "text-max-width": "120px"
          }
        },
        {
          selector: "node.selected",
          style: {
            "background-color": "#2563eb"
          }
        },
        {
          selector: "edge",
          style: {
            width: 1,
            "curve-style": "bezier",
            "line-color": "#94a3b8",
            "target-arrow-color": "#94a3b8",
            "target-arrow-shape": "triangle",
            label: "data(label)",
            "font-size": "9px",
            color: "#475569",
            "text-background-color": "#ffffff",
            "text-background-opacity": 1,
            "text-background-padding": "1px"
          }
        },
        {
          selector: "edge.selected",
          style: {
            "line-color": "#2563eb",
            "target-arrow-color": "#2563eb",
            width: 2
          }
        }
      ],
      layout: { name: "dagre", nodeSep: 50, rankSep: 100, rankDir: "TB" } as any
    });

    const cy = cyRef.current;
    if (!cy) return;

    cy.on("tap", (evt) => {
      if (evt.target === cy) handlersRef.current.onClearSelection();
    });
    cy.on("tap", "node", (evt) => {
      handlersRef.current.onNodeSelect(evt.target.id());
    });
    cy.on("tap", "edge", (evt) => {
      handlersRef.current.onEdgeSelect(evt.target.id());
    });
  }, []);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy) return;

    // Group nodes by urlPattern
    const patternGroups = new Map<string, typeof graph.nodes>();
    for (const node of graph.nodes) {
      const pattern = node.urlPattern || node.url;
      if (!patternGroups.has(pattern)) {
        patternGroups.set(pattern, []);
      }
      patternGroups.get(pattern)!.push(node);
    }

    // Create representative nodes (one per pattern)
    const patternToRepNode = new Map<string, typeof graph.nodes[0]>();
    const nodeIdToPatternNode = new Map<string, string>(); // original node id -> pattern node id
    
    for (const [pattern, nodes] of patternGroups.entries()) {
      // Select representative node (first one, or one with shortest URL)
      const repNode = nodes.reduce((prev, curr) => 
        curr.url.length < prev.url.length ? curr : prev
      );
      patternToRepNode.set(pattern, repNode);
      
      // Map all nodes in this pattern to the representative
      for (const node of nodes) {
        nodeIdToPatternNode.set(node.id, repNode.id);
      }
    }

    // Create pattern nodes with variant count
    const patternNodes = Array.from(patternToRepNode.entries()).map(([pattern, repNode]) => {
      const variantCount = patternGroups.get(pattern)!.length;
      const label = variantCount > 1 
        ? `${getNodeLabel(repNode.urlPattern || repNode.url, repNode.title)} (${variantCount})`
        : getNodeLabel(repNode.url, repNode.title);
      
      return {
        group: "nodes" as const,
        data: {
          id: repNode.id,
          label,
          depth: repNode.depth,
          pattern,
          variantCount
        }
      };
    });

    // Map edges to pattern nodes
    const patternEdges = graph.edges.map((e) => {
      const fromPattern = nodeIdToPatternNode.get(e.from) || e.from;
      const toPattern = nodeIdToPatternNode.get(e.to) || e.to;
      
      // Skip self-loops within the same pattern
      if (fromPattern === toPattern) return null;
      
      return {
        group: "edges" as const,
        data: { 
          id: e.id, 
          source: fromPattern, 
          target: toPattern, 
          label: "",
          originalFrom: e.from,
          originalTo: e.to
        }
      };
    }).filter((e): e is NonNullable<typeof e> => e !== null);

    cy.elements().remove();
    cy.add([...patternNodes, ...patternEdges]);

    cy.layout({ 
      name: "dagre", 
      nodeSep: 50,
      rankSep: 100,
      rankDir: "TB",
      ranker: "tight-tree"
    } as any).run();
  }, [graph]);

  useEffect(() => {
    const cy = cyRef.current;
    if (!cy) return;
    cy.elements().removeClass("selected");
    if (selectedNodeId) cy.getElementById(selectedNodeId).addClass("selected");
    if (selectedEdgeId) cy.getElementById(selectedEdgeId).addClass("selected");
  }, [selectedNodeId, selectedEdgeId]);

  return <div ref={hostRef} className="h-[70vh] w-full" />;
}


