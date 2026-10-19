// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexCompiledSchema}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexCompiledSchema.java}
 * blob {@code 93a471574563b793d59cd931bc91f6f563d16cc5} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 178971a47bead558fbc0e8664566ca73b1f4776a5655330e54efa976a3972b6c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3CompiledSchema}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S21}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member compile(MIndexSchema, MIndexStructuralStore) (parameter schema)}</li>
 *   <li>{@code member compile(MIndexSchema, MIndexStructuralStore, IProgressMonitor) (parameter schema)}</li>
 *   <li>{@code member schema() (return type)}</li>
 *   <li>{@code member store() (return type)}</li>
 *   <li>{@code member fieldIndex() (return type)}</li>
 *   <li>{@code member field(int) (return type)}</li>
 * </ul>
 */
public final class M3CompiledSchema {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code schema} of type {@code MIndexSchema}. */
    private int[] schemaLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code store} of type {@code MIndexStructuralStore}. */
    private int[] storeLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code fieldIndex} of type {@code MIndexFrozenLatin1Index}. */
    private int[] fieldIndexLane;

    /** primitive lane for source field {@code keyNodeIds} of type {@code int[]}. */
    private int[] keyNodeIds;

    private M3CompiledSchema() {
        throw new M3NotYetReceived("M3CompiledSchema#<init>/0");
    }

    public int fieldCount() {
        throw new M3NotYetReceived("M3CompiledSchema#fieldCount/0");
    }

    public int fieldOrdinalPrepared(byte[] source, int offset, int length, int javaHash, long bitSignal64) {
        throw new M3NotYetReceived("M3CompiledSchema#fieldOrdinalPrepared/5");
    }

    public int fieldOrdinal(byte[] source, int offset, int length) {
        throw new M3NotYetReceived("M3CompiledSchema#fieldOrdinal/3");
    }

    public int keyNodeId(int ordinal) {
        throw new M3NotYetReceived("M3CompiledSchema#keyNodeId/1");
    }

    public long keyTokenId(int ordinal) {
        throw new M3NotYetReceived("M3CompiledSchema#keyTokenId/1");
    }

    public int parseLatin1ValuePrepared(int ordinal, byte[] source, int offset, int length, int javaHash, long bitSignal64) {
        throw new M3NotYetReceived("M3CompiledSchema#parseLatin1ValuePrepared/6");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3CompiledSchema#retainedPayloadBytes/0");
    }
}
