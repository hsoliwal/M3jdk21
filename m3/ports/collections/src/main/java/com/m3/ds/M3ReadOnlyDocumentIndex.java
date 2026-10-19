// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.nio.file.Path;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexReadOnlyDocumentIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexReadOnlyDocumentIndex.java}
 * blob {@code 571d31325410355d797547db56b081adc8d54d7e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 74db0bafdf2527d20d7fba3dc8851ded3241c096e8b31d10ff5595426b70a13e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3ReadOnlyDocumentIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
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
 *   <li>{@code nested types not received: Limits}</li>
 *   <li>{@code member open(String, Path, List<? extends CharSequence>, Limits, MIndexProgressMonitor) (parameter limits)}</li>
 *   <li>{@code member openMappedLog(Path, MIndexLogSchema, Limits, int) (return type)}</li>
 *   <li>{@code member open(String, byte[], List<? extends CharSequence>, Limits, MIndexProgressMonitor) (parameter limits)}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member values() (return type)}</li>
 *   <li>{@code member access() (return type)}</li>
 *   <li>{@code member logs() (return type)}</li>
 *   <li>{@code member queryLogsByKnownAtoms(long, long, int, int, String, int) (return type)}</li>
 * </ul>
 */
public final class M3ReadOnlyDocumentIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code format} of type {@code String}. */
    private int[] formatLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code structure} of type {@code MIndexReadOnlyStructureIndex}. */
    private int[] structureLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code values} of type {@code MIndexValueTape}. */
    private int[] valuesLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code access} of type {@code MIndexValueAccessIndex}. */
    private int[] accessLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code logs} of type {@code MIndexLogIndex}. */
    private int[] logsLane;

    private M3ReadOnlyDocumentIndex() {
        throw new M3NotYetReceived("M3ReadOnlyDocumentIndex#<init>/0");
    }

    public String format() {
        throw new M3NotYetReceived("M3ReadOnlyDocumentIndex#format/0");
    }

    public M3ReadOnlyStructureIndex structure() {
        throw new M3NotYetReceived("M3ReadOnlyDocumentIndex#structure/0");
    }

    public int knownId(CharSequence text) {
        throw new M3NotYetReceived("M3ReadOnlyDocumentIndex#knownId/1");
    }

    public int fieldByKnownAtom(int mapCoordinate, int atomId) {
        throw new M3NotYetReceived("M3ReadOnlyDocumentIndex#fieldByKnownAtom/2");
    }
}
