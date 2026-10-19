// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexStructureBuilder}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructureBuilder.java}
 * blob {@code af711657175d422dae13408aee5957030d04ce58} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 20fc21f5d096642302e96677e411ccc52be9120fbf328dbeecf6d136f3b6fe09}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3StructureBuilder}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
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
 *   <li>{@code nested types not received: NodeKey}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member MIndexStructureBuilder(IndexResolver, int) (parameter resolver)}</li>
 *   <li>{@code member resolver() (return type)}</li>
 *   <li>{@code member index(CharSequence) (return type)}</li>
 *   <li>{@code member text(MIndexString) (parameter value)}</li>
 *   <li>{@code member map(Map<MIndexString, Integer>) (parameter entries)}</li>
 *   <li>{@code member metadata(int, MIndexRepresentationMetadata) (parameter value)}</li>
 * </ul>
 */
public final class M3StructureBuilder {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code resolver} of type {@code IndexResolver}. */
    private int[] resolverLane;

    /** primitive lane for source field {@code languageId} of type {@code int}. */
    private int languageId;

    /** primitive lane for source field {@code types} of type {@code byte[]}. */
    private byte[] types;

    /** primitive lane for source field {@code first} of type {@code long[]}. */
    private long[] first;

    /** primitive lane for source field {@code second} of type {@code long[]}. */
    private long[] second;

    /** primitive lane for source field {@code hashes} of type {@code long[]}. */
    private long[] hashes;

    /** primitive lane for source field {@code nodeCount} of type {@code int}. */
    private int nodeCount;

    /** primitive lane for source field {@code children} of type {@code int[]}. */
    private int[] children;

    /** primitive lane for source field {@code childCount} of type {@code int}. */
    private int childCount;

    /** primitive lane for source field {@code binaryArena} of type {@code byte[]}. */
    private byte[] binaryArena;

    /** primitive lane for source field {@code binarySize} of type {@code int}. */
    private int binarySize;

    /** int handle lane (offsets into parent-owned storage) for source field {@code texts} of type {@code MIndexString[]}. */
    private int[] textsLane;

    /** primitive lane for source field {@code textCount} of type {@code int}. */
    private int textCount;

    /** int handle lane (offsets into parent-owned storage) for source field {@code textIds} of type {@code Map<MIndexString, Integer>}. */
    private int[] textIdsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code nodes} of type {@code Map<NodeKey, Integer>}. */
    private int[] nodesLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code metadata} of type {@code Map<Integer, List<MIndexRepresentationMetadata>>}. */
    private int[] metadataLane;

    private M3StructureBuilder() {
        throw new M3NotYetReceived("M3StructureBuilder#<init>/0");
    }

    public int languageId() {
        throw new M3NotYetReceived("M3StructureBuilder#languageId/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3StructureBuilder#size/0");
    }

    public int nil() {
        throw new M3NotYetReceived("M3StructureBuilder#nil/0");
    }

    public int bool(boolean value) {
        throw new M3NotYetReceived("M3StructureBuilder#bool/1");
    }

    public int integer(int value) {
        throw new M3NotYetReceived("M3StructureBuilder#integer/1");
    }

    public int longValue(long value) {
        throw new M3NotYetReceived("M3StructureBuilder#longValue/1");
    }

    public int floatValue(float value) {
        throw new M3NotYetReceived("M3StructureBuilder#floatValue/1");
    }

    public int doubleValue(double value) {
        throw new M3NotYetReceived("M3StructureBuilder#doubleValue/1");
    }

    public int decimal(BigDecimal value) {
        throw new M3NotYetReceived("M3StructureBuilder#decimal/1");
    }

    public int text(CharSequence value) {
        throw new M3NotYetReceived("M3StructureBuilder#text/1");
    }

    public int binary(byte[] value) {
        throw new M3NotYetReceived("M3StructureBuilder#binary/1");
    }

    public int temporal(Instant value) {
        throw new M3NotYetReceived("M3StructureBuilder#temporal/1");
    }

    public int tuple(int... values) {
        throw new M3NotYetReceived("M3StructureBuilder#tuple/1");
    }

    public int sequence(int... values) {
        throw new M3NotYetReceived("M3StructureBuilder#sequence/1");
    }

    public int table(int... rows) {
        throw new M3NotYetReceived("M3StructureBuilder#table/1");
    }

    public int mapStrings(Map<? extends CharSequence, Integer> entries) {
        throw new M3NotYetReceived("M3StructureBuilder#mapStrings/1");
    }

    public int edge(int left, int relation, int right) {
        throw new M3NotYetReceived("M3StructureBuilder#edge/3");
    }

    public int ref(int target) {
        throw new M3NotYetReceived("M3StructureBuilder#ref/1");
    }

    public int event(Instant time, int source, int payload) {
        throw new M3NotYetReceived("M3StructureBuilder#event/3");
    }

    public M3Structure freeze(int rootId) {
        throw new M3NotYetReceived("M3StructureBuilder#freeze/1");
    }
}
