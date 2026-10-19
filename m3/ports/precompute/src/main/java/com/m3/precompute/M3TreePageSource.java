// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexTreePageSource}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexTreePageSource.java}
 * blob {@code 01eafbdcea47b0e9328bcbd62cce4012dc812676} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 93c9f69f5c9194cfca3537c852fa047251177c4de0eb3b7886fb98f451f1933f}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3TreePageSource}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S26}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Page}</li>
 *   <li>{@code constant ROOT (long)}</li>
 *   <li>{@code member roots(int, int, IProgressMonitor) (return type)}</li>
 *   <li>{@code member children(long, int, int, IProgressMonitor) (return type)}</li>
 *   <li>{@code member roots(int, int) (return type)}</li>
 *   <li>{@code member children(long, int, int) (return type)}</li>
 * </ul>
 */
public interface M3TreePageSource {
    long revision();
}
