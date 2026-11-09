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

import java.util.Arrays;
import java.util.Objects;

/**
 * Frozen acyclic hash-consed graph with dense integer node IDs and shared leaf ownership.
 *
 * <p>Each canonical row is [kind, payloadId, child IDs...]. The builder permits
 * references to existing nodes only, preserving DAG topology and excluding cycles.
 * Payload ID -1 means there is no leaf. IDs are local to one DAG, never portable.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatDag.java}
 * blob {@code ef7d17f925ce18e79f756535bb82dfd67dedb2e5} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatDag<T> {
    private final MatPool<T> payloads;
    private final MatInternedRows nodes;

    private MatDag(MatPool<T> payloads, MatInternedRows nodes) {
        this.payloads = payloads;
        this.nodes = nodes;
    }

    public static <T> Builder<T> builder(MatPool<T> payloads, MatLimits limits) {
        return new Builder<>(payloads, limits);
    }

    public int size() { return nodes.size(); }
    public int kind(int node) { return nodes.get(node, 0); }
    public int payloadId(int node) { return nodes.get(node, 1); }
    public T payload(int node) {
        int id = payloadId(node);
        return id == -1 ? null : payloads.value(id);
    }
    public int childCount(int node) { return nodes.length(node) - 2; }
    public int childAt(int node, int index) {
        return nodes.get(node, Math.addExact(2, Objects.checkIndex(index, childCount(node))));
    }
    public int[] children(int node) {
        int[] row = nodes.row(node);
        return Arrays.copyOfRange(row, 2, row.length);
    }
    public long payloadBytes() { return nodes.payloadBytes(); }

    public static final class Builder<T> {
        private final MatPool<T> payloads;
        private final MatInternedRows.Builder nodes;

        private Builder(MatPool<T> payloads, MatLimits limits) {
            this.payloads = Objects.requireNonNull(payloads, "payloads");
            this.nodes = MatInternedRows.builder(limits);
        }

        public int size() { return nodes.size(); }

        /** Intern a node; order and multiplicity of its child references are significant. */
        public int intern(int kind, int payloadId, int... children) {
            Objects.requireNonNull(children, "children");
            if (payloadId < -1 || payloadId >= payloads.size()) {
                throw new IndexOutOfBoundsException("unknown payload ID: " + payloadId);
            }
            for (int child : children) {
                if (child < 0 || child >= nodes.size()) {
                    throw new IndexOutOfBoundsException("unknown or forward child: " + child);
                }
            }
            int[] row = new int[Math.addExact(2, children.length)];
            row[0] = kind;
            row[1] = payloadId;
            System.arraycopy(children, 0, row, 2, children.length);
            return nodes.intern(row);
        }

        public MatDag<T> build() { return new MatDag<>(payloads, nodes.build()); }
    }
}
