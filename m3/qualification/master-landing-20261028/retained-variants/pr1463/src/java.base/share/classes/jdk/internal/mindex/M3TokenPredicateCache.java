/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable predicate truth masks bound to one {@link M3TokenIndex} generation.
 *
 * <p>Binding resolves exact text to canonical lexeme IDs, evaluates occurrence facts against
 * the index, and precomputes every row and Boolean expression into packed words. A warm
 * predicate test performs only bounds checks and a bit lookup; it reads no spelling, runs no
 * regex, and allocates no object. Repeated known lexemes reuse spelling-regex results in a
 * bounded primitive memo during binding. Binding, attribute and flag predicates are always
 * evaluated from the occurrence, never inferred from its spelling.
 *
 * <p>Regex spelling semantics come from the existing JDK {@link Pattern} owner. Regexes run
 * against a bounded view of the original spelling with whole-value matches semantics, so
 * lookaround cannot inspect adjacent tokens. Pattern execution occurs only during binding;
 * its cost follows the selected JDK regex, not the bounded token-sequence machine. The caller
 * must provide an immutable source/index/vocabulary snapshot during binding. No source,
 * vocabulary, Matcher, copied spelling or second canonical string store is retained.
 *
 * <p>The retained primitive payload is limited to 64 MiB. Accepted-term sets follow the
 * BlackLab NfaStateToken shape (Apache-2.0, algorithmic reference only), using M3 mask words.
 */
public final class M3TokenPredicateCache {
    private static final long MAX_BYTES = 64L << 20;

    /** Caller vocabulary; must be the one that assigned the index lexeme and symbolic lanes. */
    public interface Vocabulary {
        /** Canonical lexeme id of the spelling, 0 when it is not a lexicon word. */
        int lexemeOf(CharSequence spelling);

        /** Int value of a symbolic kind or attribute value, or -1 when unknown. */
        default int symbolOf(String key, String value) { return -1; }
    }

    /** Optional inverse-vocabulary capability for regex predicates on symbolic index lanes. */
    public interface SymbolVocabulary extends Vocabulary {
        /**
         * Reverse spelling of a symbolic lane value, or null when unavailable. Required only
         * for regex predicates on kind, lexeme, binding, flags or named attribute columns.
         * The returned immutable view is read during binding and is not retained. This is the
         * inverse of the caller's existing vocabulary, not an additional symbol table.
         */
        CharSequence symbolSpelling(String key, long value);
    }

    private final M3TokenPattern pattern;
    private final M3TokenIndex index;
    private final long[] values;
    private final long[] truth;
    private final int[] expressionRows;
    private final int words;

    private M3TokenPredicateCache(M3TokenPattern pattern, M3TokenIndex index,
            long[] values, long[] truth, int[] expressionRows, int words) {
        this.pattern = pattern;
        this.index = index;
        this.values = values;
        this.truth = truth;
        this.expressionRows = expressionRows;
        this.words = words;
    }

    /**
     * Precompute every predicate against the exact source and canonical vocabulary snapshot.
     * The source may be an M3String and is read through its existing CharSequence interface.
     *
     * @throws IllegalArgumentException for unknown symbols/columns, missing reverse symbolic
     *         spellings required by a regex, out-of-source spans, or an over-budget table
     */
    public static M3TokenPredicateCache bind(M3TokenPattern pattern, M3TokenIndex index,
            CharSequence source, Vocabulary vocabulary) {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(vocabulary, "vocabulary");
        int words = (int) (((long) index.count() + 63) >>> 6);
        int rows = pattern.rowCount();
        int[] expressionRows = new int[pattern.exprCount()];
        int maskRows = rows;
        for (int e = 0; e < expressionRows.length; e++) {
            expressionRows[e] = pattern.exprOp(e) == M3TokenPattern.P_ROW
                    ? pattern.exprA(e) : maskRows++;
        }
        long cells = (long) maskRows * words;
        if (cells * Long.BYTES + (long) rows * Long.BYTES
                + (long) expressionRows.length * Integer.BYTES > MAX_BYTES) {
            throw new IllegalArgumentException("token predicate primitive payload exceeds 64 MiB");
        }
        long[] truth = new long[(int) cells];
        long[] values = new long[rows];
        for (int t = 0; t < index.count(); t++) {
            if (index.endAt(t) > source.length()) {
                throw new IllegalArgumentException("token span exceeds source at " + t);
            }
        }
        Binder binder = new Binder(pattern, index, source, vocabulary);
        for (int r = 0; r < rows; r++) {
            int column = columnOf(pattern, index, r);
            values[r] = valueOf(pattern, vocabulary, r);
            Matcher regex = pattern.rowOp(r) == M3TokenPattern.O_REGEX
                    ? Pattern.compile(pattern.rowLiteral(r)).matcher("") : null;
            // Direct-mapped memo eviction affects bind cost only; equality is verified by ID.
            long[] memo = regex != null && pattern.rowKey(r) == M3TokenPattern.K_TEXT
                    ? new long[4096] : null;
            for (int t = 0; t < index.count(); t++) {
                if (binder.row(r, t, column, values[r], regex, memo)) {
                    truth[r * words + (t >>> 6)] |= 1L << (t & 63);
                }
            }
        }
        for (int e = 0; e < expressionRows.length; e++) {
            byte op = pattern.exprOp(e);
            if (op == M3TokenPattern.P_ROW) continue;
            int a = pattern.exprA(e), b = pattern.exprB(e);
            if (a >= e || (op != M3TokenPattern.P_NOT && b >= e)) {
                throw new IllegalArgumentException("predicate arena is not in dependency order");
            }
            int out = expressionRows[e] * words, left = expressionRows[a] * words;
            int right = op == M3TokenPattern.P_NOT ? 0 : expressionRows[b] * words;
            for (int w = 0; w < words; w++) {
                truth[out + w] = switch (op) {
                    case M3TokenPattern.P_NOT -> ~truth[left + w];
                    case M3TokenPattern.P_AND -> truth[left + w] & truth[right + w];
                    default -> truth[left + w] | truth[right + w];
                };
            }
        }
        return new M3TokenPredicateCache(pattern, index, values, truth, expressionRows, words);
    }

    private static int columnOf(M3TokenPattern pattern, M3TokenIndex index, int r) {
        if (pattern.rowKey(r) != M3TokenPattern.K_ATTR) return -1;
        int column = index.attributeColumn(pattern.rowAttribute(r));
        if (column < 0 && pattern.rowOp(r) != M3TokenPattern.O_ABSENT) {
            throw new IllegalArgumentException("unknown attribute column " + pattern.rowAttribute(r));
        }
        return column;
    }

    private static long valueOf(M3TokenPattern pattern, Vocabulary vocabulary, int r) {
        byte op = pattern.rowOp(r);
        if (op == M3TokenPattern.O_REGEX || op == M3TokenPattern.O_ABSENT) return 0;
        if (op != M3TokenPattern.O_EQ) return pattern.rowNumber(r);
        String literal = pattern.rowLiteral(r);
        byte key = pattern.rowKey(r);
        if (key == M3TokenPattern.K_TEXT) return vocabulary.lexemeOf(literal);
        return key == M3TokenPattern.K_FLAGS ? parseMask(literal)
                : symbol(vocabulary, pattern, r, literal);
    }

    private static long symbol(Vocabulary vocabulary, M3TokenPattern pattern, int r, String literal) {
        long number = parseIntOrMinus(literal);
        if (number >= 0) return number;
        String key = keyName(pattern, r);
        int value = vocabulary.symbolOf(key, literal);
        if (value < 0) throw new IllegalArgumentException("unknown " + key + " value " + literal);
        return value;
    }

    private static String keyName(M3TokenPattern pattern, int r) {
        return switch (pattern.rowKey(r)) {
            case M3TokenPattern.K_KIND -> "kind";
            case M3TokenPattern.K_LEXEME -> "lexeme";
            case M3TokenPattern.K_BINDING -> "binding";
            case M3TokenPattern.K_FLAGS -> "flags";
            default -> pattern.rowAttribute(r);
        };
    }

    private static long parseIntOrMinus(String literal) {
        for (int i = 0; i < literal.length(); i++) {
            if (!Character.isDigit(literal.charAt(i)) || i > 9) return -1;
        }
        return Long.parseLong(literal);
    }

    private static long parseMask(String literal) {
        try {
            return literal.startsWith("0x") || literal.startsWith("0X")
                    ? Long.parseUnsignedLong(literal.substring(2), 16) : Long.parseLong(literal);
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException("flags value is not a mask: " + literal, bad);
        }
    }

    /** Identity components for this bound object; callers must also preserve index identity. */
    public String cacheKey() {
        return pattern.patternRoot() + "/" + pattern.flags() + "/" + Long.toHexString(index.generation());
    }

    public M3TokenPattern pattern() { return pattern; }
    public M3TokenIndex index() { return index; }

    /** Compiled int value of a row (lexeme id for exact text; 0 for regex and absent rows). */
    public long rowValue(int r) { return values[Objects.checkIndex(r, values.length)]; }

    /** One precomputed expression-bit lookup at a lossless token position. */
    public boolean test(int e, int token) {
        int row = expressionRows[Objects.checkIndex(e, expressionRows.length)];
        return bit(row, token);
    }

    /** One precomputed predicate-row bit lookup at a lossless token position. */
    public boolean row(int r, int token) {
        return bit(Objects.checkIndex(r, values.length), token);
    }

    private boolean bit(int row, int token) {
        Objects.checkIndex(token, index.count());
        return (truth[row * words + (token >>> 6)] & (1L << (token & 63))) != 0;
    }

    /** Cold binding context. None of its source or regex state is retained by the cache. */
    private static final class Binder {
        private final M3TokenPattern pattern;
        private final M3TokenIndex index;
        private final CharSequence source;
        private final Vocabulary vocabulary;
        private final boolean caseInsensitive;
        private final Span span;

        Binder(M3TokenPattern pattern, M3TokenIndex index, CharSequence source, Vocabulary vocabulary) {
            this.pattern = pattern;
            this.index = index;
            this.source = source;
            this.vocabulary = vocabulary;
            this.caseInsensitive = (pattern.flags() & M3TokenPattern.CASE_INSENSITIVE) != 0;
            this.span = new Span(source);
        }

        boolean row(int r, int token, int column, long wanted, Matcher regex, long[] memo) {
            byte op = pattern.rowOp(r), key = pattern.rowKey(r);
            if (regex != null) return regex(r, token, column, regex, memo);
            if (key == M3TokenPattern.K_TEXT) return text(r, op, token, wanted);
            if (key == M3TokenPattern.K_FLAGS) {
                long flags = index.flagsAt(token);
                return op == M3TokenPattern.O_EQ ? (flags & wanted) == wanted : compare(op, flags, wanted);
            }
            if (key == M3TokenPattern.K_ATTR && op == M3TokenPattern.O_ABSENT) {
                return column < 0 || index.attributeAt(column, token) == 0;
            }
            return compare(op, lane(key, column, token), wanted);
        }

        private long lane(byte key, int column, int token) {
            return switch (key) {
                case M3TokenPattern.K_KIND -> index.kindAt(token);
                case M3TokenPattern.K_LEXEME -> index.lexemeAt(token);
                case M3TokenPattern.K_BINDING -> index.bindingAt(token);
                case M3TokenPattern.K_FLAGS -> index.flagsAt(token);
                default -> index.attributeAt(column, token);
            };
        }

        private boolean regex(int r, int token, int column, Matcher regex, long[] memo) {
            if (pattern.rowKey(r) != M3TokenPattern.K_TEXT) {
                long value = lane(pattern.rowKey(r), column, token);
                if (pattern.rowKey(r) == M3TokenPattern.K_ATTR && value == 0) return false;
                String key = keyName(pattern, r);
                CharSequence spelling = vocabulary instanceof SymbolVocabulary symbols
                        ? symbols.symbolSpelling(key, value) : null;
                if (spelling == null) {
                    throw new IllegalArgumentException("missing reverse spelling for " + key + "=" + value);
                }
                return regex.reset(spelling).matches();
            }
            int lexeme = index.lexemeAt(token);
            int slot = (lexeme * 0x9e3779b9) & (memo.length - 1);
            if (lexeme > 0 && (memo[slot] >>> 32) == lexeme) return (memo[slot] & 1L) != 0;
            span.set((int) index.startAt(token), (int) index.endAt(token));
            boolean accepted = regex.reset(span).matches();
            if (lexeme > 0) memo[slot] = ((long) lexeme << 32) | (accepted ? 1L : 0L);
            return accepted;
        }

        private boolean text(int r, byte op, int token, long wanted) {
            if (op == M3TokenPattern.O_ABSENT) return false;
            if (op != M3TokenPattern.O_EQ) return numeric(op, token, wanted);
            int lexeme = index.lexemeAt(token);
            if (!caseInsensitive && lexeme > 0) return wanted > 0 && lexeme == wanted;
            String literal = pattern.rowLiteral(r);
            int start = (int) index.startAt(token), end = (int) index.endAt(token);
            if (end - start != literal.length()) return false;
            for (int i = 0; i < literal.length(); i++) {
                char a = source.charAt(start + i), b = literal.charAt(i);
                if (a != b && !(caseInsensitive && fold(a) == fold(b))) return false;
            }
            return true;
        }

        private boolean numeric(byte op, int token, long wanted) {
            int at = (int) index.startAt(token), end = (int) index.endAt(token);
            if (at == end) return false;
            char first = source.charAt(at);
            boolean negative = first == '-';
            if (negative || first == '+') at++;
            if (at == end) return false;
            long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE, value = 0;
            long multiplyLimit = limit / 10;
            while (at < end) {
                int digit = Character.digit(source.charAt(at++), 10);
                if (digit < 0 || value < multiplyLimit) return false;
                value *= 10;
                if (value < limit + digit) return false;
                value -= digit;
            }
            return compare(op, negative ? value : -value, wanted);
        }
    }

    /** Bounded view keeps token boundary and surrogate behavior identical to an isolated value. */
    private static final class Span implements CharSequence {
        private final CharSequence source;
        private int start, end;

        Span(CharSequence source) { this.source = source; }
        void set(int start, int end) { this.start = start; this.end = end; }
        @Override public int length() { return end - start; }
        @Override public char charAt(int offset) {
            return source.charAt(start + Objects.checkIndex(offset, length()));
        }
        @Override public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length());
            Span result = new Span(source);
            result.set(start + from, start + to);
            return result;
        }
        @Override public String toString() {
            StringBuilder result = new StringBuilder(length());
            for (int i = 0; i < length(); i++) result.append(charAt(i));
            return result.toString();
        }
    }

    private static char fold(char c) { return Character.toLowerCase(Character.toUpperCase(c)); }

    private static boolean compare(byte op, long actual, long wanted) {
        return switch (op) {
            case M3TokenPattern.O_ABSENT -> actual == 0;
            case M3TokenPattern.O_GT -> actual > wanted;
            case M3TokenPattern.O_GE -> actual >= wanted;
            case M3TokenPattern.O_LT -> actual < wanted;
            case M3TokenPattern.O_LE -> actual <= wanted;
            case M3TokenPattern.O_NE -> actual != wanted;
            default -> actual == wanted;
        };
    }
}
