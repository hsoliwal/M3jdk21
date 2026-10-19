// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.api.MIndexApiRouteTable}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/api/MIndexApiRouteTable.java}
 * blob {@code ebf7d433be38049415ddfe0c95fd4609fe0efc39} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 3aceb2d0b658d256c64104e9a3cf424faa1d43726f20f43ff35bc847fc91a65b}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.api.M3ApiRouteTable}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S12}.
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
 *   <li>{@code nested types not received: Builder, RouteBuild}</li>
 *   <li>{@code member builder(MIndexApiCatalog) (return type)}</li>
 *   <li>{@code member catalog() (return type)}</li>
 *   <li>{@code member routedSources() (return type)}</li>
 * </ul>
 */
public final class M3ApiRouteTable {
    public static final int IDENTITY_MAPPER = 0;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code catalog} of type {@code MIndexApiCatalog}. */
    private int[] catalogLane;

    /** primitive lane for source field {@code sourceRouteOffsets} of type {@code int[]}. */
    private int[] sourceRouteOffsets;

    /** primitive lane for source field {@code targetMembers} of type {@code int[]}. */
    private int[] targetMembers;

    /** primitive lane for source field {@code mapperIds} of type {@code int[]}. */
    private int[] mapperIds;

    /** primitive lane for source field {@code costs} of type {@code long[]}. */
    private long[] costs;

    /** primitive lane for source field {@code weightQ31} of type {@code int[]}. */
    private int[] weightQ31;

    /** primitive lane for source field {@code flags} of type {@code long[]}. */
    private long[] flags;

    /** primitive lane for source field {@code fingerprints} of type {@code long[]}. */
    private long[] fingerprints;

    /** primitive lane for source field {@code argumentMapOffsets} of type {@code int[]}. */
    private int[] argumentMapOffsets;

    /** primitive lane for source field {@code targetArgumentOrdinals} of type {@code int[]}. */
    private int[] targetArgumentOrdinals;

    /** primitive lane for source field {@code targetRouteOffsets} of type {@code int[]}. */
    private int[] targetRouteOffsets;

    /** primitive lane for source field {@code targetRouteRows} of type {@code int[]}. */
    private int[] targetRouteRows;

    /** primitive lane for source field {@code mapperKeys} of type {@code int[]}. */
    private int[] mapperKeys;

    /** primitive lane for source field {@code mapperRouteOffsets} of type {@code int[]}. */
    private int[] mapperRouteOffsets;

    /** primitive lane for source field {@code mapperRouteRows} of type {@code int[]}. */
    private int[] mapperRouteRows;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sha256} of type {@code String}. */
    private int[] sha256Lane;

    private M3ApiRouteTable() {
        throw new M3NotYetReceived("M3ApiRouteTable#<init>/0");
    }

    public int routeCount() {
        throw new M3NotYetReceived("M3ApiRouteTable#routeCount/0");
    }

    public String sha256() {
        throw new M3NotYetReceived("M3ApiRouteTable#sha256/0");
    }

    public int routeCount(int sourceMember) {
        throw new M3NotYetReceived("M3ApiRouteTable#routeCount/1");
    }

    public int routeRow(int sourceMember, int ordinal) {
        throw new M3NotYetReceived("M3ApiRouteTable#routeRow/2");
    }

    public int bestRouteRow(int sourceMember) {
        throw new M3NotYetReceived("M3ApiRouteTable#bestRouteRow/1");
    }

    public int targetMember(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#targetMember/1");
    }

    public int mapperId(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#mapperId/1");
    }

    public long cost(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#cost/1");
    }

    public int weightQ31(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#weightQ31/1");
    }

    public long flags(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#flags/1");
    }

    public long fingerprint(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#fingerprint/1");
    }

    public int argumentMapSize(int routeRow) {
        throw new M3NotYetReceived("M3ApiRouteTable#argumentMapSize/1");
    }

    public int targetArgumentOrdinal(int routeRow, int sourceArgumentOrdinal) {
        throw new M3NotYetReceived("M3ApiRouteTable#targetArgumentOrdinal/2");
    }

    public int incomingRouteCount(int targetMember) {
        throw new M3NotYetReceived("M3ApiRouteTable#incomingRouteCount/1");
    }

    public int incomingRouteRow(int targetMember, int ordinal) {
        throw new M3NotYetReceived("M3ApiRouteTable#incomingRouteRow/2");
    }

    public int mapperRouteCount(int mapperId) {
        throw new M3NotYetReceived("M3ApiRouteTable#mapperRouteCount/1");
    }

    public int mapperRouteRow(int mapperId, int ordinal) {
        throw new M3NotYetReceived("M3ApiRouteTable#mapperRouteRow/2");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3ApiRouteTable#primitivePayloadBytes/0");
    }
}
