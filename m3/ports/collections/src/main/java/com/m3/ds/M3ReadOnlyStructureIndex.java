// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.nio.file.Path;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexReadOnlyStructureIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexReadOnlyStructureIndex.java}
 * blob {@code 2aa40074530d3c34c75bcc71ff3e268402fdb2a4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4e7c3a0c34f8c2460651e94f0d90e83f1d3720133a88cf891c7d3c1ce5e07592}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3ReadOnlyStructureIndex}); projection
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
 *   <li>{@code nested types not received: SearchImage}</li>
 *   <li>{@code member scan(byte[], MIndexAtomGrammar, int, MIndexProgressMonitor) (parameter grammar)}</li>
 *   <li>{@code member scan(String, byte[], int, MIndexProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member scan(String, byte[], List<? extends CharSequence>, int, MIndexProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member scan(Path, String, List<? extends CharSequence>, int, int, MIndexProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member atoms() (return type)}</li>
 *   <li>{@code member postings() (return type)}</li>
 *   <li>{@code member grammar() (return type)}</li>
 *   <li>{@code member cursor() (return type)}</li>
 *   <li>{@code member knownAtom(int) (return type)}</li>
 *   <li>{@code member searchImage(MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member searchImage() (return type)}</li>
 *   <li>{@code member searchImage(long, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member reconstruct() (return type)}</li>
 * </ul>
 */
public final class M3ReadOnlyStructureIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code atoms} of type {@code MIndexAtomTape}. */
    private int[] atomsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code postings} of type {@code MIndexAtomPostings}. */
    private int[] postingsLane;

    private M3ReadOnlyStructureIndex() {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#<init>/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#retainedPrimitiveBytes/0");
    }

    public int atomContainingByte(int byteOffset) {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#atomContainingByte/1");
    }

    public int knownId(CharSequence value) {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#knownId/1");
    }

    public int knownId(CharSequence value, int offset, int length) {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#knownId/3");
    }

    public int occurrenceCount(int atomId) {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#occurrenceCount/1");
    }

    public int occurrenceAtom(int atomId, int ordinal) {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#occurrenceAtom/2");
    }

    public int occurrenceStartByte(int atomId, int ordinal) {
        throw new M3NotYetReceived("M3ReadOnlyStructureIndex#occurrenceStartByte/2");
    }
}
