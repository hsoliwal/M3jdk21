// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexLiteralEffectImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexLiteralEffectImage.java}
 * blob {@code 1c03efd0756a8d9aa0e4a813ccf3e832dcc30b2e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 9cfdf042ececa51ee6306316361e1ffafb4f90289e595aafc9cbbf94e2af9da9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3LiteralEffectImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S23}.
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
 *   <li>{@code member write(Path, MIndexMappedArrays, CharSequence, long, long, IProgressMonitor) (parameter owner)}</li>
 *   <li>{@code member openVerified(Path, MIndexMappedArrays, CharSequence, String, long, IProgressMonitor) (return type)}</li>
 *   <li>{@code member open(Path, MIndexMappedArrays, CharSequence, long, IProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3LiteralEffectImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3LiteralEffectImage() {
        throw new M3NotYetReceived("M3LiteralEffectImage#<init>/0");
    }
}
