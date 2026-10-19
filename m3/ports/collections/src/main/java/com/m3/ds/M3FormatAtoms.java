// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexFormatAtoms}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexFormatAtoms.java}
 * blob {@code 7fb64763aa0bff1e099127108d5cbce81eb43b38} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 398b4d088c112e0bfbb30f7dea9ee6dff6a9d32685a37e9ac9a8d2b809ebbc17}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3FormatAtoms}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S07}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member require(String) (return type)}</li>
 *   <li>{@code member compile(String, List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member scan(String, byte[], int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member json(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member toml(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member xml(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member svg(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member yaml(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member properties(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member csv(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member tsv(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member log(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member ini(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member dotenv(List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member lines(List<? extends CharSequence>) (return type)}</li>
 * </ul>
 */
public final class M3FormatAtoms {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3FormatAtoms() {
        throw new M3NotYetReceived("M3FormatAtoms#<init>/0");
    }
}
