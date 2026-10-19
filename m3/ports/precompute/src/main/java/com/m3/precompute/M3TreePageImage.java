// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.tabletree.MIndexTreePageImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/tabletree-bridge/src/main/java/com/synexia/mindex/tabletree/MIndexTreePageImage.java}
 * blob {@code 366eab86cd7215d9429161f526016f5bcbb7d97a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a1be95a0cea14eda3075c8c55020ec2f6efcd89f5289087a12c97c595b13560d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.tabletree.M3TreePageImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member fromPage(long, MIndexTreePage) (parameter page)}</li>
 *   <li>{@code member materialize(MIndexViewSource) (return type)}</li>
 * </ul>
 */
public record M3TreePageImage(long projectionFingerprint, int parentCoordinate, int offset, int limit, int totalChildren, int[] coordinates, long fingerprint64) {
    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3TreePageImage#retainedPayloadBytes/0");
    }

    public int[] coordinates() {
        throw new M3NotYetReceived("M3TreePageImage#coordinates/0");
    }
}
