// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.MIndexApiShapePlanIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/MIndexApiShapePlanIndex.java}
 * blob {@code 443bb4eaa852956cfbc627178e661637f43715e1} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 8f958c59e584ac9be68dcfb3e01be28c8a2974fd694aa96879e054c998948c2b}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.M3ApiShapePlanIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S11}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: PrecomputeKind, ApiSpec, Plan, Builder, CompiledSpec}</li>
 *   <li>{@code member builder() (return type)}</li>
 *   <li>{@code member plan(int) (return type)}</li>
 *   <li>{@code member shapeAt(int) (return type)}</li>
 *   <li>{@code member precomputeKindAt(int) (return type)}</li>
 *   <li>{@code member invoke(int, MIndexLibrary, I, Class<O>, MIndexExecutionContext) (parameter library)}</li>
 *   <li>{@code member invoke(int, MIndexLibrary, I, Class<O>) (parameter library)}</li>
 *   <li>{@code member precomputeKind(MIndexLeetCodeAlgorithms.CanonicalShape) (return type)}</li>
 *   <li>{@code member jniCandidate(MIndexLeetCodeAlgorithms.CanonicalShape) (parameter shape)}</li>
 *   <li>{@code member jiniDistributable(MIndexLeetCodeAlgorithms.CanonicalShape) (parameter shape)}</li>
 * </ul>
 */
public final class M3ApiShapePlanIndex {
    public static final int FLAG_IMMUTABLE_INPUT = 1;

    public static final int FLAG_REPEATED = 1 << 1;

    public static final int FLAG_PRECOMPUTE = 1 << 2;

    public static final int FLAG_REUSABLE_WORKSPACE = 1 << 3;

    public static final int FLAG_JNI_BATCH_CANDIDATE = 1 << 4;

    public static final int FLAG_JINI_DISTRIBUTABLE = 1 << 5;

    public static final int FLAG_EXACT = 1 << 6;

    public static final int FLAG_INDEX_ONLY_HOT_PATH = 1 << 7;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code apiNameIds} of type {@code int[]}. */
    private int[] apiNameIds;

    /** primitive lane for source field {@code algorithmNameIds} of type {@code int[]}. */
    private int[] algorithmNameIds;

    /** primitive lane for source field {@code bindingOrdinals} of type {@code short[]}. */
    private short[] bindingOrdinals;

    /** primitive lane for source field {@code shapeOrdinals} of type {@code byte[]}. */
    private byte[] shapeOrdinals;

    /** primitive lane for source field {@code precomputeOrdinals} of type {@code byte[]}. */
    private byte[] precomputeOrdinals;

    /** primitive lane for source field {@code flags} of type {@code int[]}. */
    private int[] flags;

    /** primitive lane for source field {@code slots} of type {@code int[]}. */
    private int[] slots;

    /** primitive lane for source field {@code fingerprint64} of type {@code long}. */
    private long fingerprint64;

    private M3ApiShapePlanIndex() {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#<init>/0");
    }

    public static M3ApiShapePlanIndex standardAlgorithms() {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#standardAlgorithms/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#size/0");
    }

    public long fingerprint64() {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#fingerprint64/0");
    }

    public int planIndex(int apiNameId) {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#planIndex/1");
    }

    public int planIndex(CharSequence apiName) {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#planIndex/1");
    }

    public int apiNameIdAt(int index) {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#apiNameIdAt/1");
    }

    public int algorithmNameIdAt(int index) {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#algorithmNameIdAt/1");
    }

    public int flagsAt(int index) {
        throw new M3NotYetReceived("M3ApiShapePlanIndex#flagsAt/1");
    }
}
