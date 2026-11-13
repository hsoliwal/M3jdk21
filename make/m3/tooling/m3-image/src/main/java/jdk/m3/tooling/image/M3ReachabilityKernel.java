/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Executes deterministic transitive closure over one immutable application-class graph. */
public interface M3ReachabilityKernel {
    /** Returns a stable implementation identifier for receipts and diagnostics. */
    String id();

    /**
     * Computes the sorted unique vertex indexes reachable from the graph roots.
     *
     * @param graph validated immutable graph
     * @return sorted unique reachable vertex indexes
     */
    int[] close(M3ReachabilityGraph graph);
}
