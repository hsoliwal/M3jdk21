// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.compiler.MIndexCompiledExpression}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexCompiledExpression.java}
 * blob {@code bd60fef79c91a9ad9945f24a9257b79dc3270935} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 05f4f30f502bd77acb8acfb0e210dfce9124521cc0f9fb7378eafd4493a347df}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.compiler.M3CompiledExpression}); projection
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
 *   <li>{@code record component parseResult erased to Object (source type MIndexGrammarParseResult)}</li>
 *   <li>{@code member requireTree() (return type)}</li>
 * </ul>
 */
public record M3CompiledExpression(Object language, Object source, Object parseResult, long grammarFingerprint) {
    public boolean accepted() {
        throw new M3NotYetReceived("M3CompiledExpression#accepted/0");
    }

    public boolean conforms() {
        throw new M3NotYetReceived("M3CompiledExpression#conforms/0");
    }
}
