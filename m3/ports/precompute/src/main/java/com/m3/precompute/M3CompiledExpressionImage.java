// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.compiler.MIndexCompiledExpressionImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexCompiledExpressionImage.java}
 * blob {@code ea1f0e94862e2c9b7c9b60ec90ef22895d79dea7} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 62435c13a55bb6412e64cea3dfcaba1307d9d54191dcf8b88885dc79ae3a0e82}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.compiler.M3CompiledExpressionImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
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
 *   <li>{@code member language() (return type)}</li>
 *   <li>{@code member resolver() (return type)}</li>
 * </ul>
 */
public final class M3CompiledExpressionImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code language} of type {@code MIndexLang}. */
    private int[] languageLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code resolver} of type {@code IndexResolver}. */
    private int[] resolverLane;

    /** primitive lane for source field {@code languageId} of type {@code int}. */
    private int languageId;

    /** primitive lane for source field {@code grammarFingerprint} of type {@code long}. */
    private long grammarFingerprint;

    /** primitive lane for source field {@code sourceUtf16Length} of type {@code int}. */
    private int sourceUtf16Length;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sourceUtf16Sha256} of type {@code String}. */
    private int[] sourceUtf16Sha256Lane;

    /** primitive lane for source field {@code root} of type {@code int}. */
    private int root;

    /** primitive lane for source field {@code tokenIds} of type {@code long[]}. */
    private long[] tokenIds;

    /** primitive lane for source field {@code tokenStarts} of type {@code int[]}. */
    private int[] tokenStarts;

    /** primitive lane for source field {@code tokenEnds} of type {@code int[]}. */
    private int[] tokenEnds;

    /** primitive lane for source field {@code tokenClasses} of type {@code byte[]}. */
    private byte[] tokenClasses;

    /** primitive lane for source field {@code symbols} of type {@code int[]}. */
    private int[] symbols;

    /** primitive lane for source field {@code tokenIndices} of type {@code int[]}. */
    private int[] tokenIndices;

    /** primitive lane for source field {@code fromTokens} of type {@code int[]}. */
    private int[] fromTokens;

    /** primitive lane for source field {@code toTokens} of type {@code int[]}. */
    private int[] toTokens;

    /** primitive lane for source field {@code childStarts} of type {@code int[]}. */
    private int[] childStarts;

    /** primitive lane for source field {@code childCounts} of type {@code int[]}. */
    private int[] childCounts;

    /** primitive lane for source field {@code childArena} of type {@code int[]}. */
    private int[] childArena;

    /** primitive lane for source field {@code structuralHashes} of type {@code long[]}. */
    private long[] structuralHashes;

    private M3CompiledExpressionImage() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#<init>/0");
    }

    public int languageId() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#languageId/0");
    }

    public long grammarFingerprint() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#grammarFingerprint/0");
    }

    public int sourceUtf16Length() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#sourceUtf16Length/0");
    }

    public String sourceUtf16Sha256() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#sourceUtf16Sha256/0");
    }

    public int root() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#root/0");
    }

    public int tokenCount() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenCount/0");
    }

    public int nodeCount() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#nodeCount/0");
    }

    public int edgeCount() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#edgeCount/0");
    }

    public long tokenId(int token) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenId/1");
    }

    public int tokenStart(int token) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenStart/1");
    }

    public int tokenEnd(int token) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenEnd/1");
    }

    public int tokenClassOrdinal(int token) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenClassOrdinal/1");
    }

    public String tokenText(int token) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenText/1");
    }

    public int symbol(int node) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#symbol/1");
    }

    public int tokenIndex(int node) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenIndex/1");
    }

    public int fromToken(int node) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#fromToken/1");
    }

    public int toToken(int node) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#toToken/1");
    }

    public int childCount(int node) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#childCount/1");
    }

    public int child(int node, int ordinal) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#child/2");
    }

    public long structuralHash(int node) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#structuralHash/1");
    }

    public long rootStructuralHash() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#rootStructuralHash/0");
    }

    public long[] tokenIds() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#tokenIds/0");
    }

    public boolean exactlyEquals(M3CompiledExpressionImage other) {
        throw new M3NotYetReceived("M3CompiledExpressionImage#exactlyEquals/1");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3CompiledExpressionImage#primitivePayloadBytes/0");
    }
}
