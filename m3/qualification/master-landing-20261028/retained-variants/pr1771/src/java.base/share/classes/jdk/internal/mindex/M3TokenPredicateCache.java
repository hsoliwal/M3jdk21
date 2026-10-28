/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Predicate table of one {@link M3TokenPattern} bound to one {@link M3TokenIndex} generation.
 *
 * <p>Each predicate row compiles once: exact text to a lexeme id (int compare; a token outside
 * the lexicon compares its source span in place, never a copy), kind, lexeme, binding and
 * attribute values to ints, flags to a mask. Regex rows on the spelling are answered per lexeme
 * id from a bit table B_P[lexemeId] keyed by (pattern root, flags, generation); the fill comes
 * from the M3RX7 TokenIndex rows of M3JDK21 #1338 or the Synexia MIndexTokenRegex rank bitmap and
 * is not received yet. Occurrence predicates (binding, attribute columns, flags) are evaluated at
 * the token and never cached by spelling. Accepted-lexeme sets follow the BlackLab
 * NfaStateToken per-state term set (Apache-2.0, pattern only) with M3 mask words instead of a
 * third-party int set.
 *
 * <p>For an immutable String, completed text-regex answers are reused per exact row and token
 * occurrence inside this owner. The recent answer uses one primitive word; a second distinct
 * query may allocate at most 256 primitive result words. Collisions evict reuse, not correctness.
 * This does not complete the pending per-lexeme B_P table or certify arbitrary lexeme ids.
 */
public final class M3TokenPredicateCache {
    private static final int MAX_TEXT_REGEX_RESULTS = 256;
    private static final VarHandle TEXT_REGEX_ELEMENT = MethodHandles.arrayElementVarHandle(long[].class);

    /** Caller vocabulary; must be the one that assigned the index lexeme lane. */
    public interface Vocabulary {
        /** Canonical lexeme id of the spelling, 0 when it is not a lexicon word. */
        int lexemeOf(CharSequence spelling);

        /** Int value of a symbolic kind or attribute value, or -1 when unknown. */
        default int symbolOf(String key, String value) {
            return -1;
        }

        /**
         * Reverse spelling of a symbolic lane value for token-regex predicates, or {@code null}
         * when this vocabulary has no textual spelling for that coordinate.
         */
        default CharSequence symbolText(String key, int value) {
            return null;
        }
    }

    private final M3TokenPattern pattern;
    private final M3TokenIndex index;
    private final CharSequence source;
    private final boolean caseInsensitive;
    private final long[] values;
    private final int[] columns;
    private final Pattern[] regex;
    private final Vocabulary vocabulary;
    /** Exact row/occurrence key and truth, together in one atomic word; zero is empty. */
    private volatile long textRegexRecent;
    /** Optional bounded result metadata, allocated only after two different immutable occurrences. */
    private volatile long[] textRegexResults;

    private M3TokenPredicateCache(M3TokenPattern pattern, M3TokenIndex index, CharSequence source,
            long[] values, int[] columns, Pattern[] regex, Vocabulary vocabulary) {
        this.pattern = pattern;
        this.index = index;
        this.source = source;
        this.caseInsensitive = (pattern.flags() & M3TokenPattern.CASE_INSENSITIVE) != 0;
        this.values = values;
        this.columns = columns;
        this.regex = regex;
        this.vocabulary = vocabulary;
    }

    /**
     * Compile every predicate row against the vocabulary. {@code source} is the M3String the
     * index was derived from; it is read in place for tokens outside the lexicon.
     *
     * @throws IllegalArgumentException for an unknown symbolic value or attribute column
     */
    public static M3TokenPredicateCache bind(M3TokenPattern pattern, M3TokenIndex index,
            CharSequence source, Vocabulary vocabulary) {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(vocabulary, "vocabulary");
        int rows = pattern.rowCount();
        long[] values = new long[rows];
        int[] columns = new int[rows];
        Pattern[] regex = new Pattern[rows];
        for (int r = 0; r < rows; r++) {
            columns[r] = columnOf(pattern, index, r);
            values[r] = valueOf(pattern, vocabulary, r);
            if (pattern.rowOp(r) == M3TokenPattern.O_REGEX) {
                regex[r] = Pattern.compile(pattern.rowLiteral(r));
            }
        }
        return new M3TokenPredicateCache(pattern, index, source, values, columns, regex, vocabulary);
    }

    private static int columnOf(M3TokenPattern pattern, M3TokenIndex index, int r) {
        if (pattern.rowKey(r) != M3TokenPattern.K_ATTR) {
            return -1;
        }
        int column = index.attributeColumn(pattern.rowAttribute(r));
        if (column < 0 && pattern.rowOp(r) != M3TokenPattern.O_ABSENT) {
            throw new IllegalArgumentException("unknown attribute column " + pattern.rowAttribute(r));
        }
        return column;
    }

    private static long valueOf(M3TokenPattern pattern, Vocabulary vocabulary, int r) {
        byte op = pattern.rowOp(r);
        if (op == M3TokenPattern.O_REGEX || op == M3TokenPattern.O_ABSENT) {
            return 0;
        }
        if (op != M3TokenPattern.O_EQ) {
            return pattern.rowNumber(r);
        }
        String literal = pattern.rowLiteral(r);
        byte key = pattern.rowKey(r);
        if (key == M3TokenPattern.K_TEXT) {
            return vocabulary.lexemeOf(literal);
        }
        return key == M3TokenPattern.K_FLAGS ? parseMask(literal) : symbol(vocabulary, pattern, r, literal);
    }

    private static long symbol(Vocabulary vocabulary, M3TokenPattern pattern, int r, String literal) {
        long number = parseIntOrMinus(literal);
        if (number >= 0) {
            return number;
        }
        String key = pattern.rowKey(r) == M3TokenPattern.K_ATTR ? pattern.rowAttribute(r) : keyName(pattern.rowKey(r));
        int value = vocabulary.symbolOf(key, literal);
        if (value < 0) {
            throw new IllegalArgumentException("unknown " + key + " value " + literal);
        }
        return value;
    }

    private static String keyName(byte key) {
        return switch (key) {
            case M3TokenPattern.K_KIND -> "kind";
            case M3TokenPattern.K_LEXEME -> "lexeme";
            case M3TokenPattern.K_BINDING -> "binding";
            case M3TokenPattern.K_FLAGS -> "flags";
            default -> "text";
        };
    }

    private static long parseIntOrMinus(String literal) {
        for (int i = 0; i < literal.length(); i++) {
            if (!Character.isDigit(literal.charAt(i)) || i > 9) {
                return -1;
            }
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

    /** Cache identity: pattern root, pattern flags and canonical-store generation. */
    public String cacheKey() {
        return pattern.patternRoot() + "/" + pattern.flags() + "/" + Long.toHexString(index.generation());
    }

    public M3TokenPattern pattern() { return pattern; }
    public M3TokenIndex index() { return index; }

    /** Compiled int value of a row (lexeme id for exact text; 0 for regex and absent rows). */
    public long rowValue(int r) {
        return values[Objects.checkIndex(r, values.length)];
    }

    /** Evaluate predicate expression {@code e} of the pattern at lossless token position. */
    public boolean test(int e, int token) {
        Objects.checkIndex(token, index.count());
        return switch (pattern.exprOp(e)) {
            case M3TokenPattern.P_ROW -> row(pattern.exprA(e), token);
            case M3TokenPattern.P_NOT -> !test(pattern.exprA(e), token);
            case M3TokenPattern.P_AND -> test(pattern.exprA(e), token) && test(pattern.exprB(e), token);
            default -> test(pattern.exprA(e), token) || test(pattern.exprB(e), token);
        };
    }

    /** Evaluate one predicate row at a token. */
    public boolean row(int r, int token) {
        byte op = pattern.rowOp(r);
        if (op == M3TokenPattern.O_REGEX) {
            return regexRow(r, token);
        }
        return switch (pattern.rowKey(r)) {
            case M3TokenPattern.K_TEXT -> textRow(r, op, token);
            case M3TokenPattern.K_KIND -> compare(op, index.kindAt(token), values[r]);
            case M3TokenPattern.K_LEXEME -> compare(op, index.lexemeAt(token), values[r]);
            case M3TokenPattern.K_BINDING -> compare(op, index.bindingAt(token), values[r]);
            case M3TokenPattern.K_FLAGS -> flagsRow(op, index.flagsAt(token), r);
            default -> attributeRow(r, op, token);
        };
    }

    private boolean regexRow(int r, int token) {
        CharSequence text;
        byte key = pattern.rowKey(r);
        if (key == M3TokenPattern.K_TEXT) {
            long start = index.startAt(token);
            long end = index.endAt(token);
            if (start < 0 || end < start || end > source.length()) return false;
            if (source instanceof String) {
                return immutableTextRegex(r, token, Math.toIntExact(start), Math.toIntExact(end));
            }
            text = new Span(source, Math.toIntExact(start), Math.toIntExact(end));
        } else {
            long raw = switch (key) {
                case M3TokenPattern.K_KIND -> index.kindAt(token);
                case M3TokenPattern.K_LEXEME -> index.lexemeAt(token);
                case M3TokenPattern.K_BINDING -> index.bindingAt(token);
                case M3TokenPattern.K_FLAGS -> index.flagsAt(token);
                default -> {
                    int column = columns[r];
                    yield column < 0 ? 0L : index.attributeAt(column, token);
                }
            };
            if (raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE) {
                text = Long.toString(raw);
            } else {
                String name = key == M3TokenPattern.K_ATTR
                        ? pattern.rowAttribute(r) : keyName(key);
                CharSequence symbolic = vocabulary.symbolText(name, (int) raw);
                text = symbolic != null ? symbolic : Long.toString(raw);
            }
        }
        return regex[r].matcher(text).matches();
    }

    /** No-copy CharSequence projection of one source token span. */
    private record Span(CharSequence source, int start, int end) implements CharSequence {
        Span {
            Objects.requireNonNull(source, "source");
            Objects.checkFromToIndex(start, end, source.length());
        }

        @Override public int length() { return end - start; }

        @Override
        public char charAt(int index) {
            Objects.checkIndex(index, length());
            return source.charAt(start + index);
        }

        @Override
        public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length());
            return new Span(source, start + from, start + to);
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder(length());
            for (int index = 0; index < length(); index++) out.append(charAt(index));
            return out.toString();
        }
    }

    /**
     * Reuses only this owner's immutable String and defensive token-index snapshot. Lexeme ids
     * and vocabulary symbols are not spelling identity. Each miss still uses a fresh Matcher;
     * mutable sources and occurrence-dependent symbolic rows never enter this result lane.
     */
    private boolean immutableTextRegex(int row, int token, int start, int end) {
        long key = textRegexKey(row, token, index.count());
        long recent = textRegexRecent;
        if ((recent & ~1L) == key) {
            return (recent & 1L) != 0;
        }
        long[] results = textRegexResults;
        if (results != null) {
            int slot = (int) (key >>> 1) & (results.length - 1);
            long result = (long) TEXT_REGEX_ELEMENT.getOpaque(results, slot);
            if ((result & ~1L) == key) {
                return (result & 1L) != 0;
            }
        }
        boolean matched = regex[row].matcher(new Span(source, start, end)).matches();
        long result = key | (matched ? 1L : 0L);
        textRegexRecent = result;
        if (results != null) {
            int slot = (int) (key >>> 1) & (results.length - 1);
            TEXT_REGEX_ELEMENT.setOpaque(results, slot, result);
        } else if (recent != 0) {
            // Concurrent initializers may lose reuse, never key/truth integrity. Publication
            // retains at most one bounded primitive array; no spelling or Matcher is retained.
            try {
                long required = Math.min(MAX_TEXT_REGEX_RESULTS, (long) index.count() * pattern.rowCount());
                int capacity = 2;
                while (capacity < required) capacity <<= 1;
                results = new long[capacity];
                results[(int) (recent >>> 1) & (capacity - 1)] = recent;
                results[(int) (key >>> 1) & (capacity - 1)] = result;
                textRegexResults = results;
            } catch (OutOfMemoryError unavailable) {
                // The exact answer is already known; this optional metadata is expendable.
            }
        }
        return matched;
    }

    /**
     * Called only after row/token bounds checks. For nonnegative int row and token < count,
     * row * count + token + 1 is positive and below 2^62. Its doubled key cannot overflow,
     * never aliases the empty word, and leaves bit zero exclusively for the Boolean result.
     */
    private static long textRegexKey(int row, int token, int count) {
        return ((long) row * count + token + 1L) << 1;
    }

    private boolean textRow(int r, byte op, int token) {
        if (op == M3TokenPattern.O_ABSENT) {
            return false;
        }
        if (op != M3TokenPattern.O_EQ) {
            throw new M3NotYetReceived("M3TokenPredicateCache.numericTextRow "
                    + "T-Rex NumericPattern semantics over the in-place span");
        }
        int lexeme = index.lexemeAt(token);
        long wanted = values[r];
        if (!caseInsensitive && lexeme > 0 && wanted > 0) {
            return lexeme == wanted;
        }
        if (!caseInsensitive && lexeme > 0) {
            return false;
        }
        return spanEquals(token, pattern.rowLiteral(r));
    }

    private boolean spanEquals(int token, String literal) {
        long start = index.startAt(token);
        long length = index.endAt(token) - start;
        if (length != literal.length() || index.endAt(token) > source.length()) {
            return false;
        }
        for (int i = 0; i < literal.length(); i++) {
            char a = source.charAt((int) start + i);
            char b = literal.charAt(i);
            if (a != b && !(caseInsensitive && fold(a) == fold(b))) {
                return false;
            }
        }
        return true;
    }

    private static char fold(char c) {
        return Character.toLowerCase(Character.toUpperCase(c));
    }

    private boolean flagsRow(byte op, long flags, int r) {
        if (op == M3TokenPattern.O_ABSENT) {
            return flags == 0;
        }
        return op == M3TokenPattern.O_EQ ? (flags & values[r]) == values[r] : compare(op, flags, values[r]);
    }

    private boolean attributeRow(int r, byte op, int token) {
        int column = columns[r];
        if (op == M3TokenPattern.O_ABSENT) {
            return column < 0 || index.attributeAt(column, token) == 0;
        }
        return compare(op, index.attributeAt(column, token), values[r]);
    }

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
