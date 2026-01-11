"use client";

import { useEffect, useRef } from "react";
import cytoscape, { Core } from "cytoscape";
import type { GraphDTO } from "@/lib/contracts";

export function GraphCanvas({ graph }: { graph: GraphDTO }) {
  const hostRef = useRef<HTMLDivElement | null>(null);
  const cyRef = useRef<Core | null>(null);

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
        }
      ],
      layout: { name: "cose", animate: false }
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

  return <div ref={hostRef} className="h-[70vh] w-full" />;
}


