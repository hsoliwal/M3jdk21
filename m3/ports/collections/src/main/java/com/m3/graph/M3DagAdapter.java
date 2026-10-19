// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.graph;

import com.m3.util.M3NotYetReceived;
import java.util.Collection;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexDagAdapter}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexDagAdapter.java}
 * blob {@code 5dbea60bdbabc05af26e5c06357d813f2acd078b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 9fc4792be585f1df8a6489e38c8f6d949a87cdc91263c307db61c6c439fdf40e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3DagAdapter}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code DAG_GRAPH_RELATION}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member graphIdentity(G, MIndexScope) (return type)}</li>
 *   <li>{@code member nodeIdentity(G, N, MIndexScope) (return type)}</li>
 *   <li>{@code member role(G, N) (return type)}</li>
 *   <li>{@code member project(G, MIndexScope) (return type)}</li>
 * </ul>
 */
public interface M3DagAdapter<G, N> {
    Collection<N> nodes(G graph);

    Collection<N> dependencies(G graph, N node);
}
