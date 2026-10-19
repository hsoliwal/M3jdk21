// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexFormats}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexFormats.java}
 * blob {@code 76d3e0b688d67280bba9c572ceabb1b5c8b497fe} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7a307b9f9168b4acda699832a9d9bfe99c3baad9a26a1edc0afb4f31bad0c486}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Formats}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S07}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexFormats.java(at)cb71799f24ee}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Family, Descriptor, ExternalProviders}</li>
 *   <li>{@code member builtIns() (return type)}</li>
 *   <li>{@code member descriptors() (return type)}</li>
 *   <li>{@code member descriptor(String) (return type)}</li>
 *   <li>{@code member atomGrammar(String) (return type)}</li>
 *   <li>{@code member scanAtoms(String, byte[], int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member atomGrammar(String, List<? extends CharSequence>) (return type)}</li>
 *   <li>{@code member scanAtoms(String, byte[], List<? extends CharSequence>, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member scanAtoms(String, byte[], int) (return type)}</li>
 *   <li>{@code member require(String) (return type)}</li>
 *   <li>{@code member transcode(String, String, byte[], int, int, int, MIndexProgressMonitor) (parameter monitor)}</li>
 * </ul>
 */
public final class M3Formats {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3Formats() {
        throw new M3NotYetReceived("M3Formats#<init>/0");
    }

    public static boolean supports(String selector) {
        throw new M3NotYetReceived("M3Formats#supports/1");
    }

    public static boolean supportsAtomGrammar(String format) {
        throw new M3NotYetReceived("M3Formats#supportsAtomGrammar/1");
    }

    public static List<String> supportedSelectors() {
        throw new M3NotYetReceived("M3Formats#supportedSelectors/0");
    }
}
