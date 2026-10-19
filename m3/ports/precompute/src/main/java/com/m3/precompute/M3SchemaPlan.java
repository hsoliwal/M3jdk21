// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Map;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSchemaPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSchemaPlan.java}
 * blob {@code 2dcf8b978f844e0c08217a4831ecf1060af71763} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 8bbb380b401ad0eb25cdb797d023f252895b3d46be49a17850d8d84dba8a3897}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SchemaPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
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
 *   <li>{@code nested types not received: Field, Builder}</li>
 *   <li>{@code member builder(CharSequence) (return type)}</li>
 *   <li>{@code member compile(Map<String, MIndexValues.Kind>) (parameter fields)}</li>
 *   <li>{@code member acceptsKind(int, MIndexValues.Kind) (parameter kind)}</li>
 *   <li>{@code member validate(MIndexValues, int) (parameter values)}</li>
 * </ul>
 */
public final class M3SchemaPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code nameTextId} of type {@code int}. */
    private int nameTextId;

    /** int handle lane (offsets into parent-owned storage) for source field {@code name} of type {@code String}. */
    private int[] nameLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code fieldNames} of type {@code String[]}. */
    private int[] fieldNamesLane;

    /** primitive lane for source field {@code fieldTextIds} of type {@code int[]}. */
    private int[] fieldTextIds;

    /** primitive lane for source field {@code allowedKindMasks} of type {@code long[]}. */
    private long[] allowedKindMasks;

    /** primitive lane for source field {@code requiredWords} of type {@code long[]}. */
    private long[] requiredWords;

    /** primitive lane for source field {@code slots} of type {@code int[]}. */
    private int[] slots;

    /** primitive lane for source field {@code jsonPrefixesUtf8} of type {@code byte[][]}. */
    private byte[][] jsonPrefixesUtf8;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sha256} of type {@code String}. */
    private int[] sha256Lane;

    /** primitive lane for source field {@code hashCode} of type {@code int}. */
    private int hashCode;

    private M3SchemaPlan() {
        throw new M3NotYetReceived("M3SchemaPlan#<init>/0");
    }

    public int nameTextId() {
        throw new M3NotYetReceived("M3SchemaPlan#nameTextId/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3SchemaPlan#size/0");
    }

    public String sha256() {
        throw new M3NotYetReceived("M3SchemaPlan#sha256/0");
    }

    public String fieldName(int ordinal) {
        throw new M3NotYetReceived("M3SchemaPlan#fieldName/1");
    }

    public int fieldTextId(int ordinal) {
        throw new M3NotYetReceived("M3SchemaPlan#fieldTextId/1");
    }

    public boolean required(int ordinal) {
        throw new M3NotYetReceived("M3SchemaPlan#required/1");
    }

    public byte[] jsonPrefixUtf8(int ordinal) {
        throw new M3NotYetReceived("M3SchemaPlan#jsonPrefixUtf8/1");
    }

    public int fieldOrdinal(CharSequence nameValue) {
        throw new M3NotYetReceived("M3SchemaPlan#fieldOrdinal/1");
    }

    public int fieldOrdinalByTextId(int textId) {
        throw new M3NotYetReceived("M3SchemaPlan#fieldOrdinalByTextId/1");
    }

    public boolean equals(Object other) {
        throw new M3NotYetReceived("M3SchemaPlan#equals/1");
    }

    public int hashCode() {
        throw new M3NotYetReceived("M3SchemaPlan#hashCode/0");
    }
}
