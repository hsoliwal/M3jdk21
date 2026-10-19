// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.MIndexApiShapePlanImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/MIndexApiShapePlanImage.java}
 * blob {@code 74a1f3b969c15a7101a6852d9e80c247a2e44471} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f9bf5363c8070456ba2f3ac2612e6e134fa12ad6c032dbd668ae9be565ee1c65}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.M3ApiShapePlanImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
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
 *   <li>{@code nested types not received: EncodedRow}</li>
 *   <li>{@code member encode(MIndexApiShapePlanIndex) (parameter index)}</li>
 *   <li>{@code member decode(byte[]) (return type)}</li>
 *   <li>{@code member write(Path, MIndexApiShapePlanIndex) (parameter index)}</li>
 *   <li>{@code member read(Path) (return type)}</li>
 * </ul>
 */
public final class M3ApiShapePlanImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3ApiShapePlanImage() {
        throw new M3NotYetReceived("M3ApiShapePlanImage#<init>/0");
    }
}
