// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexJavaApiContribution}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexJavaApiContribution.java}
 * blob {@code 94f363fc59f9dc823dcc3e07a6d00145be671e9b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 328983c2190334f0d310a02ead42dbd7109bb30bddeb349c8f5f427329660fbc}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3JavaApiContribution}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
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
 *   <li>{@code nested types not received: TypeInfo}</li>
 *   <li>{@code source supertypes not received: implements MIndexUniversalContribution}</li>
 *   <li>{@code member contribute(MIndexUniversalSource.Builder, IProgressMonitor) (parameter builder)}</li>
 * </ul>
 */
public final class M3JavaApiContribution {
    public static final long API_TYPE_META = 0x4d494e444a410001L;

    public static final long API_METHOD_SIGNATURE = 0x4d494e444a410002L;

    public static final long API_CONSTRUCTOR_SIGNATURE = 0x4d494e444a410003L;

    public static final long API_FIELD_META = 0x4d494e444a410004L;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code id} of type {@code String}. */
    private int[] idLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code roots} of type {@code Class<?>[]}. */
    private int[] rootsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sourceSha256} of type {@code String}. */
    private int[] sourceSha256Lane;

    public M3JavaApiContribution(String id, Class<?>... classes) {
        throw new M3NotYetReceived("M3JavaApiContribution#<init>/2");
    }

    public String id() {
        throw new M3NotYetReceived("M3JavaApiContribution#id/0");
    }

    public String revision() {
        throw new M3NotYetReceived("M3JavaApiContribution#revision/0");
    }

    public String sourceSha256() {
        throw new M3NotYetReceived("M3JavaApiContribution#sourceSha256/0");
    }
}
