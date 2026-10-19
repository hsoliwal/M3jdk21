// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.List;
import java.util.Map;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSchemaDeclaration}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSchemaDeclaration.java}
 * blob {@code 3d3e59f13c9b8119d14f974a27a96e69603fb1ee} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 07b85f78e29b4fd9b7cd83d15ba35d0b97f3dd2b5049b36e9058f00042d48646}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SchemaDeclaration}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
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
 *   <li>{@code member scalar(MIndexValues.Kind) (parameter kind)}</li>
 *   <li>{@code member positional(MIndexValues.Kind, List<M3SchemaDeclaration>) (parameter kind)}</li>
 *   <li>{@code member kind() (return type)}</li>
 * </ul>
 */
public final class M3SchemaDeclaration {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code kind} of type {@code MIndexValues.Kind}. */
    private int[] kindLane;

    /** primitive lane for source field {@code any} of type {@code boolean}. */
    private boolean any;

    /** primitive lane for source field {@code optional} of type {@code boolean}. */
    private boolean optional;

    /** primitive lane for source field {@code nullable} of type {@code boolean}. */
    private boolean nullable;

    /** primitive lane for source field {@code closed} of type {@code boolean}. */
    private boolean closed;

    /** int handle lane (offsets into parent-owned storage) for source field {@code fields} of type {@code Map<String, M3SchemaDeclaration>}. */
    private int[] fieldsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code additionalProperties} of type {@code M3SchemaDeclaration}. */
    private int[] additionalPropertiesLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code element} of type {@code M3SchemaDeclaration}. */
    private int[] elementLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code positional} of type {@code List<M3SchemaDeclaration>}. */
    private int[] positionalLane;

    private M3SchemaDeclaration() {
        throw new M3NotYetReceived("M3SchemaDeclaration#<init>/0");
    }

    public static M3SchemaDeclaration any() {
        throw new M3NotYetReceived("M3SchemaDeclaration#any/0");
    }

    public static M3SchemaDeclaration map(Map<String, M3SchemaDeclaration> fields, boolean closed) {
        throw new M3NotYetReceived("M3SchemaDeclaration#map/2");
    }

    public static M3SchemaDeclaration map(Map<String, M3SchemaDeclaration> fields, M3SchemaDeclaration additionalProperties, boolean closed) {
        throw new M3NotYetReceived("M3SchemaDeclaration#map/3");
    }

    public static M3SchemaDeclaration sequence(M3SchemaDeclaration element) {
        throw new M3NotYetReceived("M3SchemaDeclaration#sequence/1");
    }

    public static M3SchemaDeclaration table(M3SchemaDeclaration element) {
        throw new M3NotYetReceived("M3SchemaDeclaration#table/1");
    }

    public M3SchemaDeclaration optional() {
        throw new M3NotYetReceived("M3SchemaDeclaration#optional/0");
    }

    public M3SchemaDeclaration nullable() {
        throw new M3NotYetReceived("M3SchemaDeclaration#nullable/0");
    }

    public M3SchemaDeclaration closed() {
        throw new M3NotYetReceived("M3SchemaDeclaration#closed/0");
    }

    public boolean anyType() {
        throw new M3NotYetReceived("M3SchemaDeclaration#anyType/0");
    }

    public boolean isOptional() {
        throw new M3NotYetReceived("M3SchemaDeclaration#isOptional/0");
    }

    public boolean isNullable() {
        throw new M3NotYetReceived("M3SchemaDeclaration#isNullable/0");
    }

    public boolean isClosed() {
        throw new M3NotYetReceived("M3SchemaDeclaration#isClosed/0");
    }

    public Map<String, M3SchemaDeclaration> fields() {
        throw new M3NotYetReceived("M3SchemaDeclaration#fields/0");
    }

    public M3SchemaDeclaration additionalProperties() {
        throw new M3NotYetReceived("M3SchemaDeclaration#additionalProperties/0");
    }

    public M3SchemaDeclaration element() {
        throw new M3NotYetReceived("M3SchemaDeclaration#element/0");
    }

    public List<M3SchemaDeclaration> positional() {
        throw new M3NotYetReceived("M3SchemaDeclaration#positional/0");
    }
}
