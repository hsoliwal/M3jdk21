// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexDocumentSchema}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexDocumentSchema.java}
 * blob {@code 4329654f11684128e1b348923d37c91a04d7abc4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code dfb58630c8ea259948bab9e35249006ff7da3c186402478ac7b92277273ccc21}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3DocumentSchema}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S07}.
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
 *   <li>{@code nested types not received: Validation, Mutable, FrozenBuilder}</li>
 *   <li>{@code member compile(MIndexSchemaDeclaration) (parameter declaration)}</li>
 *   <li>{@code member declaration() (return type)}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 *   <li>{@code member validate(MIndexDocument, boolean) (return type)}</li>
 * </ul>
 */
public final class M3DocumentSchema {
    public static final int FLAG_OPTIONAL = 1;

    public static final int FLAG_NULLABLE = 1 << 1;

    public static final int FLAG_CLOSED = 1 << 2;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** primitive lane for source field {@code flags} of type {@code int[]}. */
    private int[] flags;

    /** primitive lane for source field {@code edgeOffsets} of type {@code int[]}. */
    private int[] edgeOffsets;

    /** primitive lane for source field {@code edgeCounts} of type {@code int[]}. */
    private int[] edgeCounts;

    /** primitive lane for source field {@code edgeKeys} of type {@code int[]}. */
    private int[] edgeKeys;

    /** primitive lane for source field {@code edgeSchemas} of type {@code int[]}. */
    private int[] edgeSchemas;

    /** primitive lane for source field {@code sha256} of type {@code byte[]}. */
    private byte[] sha256;

    private M3DocumentSchema() {
        throw new M3NotYetReceived("M3DocumentSchema#<init>/0");
    }

    public static M3DocumentSchema infer(M3Document... documents) {
        throw new M3NotYetReceived("M3DocumentSchema#infer/1");
    }

    public int nodeCount() {
        throw new M3NotYetReceived("M3DocumentSchema#nodeCount/0");
    }

    public byte[] sha256() {
        throw new M3NotYetReceived("M3DocumentSchema#sha256/0");
    }

    public boolean any(int schemaNode) {
        throw new M3NotYetReceived("M3DocumentSchema#any/1");
    }

    public boolean optional(int schemaNode) {
        throw new M3NotYetReceived("M3DocumentSchema#optional/1");
    }

    public boolean nullable(int schemaNode) {
        throw new M3NotYetReceived("M3DocumentSchema#nullable/1");
    }

    public boolean closed(int schemaNode) {
        throw new M3NotYetReceived("M3DocumentSchema#closed/1");
    }

    public int childCount(int schemaNode) {
        throw new M3NotYetReceived("M3DocumentSchema#childCount/1");
    }

    public int childKeyOrOrdinal(int schemaNode, int child) {
        throw new M3NotYetReceived("M3DocumentSchema#childKeyOrOrdinal/2");
    }

    public int childSchema(int schemaNode, int child) {
        throw new M3NotYetReceived("M3DocumentSchema#childSchema/2");
    }

    public int fieldSchema(int schemaNode, CharSequence key) {
        throw new M3NotYetReceived("M3DocumentSchema#fieldSchema/2");
    }

    public int fieldSchemaByTextId(int schemaNode, int keyTextId) {
        throw new M3NotYetReceived("M3DocumentSchema#fieldSchemaByTextId/2");
    }

    public int additionalSchema(int schemaNode) {
        throw new M3NotYetReceived("M3DocumentSchema#additionalSchema/1");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3DocumentSchema#retainedPayloadBytes/0");
    }
}
