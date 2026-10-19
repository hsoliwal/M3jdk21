// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.compiler.MIndexCompiledTemplate}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexCompiledTemplate.java}
 * blob {@code af4726cbaf4d80379eae8175caaa4435490c6fa4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 39d613cdafda4b173eb335641c3cdcac6edf4926a92bb105baafe69cddee134c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.compiler.M3CompiledTemplate}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component sourceAst erased to Object (source type MIndexAST)}</li>
 *   <li>{@code record component sourceMap erased to Object (source type MIndexASTSourceMap)}</li>
 *   <li>{@code record component template erased to Object (source type MIndexLogicTemplate)}</li>
 * </ul>
 */
public record M3CompiledTemplate(Path source, Object sourceAst, Object sourceMap, Object template) {
}
