// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSchema}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSchema.java}
 * blob {@code c9b100e3aae117cd13c6f9351026c85ba84f0ed2} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 998617159ac1084d384cbb45fc88d0e87a0c30e17eff7013d8feeca770cff223}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Schema}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
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
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexSchema.java(at)74e57ab26d3f}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: TapeRecord, TapeTable}</li>
 *   <li>{@code member compile(MIndexValues.Builder, Map<String, MIndexValues.Kind>) (parameter builder)}</li>
 *   <li>{@code member plan() (return type)}</li>
 *   <li>{@code member tapeKeyIndex() (return type)}</li>
 *   <li>{@code member readJsonTape(byte[], int, int) (return type)}</li>
 *   <li>{@code member readJsonArrayTape(byte[], int, int) (return type)}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 *   <li>{@code member encode(MIndexValues.Builder, Object...) (parameter builder)}</li>
 *   <li>{@code member decode(MIndexValues, int) (parameter store)}</li>
 *   <li>{@code member writeJson(MIndexValues, int) (parameter store)}</li>
 * </ul>
 */
public final class M3Schema {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code owner} of type {@code MIndexValues.Builder}. */
    private int[] ownerLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code plan} of type {@code MIndexSchemaPlan}. */
    private int[] planLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code kinds} of type {@code MIndexValues.Kind[]}. */
    private int[] kindsLane;

    /** primitive lane for source field {@code keyNodes} of type {@code int[]}. */
    private int[] keyNodes;

    /** int handle lane (offsets into parent-owned storage) for source field {@code quotedJsonKeys} of type {@code String[]}. */
    private int[] quotedJsonKeysLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code tapeKeyIndex} of type {@code MIndexUtf8KeyIndex}. */
    private int[] tapeKeyIndexLane;

    /** primitive lane for source field {@code tapeKeyIds} of type {@code int[]}. */
    private int[] tapeKeyIds;

    private M3Schema() {
        throw new M3NotYetReceived("M3Schema#<init>/0");
    }

    public String sha256() {
        throw new M3NotYetReceived("M3Schema#sha256/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3Schema#size/0");
    }

    public String keyPrefix(int index) {
        throw new M3NotYetReceived("M3Schema#keyPrefix/1");
    }

    public String name(int index) {
        throw new M3NotYetReceived("M3Schema#name/1");
    }

    public int[] readJson(byte[] input, int maxNodes, int maxLinks, int maxBytes) {
        throw new M3NotYetReceived("M3Schema#readJson/4");
    }
}
