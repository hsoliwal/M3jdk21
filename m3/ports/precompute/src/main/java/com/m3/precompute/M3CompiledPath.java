// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexCompiledPath}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexCompiledPath.java}
 * blob {@code 5b3690090063f94505c61a80088264e675d29216} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code bc6ec17565330229eb82ebd11d1e0edb551e71d6ece3e9f36aa11624a92c2b32}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3CompiledPath}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexCompiledPath.java(at)758e114d7c07}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member compile(MIndexPath) (parameter path)}</li>
 *   <li>{@code member resolve(MIndexValueAccessIndex, int) (parameter index)}</li>
 *   <li>{@code member resolveInto(MIndexValueAccessIndex, int[], int, int, int[], int) (parameter index)}</li>
 * </ul>
 */
public final class M3CompiledPath {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code keyTextIds} of type {@code int[]}. */
    private int[] keyTextIds;

    /** primitive lane for source field {@code indexes} of type {@code int[]}. */
    private int[] indexes;

    private M3CompiledPath() {
        throw new M3NotYetReceived("M3CompiledPath#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3CompiledPath#size/0");
    }

    public int keyTextIdAt(int segment) {
        throw new M3NotYetReceived("M3CompiledPath#keyTextIdAt/1");
    }

    public int arrayIndexAt(int segment) {
        throw new M3NotYetReceived("M3CompiledPath#arrayIndexAt/1");
    }
}
