// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexMappedArrays}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexMappedArrays.java}
 * blob {@code 638651d22219eb7eb737b194ba67bd5e444609e1} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4073890543aaf9e8df6401b965c6ac740084f635e46f72c531a3948bfd5b53cc}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3MappedArrays}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code ARRAYS}; naming-law family {@code ARRAYS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Views}</li>
 *   <li>{@code member write(Path, String, FrozenChars[], FrozenBytes[], long) (parameter chars)}</li>
 *   <li>{@code member write(Path, String, FrozenChars[], FrozenBytes[], long, BooleanSupplier) (parameter chars)}</li>
 *   <li>{@code member bytes(int) (return type)}</li>
 *   <li>{@code member chars(int) (return type)}</li>
 *   <li>{@code member joinedBytes(int...) (return type)}</li>
 *   <li>{@code member joinedChars(int...) (return type)}</li>
 * </ul>
 */
public final class M3MappedArrays {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code image} of type {@code ByteBuffer}. */
    private int[] imageLane;

    /** primitive lane for source field {@code charRows} of type {@code int}. */
    private int charRows;

    /** primitive lane for source field {@code byteRows} of type {@code int}. */
    private int byteRows;

    /** primitive lane for source field {@code atoms} of type {@code int}. */
    private int atoms;

    /** primitive lane for source field {@code rowsBase} of type {@code int}. */
    private int rowsBase;

    /** primitive lane for source field {@code atomsBase} of type {@code int}. */
    private int atomsBase;

    /** primitive lane for source field {@code payloadBase} of type {@code int}. */
    private int payloadBase;

    /** primitive lane for source field {@code cacheLimit} of type {@code int}. */
    private int cacheLimit;

    /** int handle lane (offsets into parent-owned storage) for source field {@code schema} of type {@code String}. */
    private int[] schemaLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code generation} of type {@code String}. */
    private int[] generationLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code cache} of type {@code LinkedHashMap<Integer, Views>}. */
    private int[] cacheLane;

    private M3MappedArrays() {
        throw new M3NotYetReceived("M3MappedArrays#<init>/0");
    }

    public static M3MappedArrays open(Path path, long maxImageBytes, int maxCachedAtoms) throws IOException {
        throw new M3NotYetReceived("M3MappedArrays#open/3");
    }

    public static M3MappedArrays open(Path path, long maxImageBytes, int maxCachedAtoms, BooleanSupplier cancelled) throws IOException {
        throw new M3NotYetReceived("M3MappedArrays#open/4");
    }

    public void preload(BooleanSupplier cancelled) throws IOException {
        throw new M3NotYetReceived("M3MappedArrays#preload/1");
    }

    public int charRowCount() {
        throw new M3NotYetReceived("M3MappedArrays#charRowCount/0");
    }

    public int byteRowCount() {
        throw new M3NotYetReceived("M3MappedArrays#byteRowCount/0");
    }

    public int atomCount() {
        throw new M3NotYetReceived("M3MappedArrays#atomCount/0");
    }

    public long imageBytes() {
        throw new M3NotYetReceived("M3MappedArrays#imageBytes/0");
    }

    public long uniquePayloadBytes() {
        throw new M3NotYetReceived("M3MappedArrays#uniquePayloadBytes/0");
    }

    public String schema() {
        throw new M3NotYetReceived("M3MappedArrays#schema/0");
    }

    public String generation() {
        throw new M3NotYetReceived("M3MappedArrays#generation/0");
    }

    public int charAtomId(int row) {
        throw new M3NotYetReceived("M3MappedArrays#charAtomId/1");
    }

    public int byteAtomId(int row) {
        throw new M3NotYetReceived("M3MappedArrays#byteAtomId/1");
    }

    public int cachedAtomCount() {
        throw new M3NotYetReceived("M3MappedArrays#cachedAtomCount/0");
    }
}
