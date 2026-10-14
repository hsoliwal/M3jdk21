/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import jdk.internal.mindex.M3DotStarRelations;

/**
 * Bounded full-region dynamic programming for the conservative dot/star regex subset.
 *
 * <p>Only {@link String#matches(String)} with an existing canonical M3 String owner
 * may use this internal decision. It does not implement the stateful Pattern/Matcher
 * API, regex flags, Unicode code-point syntax, captures, or search operations.
 * An unknown decision must delegate to the ordinary JDK regex engine.</p>
 *
 * <p>Each compiled atom is one lowercase/uppercase ASCII letter, digit, or dot;
 * a following star marks zero-or-more repetition. The temporary token/DP arrays
 * never contain or own text payload, and each text unit is read directly from
 * the canonical M3 owner rather than a flattened String shadow.</p>
 */
final class M3StringRegexDp {
    static final int UNKNOWN = -1;
    static final int NO_MATCH = 0;
    static final int MATCH = 1;

    private static final int STAR = 1 << 16;
    private static final int CODE_UNIT = 0xffff;
    private static final int MAX_PATTERN_UNITS = 128;
    private static final int MAX_TEXT_UNITS = 8_192;
    private static final long MAX_DP_CELLS = 1_000_000L;

    private M3StringRegexDp() { }

    /**
     * A three-state, contract-preserving decision: UNKNOWN means use Pattern.matches.
     * No stored per-String state, JNI ABI or cross-call cache is introduced.
     */
    static int tryMatch(M3String source, String regex) {
        int n = regex.length();
        if (n < 2 || n > MAX_PATTERN_UNITS) return UNKNOWN;

        // Compile once per candidate. The compact int lane is a temporary pattern plan,
        // not an owned spelling payload. Every accepted pattern is syntactically valid.
        int[] atoms = new int[n];
        int count = 0;
        int stars = 0;
        for (int index = 0; index < n; index++) {
            char unit = regex.charAt(index);
            if (unit == '*') {
                if (count == 0 || (atoms[count - 1] & STAR) != 0) return UNKNOWN;
                atoms[count - 1] |= STAR;
                stars++;
            } else if (unit == '.' || isAsciiAlphanumeric(unit)) {
                atoms[count++] = unit;
            } else {
                return UNKNOWN;
            }
        }
        // Literal-only expressions already have an exact M3 equality fast path.
        if (stars == 0) return UNKNOWN;

        int m = source.length();
        if (m > MAX_TEXT_UNITS || ((long) m + 1) * (count + 1) > MAX_DP_CELLS) {
            return UNKNOWN;
        }

        // Qualified subset only: compose exact per-owner/range effects on the existing DAG.
        // Tiny inputs stay on rolling rows. State-work admission is bounded by the old DP cell work.
        // Optional allocation/admission failure preserves the previous rolling-row decision.
        if (m >= 256 && count < Long.SIZE && source.owner() instanceof M3StringTuple) {
            try {
                int decision = M3DotStarRelations.evaluate(source, RELATION_ACCESS, atoms, count,
                        128, Math.min(MAX_DP_CELLS, (long) m * (count + 1)), null, null).decision();
                if (decision != UNKNOWN) return decision;
            } catch (OutOfMemoryError unavailable) {
                // This optional precompute is not semantic authority or a new heap requirement.
            }
        }

        // Nullable prefix: every atom of an empty-text-matching prefix must be starred.
        boolean[] previous = new boolean[count + 1];
        boolean[] current = new boolean[count + 1];
        previous[0] = true;
        for (int j = 1; j <= count; j++) {
            previous[j] = (atoms[j - 1] & STAR) != 0 && previous[j - 1];
        }

        for (int i = 0; i < m; i++) {
            char value = source.charAt(i);
            // java.util.regex matches Unicode code points, not arbitrary halves of
            // surrogate pairs. Preserve that contract through the JDK fallback.
            if (Character.isSurrogate(value)) return UNKNOWN;
            current[0] = false;
            for (int j = 1; j <= count; j++) {
                int atom = atoms[j - 1];
                char required = (char) (atom & CODE_UNIT);
                boolean accepts = required == '.'
                        ? !isJdkLineTerminator(value)
                        : required == value;
                current[j] = (atom & STAR) != 0
                        ? current[j - 1] || (accepts && previous[j])
                        : accepts && previous[j - 1];
            }
            boolean[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[count] ? MATCH : NO_MATCH;
    }

    /** Borrow visible child coordinates. This stateless adapter is not a second text owner. */
    private static final M3DotStarRelations.Access<M3String> RELATION_ACCESS =
            new M3DotStarRelations.Access<>() {
                @Override public Object owner(M3String value) { return value.owner(); }
                @Override public long coordinate(M3String value) { return value.coordinate(); }
                @Override public int length(M3String value) { return value.length(); }
                @Override public int codeUnitAt(M3String value, int index) { return value.charAt(index); }
                @Override public M3String first(M3String value) {
                    if (!(value.owner() instanceof M3StringTuple tuple)) return null;
                    int start = value.start();
                    int end = value.end();
                    int split = tuple.left.length();
                    return start >= split
                            ? tuple.right.slice(start - split, end - split)
                            : tuple.left.slice(start, Math.min(end, split));
                }
                @Override public M3String second(M3String value) {
                    if (!(value.owner() instanceof M3StringTuple tuple)) return null;
                    int split = tuple.left.length();
                    return value.start() < split && value.end() > split
                            ? tuple.right.slice(0, value.end() - split) : null;
                }
            };

    private static boolean isAsciiAlphanumeric(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9');
    }

    /** JDK default dot semantics (no DOTALL, no UNIX_LINES). */
    private static boolean isJdkLineTerminator(char value) {
        return value == '\n' || value == '\r' || value == '\u0085'
                || value == '\u2028' || value == '\u2029';
    }
}
