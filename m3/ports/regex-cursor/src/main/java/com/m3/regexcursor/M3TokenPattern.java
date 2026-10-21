// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Map;
import java.util.Objects;

/**
 * Token-aware regex: a pattern over interned token ids, matched through a chunk cursor
 * ({@link M3TokenCursor}) by the same gates, lazy DFA and PikeVM as text.
 *
 * <p>Because the alphabet is the token id, a match is always a whole token span: it cannot start
 * or end inside a token, cross a token boundary, or reach into a literal or comment token unless
 * the pattern names that token. Group spans are token coordinates. Notation and examples:
 * {@link M3TokenPatternParser}; e.g. {@code <#IDENT> <20> (<#IDENT>|<#NUMBER>) <!#COMMENT>*?
 * <21>} with class table {@code IDENT -> ids, NUMBER -> ids, COMMENT -> ids}.</p>
 *
 * <p>Immutable and shareable; matchers are thread-confined. There is no stock engine for token
 * ids: when a lazy DFA cache fills, the PikeVM answers.</p>
 */
public final class M3TokenPattern {
    private final M3CursorRegex program;

    private M3TokenPattern(M3CursorRegex program) {
        this.program = program;
    }

    /** Compiles {@code notation} without named classes. */
    public static M3TokenPattern compile(String notation) {
        return compile(notation, Map.of());
    }

    /**
     * Compiles {@code notation}; {@code <#NAME>} resolves through {@code classes}.
     *
     * @throws IllegalArgumentException on malformed notation or unknown classes
     */
    public static M3TokenPattern compile(String notation, Map<String, int[]> classes) {
        return compile(notation, classes, M3CursorDfa.DEFAULT_MAX_STATES);
    }

    /** As {@link #compile(String, Map)} with an explicit lazy DFA state budget. */
    public static M3TokenPattern compile(String notation, Map<String, int[]> classes, int maxDfaStates) {
        Objects.requireNonNull(notation, "notation");
        M3RegexAst ast = M3TokenPatternParser.parse(notation, Objects.requireNonNull(classes, "classes"));
        return new M3TokenPattern(new M3CursorRegex(notation, null, ast, null, maxDfaStates));
    }

    /** New matcher over a token cursor. */
    public M3CursorMatcher matcher(M3ChunkCursor tokens) {
        return program.matcher(tokens);
    }

    /** Source notation. */
    public String notation() {
        return program.source;
    }

    /** Number of capturing groups. */
    public int groupCount() {
        return program.groupCount;
    }

    /** Engine strategy ({@code M3CursorRegex.STRATEGY_*}). */
    public int strategy() {
        return program.strategy;
    }

    @Override
    public String toString() {
        return program.source;
    }
}
