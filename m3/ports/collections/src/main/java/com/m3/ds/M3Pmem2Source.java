// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexPmem2Source}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexPmem2Source.java}
 * blob {@code 4370faaca4713c46639cc799a26e2e15e9de2d3e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 5a77ced77ff5fe37f365097f7f1611989c11a98091314356170059c1152c1497}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Pmem2Source}); projection
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
 *   <li>{@code source supertypes not received: implements MIndexSource}</li>
 *   <li>{@code member open(Path, MIndexPmem2ShardStager.StageEvidence, MIndexProgressMonitor) (parameter evidence)}</li>
 *   <li>{@code member open(Path, MIndexPmem2ShardStager.StageEvidence, boolean, MIndexProgressMonitor) (parameter evidence)}</li>
 * </ul>
 */
public final class M3Pmem2Source {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code region} of type {@code MIndexPmem2Region}. */
    private int[] regionLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code evidence} of type {@code MIndexPmem2ShardStager.StageEvidence}. */
    private int[] evidenceLane;

    /** primitive lane for source field {@code ownsRegion} of type {@code boolean}. */
    private boolean ownsRegion;

    /** primitive lane for source field {@code cache} of type {@code byte[]}. */
    private byte[] cache;

    /** primitive lane for source field {@code cacheStart} of type {@code long}. */
    private long cacheStart;

    /** primitive lane for source field {@code cacheLength} of type {@code int}. */
    private int cacheLength;

    /** primitive lane for source field {@code closed} of type {@code boolean}. */
    private boolean closed;

    private M3Pmem2Source() {
        throw new M3NotYetReceived("M3Pmem2Source#<init>/0");
    }

    public long length() {
        throw new M3NotYetReceived("M3Pmem2Source#length/0");
    }

    public byte byteAt(long offset) throws IOException {
        throw new M3NotYetReceived("M3Pmem2Source#byteAt/1");
    }

    public void read(long offset, byte[] target, int targetOffset, int count) throws IOException {
        throw new M3NotYetReceived("M3Pmem2Source#read/4");
    }

    public ByteBuffer slice(long offset, int count) throws IOException {
        throw new M3NotYetReceived("M3Pmem2Source#slice/2");
    }

    public String sha256() throws IOException {
        throw new M3NotYetReceived("M3Pmem2Source#sha256/0");
    }

    public void verifyUnchanged() throws IOException {
        throw new M3NotYetReceived("M3Pmem2Source#verifyUnchanged/0");
    }

    public void close() throws IOException {
        throw new M3NotYetReceived("M3Pmem2Source#close/0");
    }
}
