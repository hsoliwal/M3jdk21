// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexDeferredDocument}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexDeferredDocument.java}
 * blob {@code 531db0c05d4398d749c6300bbee6fe90a7c363fc} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 40a97ed5fd1703e62c212fdc41736a2f80b5a9948762a8e72044ca6827e1aa22}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3DeferredDocument}); projection
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
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexDeferredDocument.java(at)2f1e4886d32b}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member index(List<? extends CharSequence>, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member index(int) (return type)}</li>
 * </ul>
 */
public final class M3DeferredDocument {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code codec} of type {@code ValueCodec}. */
    private int[] codecLane;

    /** primitive lane for source field {@code source} of type {@code byte[]}. */
    private byte[] source;

    /** primitive lane for source field {@code maxNodes} of type {@code int}. */
    private int maxNodes;

    /** primitive lane for source field {@code maxLinks} of type {@code int}. */
    private int maxLinks;

    /** primitive lane for source field {@code maxBytes} of type {@code int}. */
    private int maxBytes;

    /** int handle lane (offsets into parent-owned storage) for source field {@code materialized} of type {@code MIndexDocument}. */
    private int[] materializedLane;

    private M3DeferredDocument() {
        throw new M3NotYetReceived("M3DeferredDocument#<init>/0");
    }

    public static M3DeferredDocument open(String format, byte[] source, int maxNodes, int maxLinks, int maxBytes) {
        throw new M3NotYetReceived("M3DeferredDocument#open/5");
    }

    public String format() {
        throw new M3NotYetReceived("M3DeferredDocument#format/0");
    }

    public boolean supportsSelectiveMaterialization() {
        throw new M3NotYetReceived("M3DeferredDocument#supportsSelectiveMaterialization/0");
    }

    public boolean isMaterialized() {
        throw new M3NotYetReceived("M3DeferredDocument#isMaterialized/0");
    }

    public int sourceBytes() {
        throw new M3NotYetReceived("M3DeferredDocument#sourceBytes/0");
    }

    public boolean supportsLexicalIndex() {
        throw new M3NotYetReceived("M3DeferredDocument#supportsLexicalIndex/0");
    }

    public M3Document select(CharSequence pointer) {
        throw new M3NotYetReceived("M3DeferredDocument#select/1");
    }

    public M3Document materialize() {
        throw new M3NotYetReceived("M3DeferredDocument#materialize/0");
    }

    public byte[] sourceCopy() {
        throw new M3NotYetReceived("M3DeferredDocument#sourceCopy/0");
    }
}
