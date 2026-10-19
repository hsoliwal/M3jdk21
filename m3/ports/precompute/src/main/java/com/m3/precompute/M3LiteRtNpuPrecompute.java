// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexLiteRtNpuPrecompute}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexLiteRtNpuPrecompute.java}
 * blob {@code cc75d2905064ba09ba8adb3fe9a7e2873e9f00e2} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code faf84caefb7dcc032e40e7e81f14fe5c1ba5078fff3e85d5728ff40a550713da}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3LiteRtNpuPrecompute}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3LiteRtNpuPrecompute {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3LiteRtNpuPrecompute() {
        throw new M3NotYetReceived("M3LiteRtNpuPrecompute#<init>/0");
    }

    public static boolean available() {
        throw new M3NotYetReceived("M3LiteRtNpuPrecompute#available/0");
    }

    public static byte[] inferInt8(Path modelFile, byte[] inputRowMajor, int rows, int inputWidth, int outputWidth, boolean requireNonCpuFullyAccelerated) {
        throw new M3NotYetReceived("M3LiteRtNpuPrecompute#inferInt8/6");
    }
}
