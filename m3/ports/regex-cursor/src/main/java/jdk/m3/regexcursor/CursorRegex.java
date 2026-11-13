// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Compiled regex for chunk cursors: the regex-cursor meta strategy with M3 gates in front and the
 * stock engine last (doc/m3/regex-cursor/DESIGN.md section 3).
 *
 * <p>Strategy chosen at compile time: {@link #STRATEGY_LITERAL} (pure literal, literal search is
 * the engine), {@link #STRATEGY_DFA} (assertion-free subset: lazy DFA, PikeVM for groups),
 * {@link #STRATEGY_PIKEVM} (subset with {@code ^ $ \A \z \Z}), {@link #STRATEGY_STOCK} (refused
 * syntax: stock {@code java.util.regex} over {@link M3CursorCharSequence}). Immutable and
 * shareable; per-search caches live in {@link M3CursorMatcher}.</p>
 */
public final class CursorRegex {
    public static final int STRATEGY_LITERAL = 1;
    public static final int STRATEGY_DFA = 2;
    public static final int STRATEGY_PIKEVM = 3;
    public static final int STRATEGY_STOCK = 4;

    final String source;
    final Pattern stock;
    final int strategy;
    final String refusal;
    final int groupCount;
    final RegexAst ast;
    final boolean quitSurrogates;
    final CursorLiteralSearch literal;
    final CursorLiteralSearch prefix;
    final CursorLiteralSearch required;
    final long requiredMask;
    final CursorNfa anchored;
    final CursorNfa unanchored;
    final CursorNfa reverse;
    final UnitClasses classes;
    final int maxDfaStates;

    CursorRegex(String source, Pattern stock, RegexAst ast, String refusal, int maxDfaStates) {
        this.source = source;
        this.stock = stock;
        this.ast = ast;
        this.refusal = refusal;
        this.maxDfaStates = maxDfaStates;
        this.quitSurrogates = ast != null && ast.alphabetMax == 0xFFFF;
        RegexLiterals facts = ast == null ? null : RegexLiterals.of(ast);
        this.strategy = ast == null ? STRATEGY_STOCK
                : facts.pure != null ? STRATEGY_LITERAL
                : ast.assertions ? STRATEGY_PIKEVM : STRATEGY_DFA;
        this.groupCount = ast == null ? stock.matcher("").groupCount() : ast.groupCount;
        this.literal = facts != null && facts.pure != null ? new CursorLiteralSearch(facts.pure) : null;
        this.prefix = strategy == STRATEGY_DFA && facts.prefix != null ? new CursorLiteralSearch(facts.prefix) : null;
        int[] needed = facts == null ? null : facts.pure != null ? facts.pure : facts.required;
        this.required = needed != null && needed.length > 0 ? new CursorLiteralSearch(needed) : null;
        this.requiredMask = RegexLiterals.mask(needed);
        boolean automata = strategy == STRATEGY_DFA || strategy == STRATEGY_PIKEVM;
        this.anchored = automata ? CursorNfa.forward(ast, false) : null;
        this.unanchored = strategy == STRATEGY_DFA ? CursorNfa.forward(ast, true) : null;
        this.reverse = strategy == STRATEGY_DFA ? CursorNfa.reverse(ast) : null;
        this.classes = strategy == STRATEGY_DFA
                ? UnitClasses.of(ast, quitSurrogates ? Character.MIN_SURROGATE : -1, Character.MAX_SURROGATE)
                : null;
    }

    /** Compiles {@code regex} with no flags. */
    public static CursorRegex compile(String regex) {
        return compile(regex, 0);
    }

    /**
     * Compiles {@code regex}; throws exactly what {@link Pattern#compile(String, int)} throws.
     * Syntax outside the cursor subset compiles to {@link #STRATEGY_STOCK}.
     */
    public static CursorRegex compile(String regex, int flags) {
        return compile(regex, flags, CursorDfa.DEFAULT_MAX_STATES);
    }

    /** As {@link #compile(String, int)} with an explicit lazy DFA state budget. */
    public static CursorRegex compile(String regex, int flags, int maxDfaStates) {
        Pattern stock = Pattern.compile(regex, flags);
        try {
            return new CursorRegex(regex, stock, RegexSubsetParser.parse(regex, flags), null, maxDfaStates);
        } catch (RegexRefusal refusal) {
            return new CursorRegex(regex, stock, null, refusal.getMessage(), maxDfaStates);
        }
    }

    /** New matcher reading {@code cursor}. */
    public CursorMatcher matcher(ChunkCursor cursor) {
        return new CursorMatcher(this, Objects.requireNonNull(cursor, "cursor"));
    }

    /** Source pattern. */
    public String pattern() {
        return source;
    }

    /** Engine strategy chosen at compile time. */
    public int strategy() {
        return strategy;
    }

    /** Why the cursor engines refused the pattern, or {@code null}. */
    public String refusal() {
        return refusal;
    }

    /** Number of capturing groups. */
    public int groupCount() {
        return groupCount;
    }

    @Override
    public String toString() {
        return source;
    }
}
