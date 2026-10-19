// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexWritableDeltaSnapshot}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexWritableDeltaSnapshot.java}
 * blob {@code c46c567554bf4ab80e28c49931e95723864fb2ef} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 0e17852ad10cedefa02b7f9fc0913c2abf80f87550d6f8538c0176ecce24ba91}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3WritableDeltaSnapshot}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S26}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3WritableDeltaSnapshot(int languageId, long baseCoordinateFingerprint, long deltaGeneration, String[] spellings, long[] overlayIds) {
    public int size() {
        throw new M3NotYetReceived("M3WritableDeltaSnapshot#size/0");
    }

    public String[] spellings() {
        throw new M3NotYetReceived("M3WritableDeltaSnapshot#spellings/0");
    }

    public long[] overlayIds() {
        throw new M3NotYetReceived("M3WritableDeltaSnapshot#overlayIds/0");
    }
}
