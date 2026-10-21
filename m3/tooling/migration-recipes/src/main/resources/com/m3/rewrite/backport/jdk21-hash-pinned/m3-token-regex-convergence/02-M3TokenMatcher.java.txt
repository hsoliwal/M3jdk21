/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Match state of one {@link M3TokenPattern} over one {@link M3TokenIndex}; results are
 * half-open token positions plus source spans, captures included.
 *
 * <p>Capture contract (Square T-Rex Matcher/SequencePattern policy, written explicitly): group 0
 * is the whole match; group k reports its LAST iteration, or -1 when it did not participate;
 * greedy quantifiers try longest first, reluctant shortest first, alternatives in written order;
 * an iteration that consumes no token ends its loop. {@link #FIND_ALL_SPANS} (T-Rex find)
 * enumerates every distinct [s, e) from start s in priority order before s + 1, so overlapping
 * spans are returned; {@link #FIND_NEXT} (java.util.regex style) continues after the previous
 * match end, one token further after an empty match. Positions are lossless; with
 * {@code significantOnly} trivia tokens are skipped while matching but never renumbered.
 *
 * <p>Execution is token-coordinate dynamic programming over primitive state lanes. Paths that
 * reach the same token endpoint are folded immediately in priority order; capture registers of the
 * first path win. Greedy/lazy repetition order and written alternative order are preserved.
 * Lexical boundaries belong only to {@link M3TokenIndex}; underlying M3String tuple/atom
 * boundaries are invisible to this matcher.
 */
public final class M3TokenMatcher {
    /** T-Rex find(): every distinct span per start, overlapping spans included. */
    public static final int FIND_ALL_SPANS = 0;
    /** java.util.regex find(): continue after the previous match. */
    public static final int FIND_NEXT = 1;

    private final M3TokenPattern pattern;
    private final M3TokenIndex index;
    private final M3TokenPredicateCache predicates;
    private final int mode;
    private final boolean significantOnly;
    private final int[] registers;
    private final M3TokenEngine engine;
    private boolean matched;

    private M3TokenMatcher(M3TokenPredicateCache predicates, int mode, boolean significantOnly) {
        this.predicates = predicates;
        this.pattern = predicates.pattern();
        this.index = predicates.index();
        this.mode = mode;
        this.significantOnly = significantOnly;
        this.registers = new int[2 * (pattern.groupCount() + 1)];
        this.engine = new M3TokenEngine(predicates, significantOnly);
        Arrays.fill(registers, -1);
    }

    /** @param mode {@link #FIND_ALL_SPANS} or {@link #FIND_NEXT} */
    public static M3TokenMatcher of(M3TokenPredicateCache predicates, int mode, boolean significantOnly) {
        Objects.requireNonNull(predicates, "predicates");
        if (mode != FIND_ALL_SPANS && mode != FIND_NEXT) {
            throw new IllegalArgumentException("unknown find mode " + mode);
        }
        return new M3TokenMatcher(predicates, mode, significantOnly);
    }

    public int mode() { return mode; }
    public boolean significantOnly() { return significantOnly; }
    public int groupCount() { return pattern.groupCount(); }

    /** Whole (projected) tape match; groups from the highest-priority accepting path. */
    public boolean matches() {
        return publish(engine.matches());
    }

    /** Next match under the find mode; see the class contract. */
    public boolean find() {
        return publish(engine.find(mode));
    }

    /** Forget the previous match and search from the first token again. */
    public void reset() {
        engine.reset();
        matched = false;
        Arrays.fill(registers, -1);
    }

    /** Lossless token position where the next find starts (diagnostics). */
    public int searchFrom() {
        return engine.searchFrom();
    }

    public int start() { return start(0); }
    public int end() { return end(0); }

    /** Start token of group g, or -1 when the group did not participate. */
    public int start(int group) {
        requireMatch();
        return registers[2 * Objects.checkIndex(group, pattern.groupCount() + 1)];
    }

    /** End token (exclusive) of group g, or -1 when the group did not participate. */
    public int end(int group) {
        requireMatch();
        return registers[2 * Objects.checkIndex(group, pattern.groupCount() + 1) + 1];
    }

    /** Start token of the first group with this name (T-Rex: no match yet is IllegalStateException). */
    public int start(String name) {
        requireMatch();
        return start(named(name));
    }

    /** End token of the first group with this name. */
    public int end(String name) {
        requireMatch();
        return end(named(name));
    }

    /** Source start (UTF-16 units) of group g, or -1 when it did not participate. */
    public long sourceStart(int group) {
        int s = start(group);
        return s < 0 ? -1 : index.spanStart(s, end(group));
    }

    /** Source end (UTF-16 units, exclusive) of group g, or -1 when it did not participate. */
    public long sourceEnd(int group) {
        int s = start(group);
        return s < 0 ? -1 : index.spanEnd(s, end(group));
    }

    private boolean publish(M3TokenEngine.Result result) {
        Arrays.fill(registers, -1);
        if (result == null) {
            matched = false;
            return false;
        }
        int[] values = result.registers();
        if (values.length != registers.length) {
            throw new IllegalStateException("token regex capture register geometry");
        }
        System.arraycopy(values, 0, registers, 0, values.length);
        matched = true;
        return true;
    }

    private int named(String name) {
        int g = pattern.groupIndex(name);
        if (g < 0) {
            throw new IllegalArgumentException("no group named " + name);
        }
        return g;
    }

    private void requireMatch() {
        if (!matched) {
            throw new IllegalStateException("no match available; call matches() or find() first");
        }
    }

    /** Bound predicate table (shared, immutable). */
    public M3TokenPredicateCache predicates() {
        return predicates;
    }
}
