/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.mat;

import java.util.Objects;

/**
 * Adaptive immutable directed graph: sparse CSR or dense bit matrix.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatAdaptiveIntGraph.java}
 * blob {@code fcd25f33adba6f6edf630d0ebb02a84e3960352a} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatAdaptiveIntGraph {
    public enum Encoding { CSR, BIT_MATRIX }

    private final int nodes;
    private final int edges;
    private final Encoding encoding;
    private final MatCsrGraph csr;
    private final MatBitMatrix matrix;

    private MatAdaptiveIntGraph(
            int nodes, int edges, Encoding encoding, MatCsrGraph csr, MatBitMatrix matrix) {
        this.nodes = nodes;
        this.edges = edges;
        this.encoding = encoding;
        this.csr = csr;
        this.matrix = matrix;
    }

    public static MatAdaptiveIntGraph of(
            int nodes, int[] from, int[] to, MatCollectionPolicy policy) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(policy, "policy");
        MatCsrGraph csr = MatCsrGraph.of(nodes, from, to);
        long csrBytes = csr.primitivePayloadBytes();
        int wordsPerRow = (nodes + 63) >>> 6;
        long matrixWords = (long) nodes * wordsPerRow;
        long matrixBytes = matrixWords > Integer.MAX_VALUE ? Long.MAX_VALUE : matrixWords * 8L;

        boolean dense = switch (policy) {
            case MIN_MEMORY -> matrixBytes < csrBytes;
            case FAST_LOOKUP -> matrixBytes <= csrBytes * 2L;
            case BALANCED -> matrixBytes <= csrBytes && csr.edges() > nodes;
        };
        if (!dense) {
            return new MatAdaptiveIntGraph(nodes, csr.edges(), Encoding.CSR, csr, null);
        }

        MatBitMatrix.Builder builder = MatBitMatrix.builder(nodes, nodes);
        for (int source = 0; source < nodes; source++) {
            MatIntList successors = csr.successors(source);
            for (int i = 0; i < successors.size(); i++) builder.set(source, successors.getInt(i));
        }
        return new MatAdaptiveIntGraph(nodes, csr.edges(), Encoding.BIT_MATRIX, null, builder.build());
    }

    public int nodes() { return nodes; }
    public int edges() { return edges; }
    public Encoding encoding() { return encoding; }
    public long primitivePayloadBytes() {
        return encoding == Encoding.CSR ? csr.primitivePayloadBytes() : matrix.primitivePayloadBytes();
    }

    public boolean containsEdge(int from, int to) {
        return encoding == Encoding.CSR ? csr.containsEdge(from, to) : matrix.get(from, to);
    }

    public int outDegree(int node) {
        return encoding == Encoding.CSR ? csr.outDegree(node) : matrix.rowCardinality(node);
    }

    public MatIntList successors(int node) {
        return encoding == Encoding.CSR ? csr.successors(node) : matrix.setColumns(node);
    }
}
