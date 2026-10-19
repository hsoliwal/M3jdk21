// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexOperand}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexOperand.java}
 * blob {@code 2e6b2dccc73ab82756fd005ba7819cafb8c6138b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 36136c9fb4240918472f4b57f30bf6694cf7d5286986cdcdb082d55896679e92}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3Operand}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component coordinate erased to Object (source type MIndexCoordinate)}</li>
 * </ul>
 */
public record M3Operand(Object coordinate, M3Role role) {
}
