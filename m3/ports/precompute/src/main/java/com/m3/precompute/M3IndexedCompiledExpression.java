// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.compiler.MIndexIndexedCompiledExpression}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexIndexedCompiledExpression.java}
 * blob {@code b29afe0668a625ce09b2c52a0e5ca5ca9aede714} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c9754a650434ee6035584dba4a9ac9aa5aba60a93d6c5c3dc9ac9d3d46dc459c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.compiler.M3IndexedCompiledExpression}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component language erased to Object (source type MIndexLang)}</li>
 *   <li>{@code record component source erased to Object (source type MIndexString)}</li>
 *   <li>{@code record component tokens erased to Object (source type MIndexTokenStream)}</li>
 *   <li>{@code record component parseResult erased to Object (source type MIndexGrammarParseResult)}</li>
 *   <li>{@code member requireTree() (return type)}</li>
 * </ul>
 */
public record M3IndexedCompiledExpression(Object language, Object source, Object tokens, Object parseResult, long grammarFingerprint, int languageId, boolean coordinateBound, long coordinateFingerprint) {
    public boolean accepted() {
        throw new M3NotYetReceived("M3IndexedCompiledExpression#accepted/0");
    }

    public boolean conforms() {
        throw new M3NotYetReceived("M3IndexedCompiledExpression#conforms/0");
    }

    public boolean hasCoordinateFingerprint() {
        throw new M3NotYetReceived("M3IndexedCompiledExpression#hasCoordinateFingerprint/0");
    }

    public int tokenCount() {
        throw new M3NotYetReceived("M3IndexedCompiledExpression#tokenCount/0");
    }

    public long[] tokenIds() {
        throw new M3NotYetReceived("M3IndexedCompiledExpression#tokenIds/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3IndexedCompiledExpression#primitivePayloadBytes/0");
    }
}
