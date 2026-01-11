"use client";

import { useEffect, useRef } from "react";
import cytoscape, { Core } from "cytoscape";
import type { GraphDTO } from "@/lib/contracts";

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
            "text-background-padding": 2,
            "font-size": 10,
            "text-wrap": "ellipsis",
            "text-max-width": 120
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
            "font-size": 9,
            color: "#475569",
            "text-background-color": "#ffffff",
            "text-background-opacity": 1,
            "text-background-padding": 1
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
      layout: { name: "cose", animate: false }
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

    cy.elements().remove();
    cy.add([
      ...graph.nodes.map((n) => ({
        group: "nodes" as const,
        data: { id: n.id, label: n.title || n.url }
      })),
      ...graph.edges.map((e) => ({
        group: "edges" as const,
        data: { id: e.id, source: e.from, target: e.to, label: `${e.actionType}` }
      }))
    ]);

    cy.layout({ name: "cose", animate: false }).run();
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


