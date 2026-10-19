// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.compiler.MIndexCompiledUnit}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexCompiledUnit.java}
 * blob {@code 76db3187cdc557a475b1fa4caa8ecbe78b587b9e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b7c0ac0f9fa7469f910a27c0e6c1e2f3b9ba5f7f2957a18ea3395db42d0ea929}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.compiler.M3CompiledUnit}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/compiler/MIndexCompiledUnit.java(at)97911d73630d}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component language erased to Object (source type MIndexLang)}</li>
 *   <li>{@code record component audit erased to Object (source type MIndexAnalysis)}</li>
 *   <li>{@code record component indexedUnit erased to Object (source type MIndexIndexedCompilationUnit)}</li>
 * </ul>
 */
public record M3CompiledUnit(Object language, Object audit, Object indexedUnit, String sourceSha256) {
    public Path source() {
        throw new M3NotYetReceived("M3CompiledUnit#source/0");
    }

    public long languageFingerprint() {
        throw new M3NotYetReceived("M3CompiledUnit#languageFingerprint/0");
    }
}
