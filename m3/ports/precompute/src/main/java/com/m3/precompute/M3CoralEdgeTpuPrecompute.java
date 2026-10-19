// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexCoralEdgeTpuPrecompute}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexCoralEdgeTpuPrecompute.java}
 * blob {@code 2b255e9ef86bd67d813c3b67d36be61a91e9a404} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f6660f5fc6443d67a9a2da0fb7331514650f51871c265fe4a7f678ed5a7e7ac3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3CoralEdgeTpuPrecompute}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3CoralEdgeTpuPrecompute {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3CoralEdgeTpuPrecompute() {
        throw new M3NotYetReceived("M3CoralEdgeTpuPrecompute#<init>/0");
    }

    public static boolean available() {
        throw new M3NotYetReceived("M3CoralEdgeTpuPrecompute#available/0");
    }

    public static int deviceCount() {
        throw new M3NotYetReceived("M3CoralEdgeTpuPrecompute#deviceCount/0");
    }

    public static byte[] inferInt8(Path modelFile, byte[] inputRowMajor, int rows, int inputWidth, int outputWidth, int deviceIndex) {
        throw new M3NotYetReceived("M3CoralEdgeTpuPrecompute#inferInt8/6");
    }
}
