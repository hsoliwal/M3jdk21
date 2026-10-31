// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Immutable typed DAG over canonical M3 atom coordinates.
 *
 * <p>Node identity is the atom ID. An edge is only primitive state:
 * {@code from + relationWordId + to + ordinal + metadataBits + interactionKind}. Relation IDs are
 * M3 String/lexicon coordinates, so logic/evidence/reason/discussion labels use the same language
 * identity space rather than a second graph-specific string table.</p>
 */
public final class M3Dag {
    public record EdgeView(
            int edgeId,
            int fromAtomId,
            long relationWordId,
            int toAtomId,
            int ordinal,
            long metadataBits,
            M3InteractionKind interactionKind) { }

    private record Draft(
            int from,
            long relation,
            int to,
            int ordinal,
            long metadata,
            M3InteractionKind kind) { }

    public static final class Builder {
        private final M3AtomStore atoms;
        private final List<Draft> edges = new ArrayList<>();
        private boolean frozen;

        public Builder(M3AtomStore atoms) {
            this.atoms = Objects.requireNonNull(atoms, "atoms");
        }

        public M3AtomStore atoms() { return atoms; }

        public Builder edge(int from, long relationWordId, int to) {
            return edge(from, relationWordId, to, 0, 0L, M3InteractionMode.CONNECT);
        }

        public Builder edge(
                int from,
                long relationWordId,
                int to,
                int ordinal,
                long metadataBits,
                M3InteractionMode mode) {
            ensureMutable();
            Objects.checkIndex(from, atoms.atomCount());
            Objects.checkIndex(to, atoms.atomCount());
            if (from == to) throw new IllegalArgumentException("self DAG edge");
            if (relationWordId < 0L) throw new IllegalArgumentException("relation ID must be >= 0");
            if (ordinal < 0) throw new IllegalArgumentException("edge ordinal must be >= 0");
            M3InteractionKind kind = M3Interaction.classify(
                    atoms.domain(from), atoms.domain(to), Objects.requireNonNull(mode, "mode"));
            edges.add(new Draft(from, relationWordId, to, ordinal, metadataBits, kind));
            return this;
        }

        public M3Dag freeze() {
            ensureMutable();
            frozen = true;
            Draft[] sorted = edges.toArray(Draft[]::new);
            Arrays.sort(sorted, Comparator.comparingInt(Draft::from)
                    .thenComparing((left, right) ->
                            Long.compareUnsigned(left.relation(), right.relation()))
                    .thenComparingInt(Draft::ordinal)
                    .thenComparingInt(Draft::to)
                    .thenComparingLong(Draft::metadata)
                    .thenComparingInt(value -> value.kind().ordinal()));

            for (int i = 1; i < sorted.length; i++) {
                if (sorted[i - 1].equals(sorted[i])) {
                    throw new IllegalArgumentException("duplicate DAG edge");
                }
            }

            int edgeCount = sorted.length;
            int[] from = new int[edgeCount];
            long[] relation = new long[edgeCount];
            int[] to = new int[edgeCount];
            int[] ordinal = new int[edgeCount];
            long[] metadata = new long[edgeCount];
            byte[] kind = new byte[edgeCount];
            int[] outgoingOffsets = new int[atoms.atomCount() + 1];
            int[] incomingOffsets = new int[atoms.atomCount() + 1];

            for (int row = 0; row < edgeCount; row++) {
                Draft edge = sorted[row];
                from[row] = edge.from();
                relation[row] = edge.relation();
                to[row] = edge.to();
                ordinal[row] = edge.ordinal();
                metadata[row] = edge.metadata();
                kind[row] = (byte) edge.kind().ordinal();
                outgoingOffsets[edge.from() + 1]++;
                incomingOffsets[edge.to() + 1]++;
            }
            prefix(outgoingOffsets);
            prefix(incomingOffsets);

            int[] incomingRows = new int[edgeCount];
            int[] cursor = incomingOffsets.clone();
            for (int row = 0; row < edgeCount; row++) {
                incomingRows[cursor[to[row]]++] = row;
            }

            int[] topological = topological(
                    atoms.atomCount(), to, outgoingOffsets);
            long contentHash = contentHash(atoms, from, relation, to, ordinal, metadata, kind);

            return new M3Dag(
                    atoms,
                    outgoingOffsets,
                    incomingOffsets,
                    incomingRows,
                    from,
                    relation,
                    to,
                    ordinal,
                    metadata,
                    kind,
                    topological,
                    contentHash);
        }

        private void ensureMutable() {
            if (frozen) throw new IllegalStateException("DAG builder already frozen");
        }
    }

    private final M3AtomStore atoms;
    private final int[] outgoingOffsets;
    private final int[] incomingOffsets;
    private final int[] incomingRows;
    private final int[] from;
    private final long[] relationWordIds;
    private final int[] to;
    private final int[] ordinals;
    private final long[] metadataBits;
    private final byte[] interactionKinds;
    private final int[] topologicalOrder;
    private final long contentHash;

    private M3Dag(
            M3AtomStore atoms,
            int[] outgoingOffsets,
            int[] incomingOffsets,
            int[] incomingRows,
            int[] from,
            long[] relationWordIds,
            int[] to,
            int[] ordinals,
            long[] metadataBits,
            byte[] interactionKinds,
            int[] topologicalOrder,
            long contentHash) {
        this.atoms = atoms;
        this.outgoingOffsets = outgoingOffsets;
        this.incomingOffsets = incomingOffsets;
        this.incomingRows = incomingRows;
        this.from = from;
        this.relationWordIds = relationWordIds;
        this.to = to;
        this.ordinals = ordinals;
        this.metadataBits = metadataBits;
        this.interactionKinds = interactionKinds;
        this.topologicalOrder = topologicalOrder;
        this.contentHash = contentHash;
    }

    public static M3Dag fromAst(M3Ast ast, long childRelationId) {
        Objects.requireNonNull(ast, "ast");
        Builder builder = new Builder(ast.atoms());
        for (int parent = 0; parent < ast.atoms().atomCount(); parent++) {
            if (ast.atoms().domain(parent) != M3AtomDomain.AST) continue;
            int count = ast.atoms().childCount(parent);
            for (int ordinal = 0; ordinal < count; ordinal++) {
                builder.edge(
                        parent,
                        childRelationId,
                        ast.atoms().childAt(parent, ordinal),
                        ordinal,
                        0L,
                        M3InteractionMode.CONNECT);
            }
        }
        return builder.freeze();
    }

    public M3AtomStore atoms() { return atoms; }
    public int nodeCount() { return atoms.atomCount(); }
    public int edgeCount() { return from.length; }
    public long contentHash() { return contentHash; }
    public int[] topologicalOrder() { return topologicalOrder.clone(); }

    public EdgeView edge(int edgeId) {
        int row = Objects.checkIndex(edgeId, edgeCount());
        return new EdgeView(
                row,
                from[row],
                relationWordIds[row],
                to[row],
                ordinals[row],
                metadataBits[row],
                interactionKind(row));
    }

    public int outgoingCount(int atomId) {
        int row = Objects.checkIndex(atomId, nodeCount());
        return outgoingOffsets[row + 1] - outgoingOffsets[row];
    }

    public EdgeView outgoingEdge(int atomId, int ordinal) {
        int node = Objects.checkIndex(atomId, nodeCount());
        int start = outgoingOffsets[node];
        int count = outgoingOffsets[node + 1] - start;
        return edge(start + Objects.checkIndex(ordinal, count));
    }

    public int incomingCount(int atomId) {
        int row = Objects.checkIndex(atomId, nodeCount());
        return incomingOffsets[row + 1] - incomingOffsets[row];
    }

    public EdgeView incomingEdge(int atomId, int ordinal) {
        int node = Objects.checkIndex(atomId, nodeCount());
        int start = incomingOffsets[node];
        int count = incomingOffsets[node + 1] - start;
        return edge(incomingRows[start + Objects.checkIndex(ordinal, count)]);
    }

    public M3InteractionKind interactionKind(int edgeId) {
        return M3InteractionKind.values()[
                Byte.toUnsignedInt(interactionKinds[Objects.checkIndex(edgeId, edgeCount())])];
    }

    public int[] roots() {
        int count = 0;
        for (int node = 0; node < nodeCount(); node++) if (incomingCount(node) == 0) count++;
        int[] result = new int[count];
        int at = 0;
        for (int node = 0; node < nodeCount(); node++) {
            if (incomingCount(node) == 0) result[at++] = node;
        }
        return result;
    }

    public int[] leaves() {
        int count = 0;
        for (int node = 0; node < nodeCount(); node++) if (outgoingCount(node) == 0) count++;
        int[] result = new int[count];
        int at = 0;
        for (int node = 0; node < nodeCount(); node++) {
            if (outgoingCount(node) == 0) result[at++] = node;
        }
        return result;
    }

    /** Deterministic executable frontiers; no scheduler or execution authority is implied. */
    public int[][] waves() {
        int[] indegree = new int[nodeCount()];
        for (int target : to) indegree[target]++;
        boolean[] done = new boolean[nodeCount()];
        int remaining = nodeCount();
        ArrayList<int[]> waves = new ArrayList<>();
        while (remaining != 0) {
            int count = 0;
            for (int node = 0; node < nodeCount(); node++) {
                if (!done[node] && indegree[node] == 0) count++;
            }
            if (count == 0) throw new IllegalStateException("DAG cycle");
            int[] wave = new int[count];
            int at = 0;
            for (int node = 0; node < nodeCount(); node++) {
                if (!done[node] && indegree[node] == 0) wave[at++] = node;
            }
            for (int node : wave) {
                done[node] = true;
                remaining--;
                for (int row = outgoingOffsets[node]; row < outgoingOffsets[node + 1]; row++) {
                    indegree[to[row]]--;
                }
            }
            waves.add(wave);
        }
        return waves.toArray(int[][]::new);
    }

    public long primitiveBytes() {
        return atoms.primitiveBytes()
                + 4L * (outgoingOffsets.length + incomingOffsets.length + incomingRows.length
                        + from.length + to.length + ordinals.length + topologicalOrder.length)
                + 8L * (relationWordIds.length + metadataBits.length)
                + interactionKinds.length;
    }

    private static void prefix(int[] offsets) {
        for (int i = 1; i < offsets.length; i++) {
            offsets[i] = Math.addExact(offsets[i], offsets[i - 1]);
        }
    }

    private static int[] topological(int nodes, int[] to, int[] outgoingOffsets) {
        int[] indegree = new int[nodes];
        for (int target : to) indegree[target]++;
        PriorityQueue<Integer> ready = new PriorityQueue<>();
        for (int node = 0; node < nodes; node++) if (indegree[node] == 0) ready.add(node);
        int[] result = new int[nodes];
        int at = 0;
        while (!ready.isEmpty()) {
            int node = ready.remove();
            result[at++] = node;
            for (int row = outgoingOffsets[node]; row < outgoingOffsets[node + 1]; row++) {
                int target = to[row];
                if (--indegree[target] == 0) ready.add(target);
            }
        }
        if (at != nodes) throw new IllegalArgumentException("M3 DAG contains a cycle");
        return result;
    }

    private static long contentHash(
            M3AtomStore atoms,
            int[] from,
            long[] relations,
            int[] to,
            int[] ordinals,
            long[] metadata,
            byte[] kinds) {
        long hash = 0x6a09e667f3bcc909L;
        hash = M3AtomStore.mix(hash, atoms.atomCount());
        for (int atom = 0; atom < atoms.atomCount(); atom++) {
            hash = M3AtomStore.mix(hash, atoms.contentHash(atom));
        }
        hash = M3AtomStore.mix(hash, from.length);
        for (int row = 0; row < from.length; row++) {
            hash = M3AtomStore.mix(hash, from[row]);
            hash = M3AtomStore.mix(hash, relations[row]);
            hash = M3AtomStore.mix(hash, to[row]);
            hash = M3AtomStore.mix(hash, ordinals[row]);
            hash = M3AtomStore.mix(hash, metadata[row]);
            hash = M3AtomStore.mix(hash, Byte.toUnsignedInt(kinds[row]));
        }
        return hash;
    }
}
