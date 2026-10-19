// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSchemaCodec}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSchemaCodec.java}
 * blob {@code 86270e4fb15346afdae2a753d8336b3ff6a27503} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2cc80fa4594a0ce15f3c0539b11f2824b38f5117879dc5231027455c20dbaf2d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SchemaCodec}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member decode(byte[], int, int, int) (return type)}</li>
 *   <li>{@code member encode(MIndexDocumentSchema) (parameter schema)}</li>
 * </ul>
 */
public interface M3SchemaCodec {
    String format();
}
