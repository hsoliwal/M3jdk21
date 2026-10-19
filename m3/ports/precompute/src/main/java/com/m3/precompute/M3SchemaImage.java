// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSchemaImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSchemaImage.java}
 * blob {@code 2df1b80445b208a367dcb32e6be7eba3925dc16e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 8225046b6500172a2f28e2cf72ddabc46bb633fa7a3559f1b07473c769abcfe9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SchemaImage}); projection
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
 *   <li>{@code member write(MIndexDocumentSchema) (parameter schema)}</li>
 *   <li>{@code member read(byte[]) (return type)}</li>
 * </ul>
 */
public final class M3SchemaImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3SchemaImage() {
        throw new M3NotYetReceived("M3SchemaImage#<init>/0");
    }
}
