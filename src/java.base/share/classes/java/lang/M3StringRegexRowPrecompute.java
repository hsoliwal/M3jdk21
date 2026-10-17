/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal bounded batch regex candidate index over immutable M3 String coordinates.
 *
 * <p>This is the JDK receiving analogue of Synexia's MIndexRegexRowPrecompute. It reuses
 * canonical M3StringFacts.bitSignal64; no new spelling, native address, JNI signature,
 * String field, String API, or regex interpreter is introduced. Pattern/Matcher remains the
 * authority outside a mathematically exact zero-flags ASCII literal shape.</p>
 */
final class M3StringRegexRowPrecompute {
    enum Shape { GENERIC, EXACT_LITERAL, PREFIX_LITERAL, SUFFIX_LITERAL, CONTAINS_LITERAL }
    private static final long MAX_BYTES = 32L << 20;
    private static final int MAX_LITERAL = 256;
    private static final int SIGNAL_BITS = Long.SIZE;
    private final M3String[] values;
    private final int words;
    private final long[] postings;
    private volatile M3StringAffixPrecompute affixes;
    private int affixQueries;

    private M3StringRegexRowPrecompute(M3String[] inputs) {
        values = Objects.requireNonNull(inputs, "inputs").clone();
        for (M3String value : values) Objects.requireNonNull(value, "M3 String value");
        words = Math.toIntExact(((long) values.length + 63L) >>> 6);
        long bytes = Math.multiplyExact(Math.multiplyExact((long) words, SIGNAL_BITS), Long.BYTES);
        Runtime runtime = Runtime.getRuntime();
        long used = Math.max(0, runtime.totalMemory() - runtime.freeMemory());
        long budget = Math.min(MAX_BYTES,
                Math.min(runtime.maxMemory() >>> 5,
                        Math.max(0, runtime.maxMemory() - used) >>> 3));
        if (bytes == 0L || bytes > budget || (long) words * SIGNAL_BITS > Integer.MAX_VALUE) {
            postings = null;
            return;
        }
        long[] prepared;
        try {
            prepared = new long[words * SIGNAL_BITS];
            for (int row = 0; row < values.length; row++) {
                long signal = values[row].facts().bitSignal64;
                int word = row >>> 6;
                long bit = 1L << (row & 63);
                while (signal != 0L) {
                    int position = Long.numberOfTrailingZeros(signal);
                    prepared[position * words + word] |= bit;
                    signal &= signal - 1L;
                }
            }
        } catch (OutOfMemoryError unavailable) {
            postings = null;
            return;
        }
        postings = prepared;
    }

    static M3StringRegexRowPrecompute compile(M3String[] values) {
        return new M3StringRegexRowPrecompute(values);
    }

    long retainedPostingBytes() {
        return postings == null ? 0L : (long) postings.length * Long.BYTES;
    }

    int size() { return values.length; }

    M3String valueAt(int row) { return values[Objects.checkIndex(row, values.length)]; }

    /** Admit lexical/reverse-lexical rank planes after repeated eligible queries. */
    private M3StringAffixPrecompute reusedAffixes(Shape shape, String literal) {
        if (values.length < 64 || literal.length() < 2
                || shape != Shape.EXACT_LITERAL && shape != Shape.PREFIX_LITERAL
                        && shape != Shape.SUFFIX_LITERAL) return null;
        M3StringAffixPrecompute available = affixes;
        if (available != null) return available;
        synchronized (this) {
            // Build on fourth eligible use. A memory-pressure refusal retries only
            // every 128 further eligible uses rather than re-triggering on every call.
            if (affixQueries != Integer.MAX_VALUE) affixQueries++;
            if (affixes == null && (affixQueries == 4 || (affixQueries >= 128
                    && (affixQueries & 127) == 0))) {
                affixes = M3StringAffixPrecompute.tryPrepare(values);
            }
            return affixes;
        }
    }

    Prepared prepare(String regex, int flags) {
        Pattern pattern = Pattern.compile(Objects.requireNonNull(regex, "regex"), flags);
        Shape shape = Shape.GENERIC;
        String literal = "";
        if (flags == 0) {
            boolean before = regex.startsWith(".*");
            boolean after = regex.endsWith(".*");
            int start = before ? 2 : 0;
            int end = regex.length() - (after ? 2 : 0);
            if (start <= end) {
                String middle = regex.substring(start, end);
                if (middle.length() <= MAX_LITERAL && plainAscii(middle)) {
                    literal = middle;
                    shape = before && after ? Shape.CONTAINS_LITERAL
                            : before ? Shape.SUFFIX_LITERAL
                            : after ? Shape.PREFIX_LITERAL : Shape.EXACT_LITERAL;
                    if (middle.isEmpty() && (before || after)) shape = Shape.GENERIC;
                }
            }
        }
        long[] mask = null;
        if (postings != null && shape != Shape.GENERIC && !literal.isEmpty()) {
            try {
                mask = new long[words];
                Arrays.fill(mask, -1L);
                long required = 0L;
                for (int i = 0; i < literal.length(); i++)
                    required |= M3StringFacts.codeUnitSignal(literal.charAt(i));
                while (required != 0L) {
                    int position = Long.numberOfTrailingZeros(required);
                    int base = position * words;
                    for (int word = 0; word < words; word++) mask[word] &= postings[base + word];
                    required &= required - 1L;
                }
            } catch (OutOfMemoryError unavailable) {
                mask = null;
            }
        }
        return new Prepared(pattern, shape, literal, mask, reusedAffixes(shape, literal));
    }

    private static boolean plainAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                    || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    private static boolean equalRange(CharSequence subject, String literal, int start) {
        for (int i = 0; i < literal.length(); i++) {
            if (subject.charAt(start + i) != literal.charAt(i)) return false;
        }
        return true;
    }

    final class Prepared {
        private final Pattern pattern;
        private final Shape shape;
        private final String literal;
        private final long[] mask;
        private final M3StringAffixPrecompute affix;

        private Prepared(Pattern pattern, Shape shape, String literal, long[] mask,
                M3StringAffixPrecompute affix) {
            this.pattern = pattern;
            this.shape = shape;
            this.literal = literal;
            this.mask = mask;
            this.affix = affix;
        }

        Shape shape() { return shape; }
        long candidateMaskBytes() { return mask == null ? 0L : (long) mask.length * Long.BYTES; }

        private int[] indexedCandidates() {
            if (affix == null) return null;
            boolean suffix = shape == Shape.SUFFIX_LITERAL;
            M3StringAffixPrecompute.Range range = suffix
                    ? affix.suffixRange(literal) : affix.prefixRange(literal);
            // Dense intervals retain the existing posting/linear route. Avoid sorting
            // most of the corpus merely to restore original encounter order.
            if ((long) range.size() * 2L > values.length) return null;
            int[] rows = affix.rowsInSourceOrder(range, suffix);
            // Retain the existing mandatory-character posting evidence even on
            // the affix route, so exact indexed candidates can only get smaller.
            int count = 0;
            for (int row : rows) {
                if (mask != null && (mask[row >>> 6] & (1L << (row & 63))) == 0L) continue;
                if (shape == Shape.EXACT_LITERAL && values[row].length() != literal.length()) {
                    continue;
                }
                rows[count++] = row;
            }
            return count == rows.length ? rows : Arrays.copyOf(rows, count);
        }

        /** Return an ordered list of original row coordinates, never M3String wrappers. */
        int[] candidateRows() {
            int[] indexed = indexedCandidates();
            if (indexed != null) return indexed;
            int[] selected = new int[Math.min(values.length, 64)];
            int size = 0;
            for (int word = 0; word < words; word++) {
                long active = mask == null ? -1L : mask[word];
                while (active != 0L) {
                    int position = Long.numberOfTrailingZeros(active);
                    active &= active - 1L;
                    int row = (word << 6) + position;
                    if (row >= values.length) break;
                    M3String subject = values[row];
                    int width = literal.length();
                    if (shape != Shape.GENERIC && subject.length() < width) continue;
                    boolean admitted = switch (shape) {
                        case GENERIC -> true;
                        case EXACT_LITERAL -> subject.length() == width
                                && equalRange(subject, literal, 0);
                        case PREFIX_LITERAL -> equalRange(subject, literal, 0);
                        case SUFFIX_LITERAL -> equalRange(subject, literal, subject.length() - width);
                        case CONTAINS_LITERAL -> contains(subject, literal);
                    };
                    if (!admitted) continue;
                    if (size == selected.length) {
                        int capacity = selected.length <= values.length / 2
                                ? selected.length * 2 : values.length;
                        selected = Arrays.copyOf(selected, capacity);
                    }
                    selected[size++] = row;
                }
            }
            return Arrays.copyOf(selected, size);
        }

        int[] matchingRows() {
            int[] rows = candidateRows();
            if (shape == Shape.EXACT_LITERAL) return rows;
            Matcher execution = pattern.matcher("");
            int count = 0;
            for (int row : rows) {
                if (execution.reset(values[row]).matches()) rows[count++] = row;
            }
            return Arrays.copyOf(rows, count);
        }
    }

    private static boolean contains(CharSequence text, String literal) {
        int width = literal.length();
        for (int start = 0; start <= text.length() - width; start++) {
            if (equalRange(text, literal, start)) return true;
        }
        return false;
    }
}
