// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexMappedStructureIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexMappedStructureIndex.java}
 * blob {@code e3a1501caf48fe739767aac7077ea86048a0f536} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b655ea0b539eb97553829e203a3a284a286c6717cce0d1bef83fa21e73893cbb}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3MappedStructureIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
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
 *   <li>{@code source supertypes not received: implements AutoCloseable}</li>
 *   <li>{@code member scan(Path, String, List<? extends CharSequence>, long, long, int, MIndexProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member scan(MIndexSource, long, long, String, List<? extends CharSequence>, long, MIndexProgressMonitor) (parameter source)}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member grammar() (return type)}</li>
 *   <li>{@code member knownAtom(int) (return type)}</li>
 *   <li>{@code member reconstruct(int) (return type)}</li>
 * </ul>
 */
public final class M3MappedStructureIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexSource}. */
    private int[] sourceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code atoms} of type {@code MIndexLongAtomTape}. */
    private int[] atomsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code postings} of type {@code MIndexLongAtomPostings}. */
    private int[] postingsLane;

    /** primitive lane for source field {@code ownsSource} of type {@code boolean}. */
    private boolean ownsSource;

    private M3MappedStructureIndex() {
        throw new M3NotYetReceived("M3MappedStructureIndex#<init>/0");
    }

    public M3LongAtomTape atoms() {
        throw new M3NotYetReceived("M3MappedStructureIndex#atoms/0");
    }

    public M3LongAtomCursor cursor() {
        throw new M3NotYetReceived("M3MappedStructureIndex#cursor/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3MappedStructureIndex#retainedPrimitiveBytes/0");
    }

    public long atomContainingByte(long byteOffset) {
        throw new M3NotYetReceived("M3MappedStructureIndex#atomContainingByte/1");
    }

    public int knownId(CharSequence value) {
        throw new M3NotYetReceived("M3MappedStructureIndex#knownId/1");
    }

    public long occurrenceCount(int atomId) {
        throw new M3NotYetReceived("M3MappedStructureIndex#occurrenceCount/1");
    }

    public long occurrenceAtom(int atomId, long ordinal) {
        throw new M3NotYetReceived("M3MappedStructureIndex#occurrenceAtom/2");
    }

    public long occurrenceStartByte(int atomId, long ordinal) {
        throw new M3NotYetReceived("M3MappedStructureIndex#occurrenceStartByte/2");
    }

    public String sourceSha256() throws IOException {
        throw new M3NotYetReceived("M3MappedStructureIndex#sourceSha256/0");
    }

    public void verifyUnchanged() throws IOException {
        throw new M3NotYetReceived("M3MappedStructureIndex#verifyUnchanged/0");
    }

    public void close() throws IOException {
        throw new M3NotYetReceived("M3MappedStructureIndex#close/0");
    }
}
