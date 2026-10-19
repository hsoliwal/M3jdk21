// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.io.IOException;
import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.api.MIndexApiPrecompute}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/api/MIndexApiPrecompute.java}
 * blob {@code 6031bff1d0741b92275b1fece7c7973372f5e1a7} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2582a18c1ddfdbde20d1772cfdb9fc972f9f46ec80c68816bffae4ad28632d99}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.api.M3ApiPrecompute}); projection
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
 *   <li>{@code member catalog(Class<?>...) (return type)}</li>
 *   <li>{@code member catalogDeep(Class<?>...) (return type)}</li>
 *   <li>{@code member routes(MIndexApiCatalog) (return type)}</li>
 *   <li>{@code member snapshot(MIndexApiCatalog, MIndexApiRouteTable) (parameter catalog)}</li>
 *   <li>{@code member writeRuntimeImage(Path, MIndexApiSnapshot, IProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member openRuntimeImage(Path) (return type)}</li>
 * </ul>
 */
public final class M3ApiPrecompute {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3ApiPrecompute() {
        throw new M3NotYetReceived("M3ApiPrecompute#<init>/0");
    }

    public static void writeRuntimeImage(Path target, M3ApiSnapshot snapshot) throws IOException {
        throw new M3NotYetReceived("M3ApiPrecompute#writeRuntimeImage/2");
    }
}
