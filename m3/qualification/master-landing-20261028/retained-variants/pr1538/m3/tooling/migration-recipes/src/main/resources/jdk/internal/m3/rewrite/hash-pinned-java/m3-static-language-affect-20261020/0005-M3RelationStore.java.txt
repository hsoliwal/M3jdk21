// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable primitive relation graph over canonical M3 atoms.
 *
 * <p>This is the cyclic/general sibling of {@link M3Dag}. Edge mechanics are identical in shape:
 * from + relation-word coordinate + to + ordinal + metadata + interaction kind, with CSR
 * outgoing/incoming indexes. Unlike M3Dag, cycles and symmetric relations are valid.</p>
 */
public final class M3RelationStore {
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
            if (relationWordId < 0L) throw new IllegalArgumentException("relation ID must be >= 0");
            if (ordinal < 0) throw new IllegalArgumentException("edge ordinal must be >= 0");
            M3InteractionKind kind = M3Interaction.classify(
                    atoms.domain(from), atoms.domain(to), Objects.requireNonNull(mode, "mode"));
            edges.add(new Draft(from, relationWordId, to, ordinal, metadataBits, kind));
            return this;
        }

        public M3RelationStore freeze() {
            ensureMutable();
            frozen = true;
            Draft[] sorted = edges.toArray(Draft[]::new);
            Arrays.sort(sorted, Comparator.comparingInt(Draft::from)
                    .thenComparing((a, b) -> Long.compareUnsigned(a.relation(), b.relation()))
                    .thenComparingInt(Draft::ordinal)
                    .thenComparingInt(Draft::to)
                    .thenComparingLong(Draft::metadata)
                    .thenComparingInt(value -> value.kind().ordinal()));
            for (int index = 1; index < sorted.length; index++) {
                if (sorted[index - 1].equals(sorted[index])) {
                    throw new IllegalArgumentException("duplicate relation edge");
                }
            }

            int count = sorted.length;
            int[] from = new int[count];
            long[] relation = new long[count];
            int[] to = new int[count];
            int[] ordinal = new int[count];
            long[] metadata = new long[count];
            byte[] kinds = new byte[count];
            int[] outgoing = new int[atoms.atomCount() + 1];
            int[] incoming = new int[atoms.atomCount() + 1];

            for (int row = 0; row < count; row++) {
                Draft edge = sorted[row];
                from[row] = edge.from();
                relation[row] = edge.relation();
                to[row] = edge.to();
                ordinal[row] = edge.ordinal();
                metadata[row] = edge.metadata();
                kinds[row] = (byte) edge.kind().ordinal();
                outgoing[edge.from() + 1]++;
                incoming[edge.to() + 1]++;
            }
            prefix(outgoing);
            prefix(incoming);

            int[] incomingRows = new int[count];
            int[] cursor = incoming.clone();
            for (int row = 0; row < count; row++) incomingRows[cursor[to[row]]++] = row;

            long hash = 0x510e527fade682d1L;
            for (int atom = 0; atom < atoms.atomCount(); atom++) {
                hash = M3AtomStore.mix(hash, atoms.contentHash(atom));
            }
            for (int row = 0; row < count; row++) {
                hash = M3AtomStore.mix(hash, from[row]);
                hash = M3AtomStore.mix(hash, relation[row]);
                hash = M3AtomStore.mix(hash, to[row]);
                hash = M3AtomStore.mix(hash, ordinal[row]);
                hash = M3AtomStore.mix(hash, metadata[row]);
                hash = M3AtomStore.mix(hash, Byte.toUnsignedInt(kinds[row]));
            }

            return new M3RelationStore(
                    atoms, from, relation, to, ordinal, metadata, kinds,
                    outgoing, incoming, incomingRows, hash);
        }

        private void ensureMutable() {
            if (frozen) throw new IllegalStateException("relation builder already frozen");
        }
    }

    private final M3AtomStore atoms;
    private final int[] from;
    private final long[] relationWordIds;
    private final int[] to;
    private final int[] ordinals;
    private final long[] metadataBits;
    private final byte[] interactionKinds;
    private final int[] outgoingOffsets;
    private final int[] incomingOffsets;
    private final int[] incomingRows;
    private final long contentHash;

    private M3RelationStore(
            M3AtomStore atoms,
            int[] from,
            long[] relationWordIds,
            int[] to,
            int[] ordinals,
            long[] metadataBits,
            byte[] interactionKinds,
            int[] outgoingOffsets,
            int[] incomingOffsets,
            int[] incomingRows,
            long contentHash) {
        this.atoms = atoms;
        this.from = from;
        this.relationWordIds = relationWordIds;
        this.to = to;
        this.ordinals = ordinals;
        this.metadataBits = metadataBits;
        this.interactionKinds = interactionKinds;
        this.outgoingOffsets = outgoingOffsets;
        this.incomingOffsets = incomingOffsets;
        this.incomingRows = incomingRows;
        this.contentHash = contentHash;
    }

    public M3AtomStore atoms() { return atoms; }
    public int edgeCount() { return from.length; }
    public long contentHash() { return contentHash; }

    public EdgeView edge(int edgeId) {
        int row = Objects.checkIndex(edgeId, edgeCount());
        return new EdgeView(row, from[row], relationWordIds[row], to[row], ordinals[row],
                metadataBits[row], interactionKind(row));
    }

    public int outgoingCount(int atomId) {
        int node = Objects.checkIndex(atomId, atoms.atomCount());
        return outgoingOffsets[node + 1] - outgoingOffsets[node];
    }

    public EdgeView outgoingEdge(int atomId, int ordinal) {
        int node = Objects.checkIndex(atomId, atoms.atomCount());
        int start = outgoingOffsets[node];
        return edge(start + Objects.checkIndex(ordinal, outgoingOffsets[node + 1] - start));
    }

    public int incomingCount(int atomId) {
        int node = Objects.checkIndex(atomId, atoms.atomCount());
        return incomingOffsets[node + 1] - incomingOffsets[node];
    }

    public EdgeView incomingEdge(int atomId, int ordinal) {
        int node = Objects.checkIndex(atomId, atoms.atomCount());
        int start = incomingOffsets[node];
        int count = incomingOffsets[node + 1] - start;
        return edge(incomingRows[start + Objects.checkIndex(ordinal, count)]);
    }

    public M3InteractionKind interactionKind(int edgeId) {
        return M3InteractionKind.values()[
                Byte.toUnsignedInt(interactionKinds[Objects.checkIndex(edgeId, edgeCount())])];
    }

    public boolean related(int fromAtomId, long relationWordId, int toAtomId) {
        int fromNode = Objects.checkIndex(fromAtomId, atoms.atomCount());
        Objects.checkIndex(toAtomId, atoms.atomCount());
        for (int row = outgoingOffsets[fromNode]; row < outgoingOffsets[fromNode + 1]; row++) {
            if (relationWordIds[row] == relationWordId && to[row] == toAtomId) return true;
        }
        return false;
    }

    public long primitiveBytes() {
        return atoms.primitiveBytes()
                + 4L * (from.length + to.length + ordinals.length + outgoingOffsets.length
                        + incomingOffsets.length + incomingRows.length)
                + 8L * (relationWordIds.length + metadataBits.length)
                + interactionKinds.length;
    }

    private static void prefix(int[] offsets) {
        for (int index = 1; index < offsets.length; index++) {
            offsets[index] = Math.addExact(offsets[index], offsets[index - 1]);
        }
    }
}
