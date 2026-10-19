// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.io.IOException;
import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.api.MIndexApiRuntimeImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/api/MIndexApiRuntimeImage.java}
 * blob {@code e8166c0dbba384b40e6fae79aa11ee02c799cf87} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4eb0a55d87bea833841ee1c12ff2ca2977a2dc23cd0039012227c81e2f6f7182}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.api.M3ApiRuntimeImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S20}.
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
 *   <li>{@code nested types not received: Layout}</li>
 *   <li>{@code member write(Path, MIndexApiSnapshot, IProgressMonitor) (parameter monitor)}</li>
 * </ul>
 */
public final class M3ApiRuntimeImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3ApiRuntimeImage() {
        throw new M3NotYetReceived("M3ApiRuntimeImage#<init>/0");
    }

    public static void write(Path target, M3ApiSnapshot snapshot) throws IOException {
        throw new M3NotYetReceived("M3ApiRuntimeImage#write/2");
    }
}
