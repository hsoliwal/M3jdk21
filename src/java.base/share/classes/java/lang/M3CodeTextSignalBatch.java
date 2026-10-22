/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

/**
 * Bounded packed-image receiver for code-text lexical precompute.
 *
 * <p>The packed lane is a necessary-condition routing signal. It is not Java syntax proof,
 * regex validity proof, or semantic authority. Java remains the default and semantic reference.
 * Explicit native execution requires the integrated libjava ABI and never silently falls back.</p>
 */
final class M3CodeTextSignalBatch {
    enum Backend { JAVA, NATIVE_REQUIRED }

    record Limits(long maxTransferBytes, long maxUtf16Units, int maxRows) {
        static final Limits DEFAULT = new Limits(64L << 20, 16_000_000L, 1_000_000);

        Limits {
            if (maxTransferBytes < 0 || maxUtf16Units < 0 || maxRows < 1) {
                throw new IllegalArgumentException("invalid code-text signal limits");
            }
        }
    }

    private M3CodeTextSignalBatch() {}

    static long[] analyze(
            char[] units,
            int[] offsets,
            int[] lengths,
            Limits limits) {
        validate(units, offsets, lengths, limits);
        long[] result = new long[offsets.length];
        for (int row = 0; row < result.length; row++) {
            result[row] = pack(units, offsets[row], lengths[row]);
        }
        return result;
    }

    /**
     * Selects an internal implementation explicitly, without an automatic size threshold.
     * The caller must keep all three input arrays unchanged for the duration of this call.
     * Rows may overlap and appear in any order; no text or result is retained after return.
     * Native admission validates the complete geometry before allocating or scanning output.
     * An unavailable required native ABI throws UnsatisfiedLinkError, including for zero rows.
     */
    static long[] analyze(
            char[] units,
            int[] offsets,
            int[] lengths,
            Limits limits,
            Backend backend) {
        return switch (Objects.requireNonNull(backend, "backend")) {
            case JAVA -> analyze(units, offsets, lengths, limits);
            case NATIVE_REQUIRED -> {
                validate(units, offsets, lengths, limits);
                yield analyzeNative(units, offsets, lengths,
                        limits.maxTransferBytes(), limits.maxUtf16Units(), limits.maxRows());
            }
        };
    }

    private static native long[] analyzeNative(
            char[] units,
            int[] offsets,
            int[] lengths,
            long maxTransferBytes,
            long maxUtf16Units,
            int maxRows);

    static void validate(
            char[] units,
            int[] offsets,
            int[] lengths,
            Limits limits) {
        Objects.requireNonNull(units, "units");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        Objects.requireNonNull(limits, "limits");
        if (offsets.length != lengths.length || offsets.length > limits.maxRows()) {
            throw new IllegalArgumentException("code-text row geometry");
        }
        if (units.length > limits.maxUtf16Units()) {
            throw new IllegalArgumentException("code-text UTF-16 budget exceeded");
        }
        long transfer = Math.addExact(
                Math.addExact(2L * units.length, 4L * offsets.length),
                Math.addExact(4L * lengths.length, 8L * offsets.length));
        if (transfer > limits.maxTransferBytes()) {
            throw new IllegalArgumentException("code-text transfer budget exceeded");
        }
        for (int row = 0; row < offsets.length; row++) {
            int offset = offsets[row];
            int length = lengths[row];
            if (offset < 0 || length < 0 || (long) offset + length > units.length) {
                throw new IllegalArgumentException("code-text row outside packed image");
            }
        }
    }

    static long pack(char[] units, int offset, int length) {
        Objects.requireNonNull(units, "units");
        Objects.checkFromIndexSize(offset, length, units.length);
        int flags = 0;
        int semicolons = 0;
        int braces = 0;
        int parens = 0;
        int backslashes = 0;
        int regexMeta = 0;
        int punctuation = 0;
        int end = offset + length;
        for (int index = offset; index < end; index++) {
            char unit = units[index];
            switch (unit) {
                case '{', '}' -> {
                    flags |= M3CodeTextFacts.HAS_BRACE;
                    braces++;
                    regexMeta++;
                    punctuation++;
                }
                case '(', ')' -> {
                    flags |= M3CodeTextFacts.HAS_PAREN;
                    parens++;
                    regexMeta++;
                    punctuation++;
                }
                case ';' -> {
                    flags |= M3CodeTextFacts.HAS_SEMICOLON;
                    semicolons++;
                    punctuation++;
                }
                case '\\' -> {
                    flags |= M3CodeTextFacts.HAS_BACKSLASH;
                    backslashes++;
                    punctuation++;
                }
                case '.', '*', '+', '?', '|', '^', '$' -> {
                    flags |= M3CodeTextFacts.HAS_REGEX_META;
                    regexMeta++;
                    punctuation++;
                    if (unit == '*' || unit == '+' || unit == '?') {
                        flags |= M3CodeTextFacts.HAS_QUANTIFIER;
                    }
                    if (unit == '.') flags |= M3CodeTextFacts.HAS_DOT;
                    if (unit == '|') flags |= M3CodeTextFacts.HAS_AMP_PIPE;
                }
                case '[', ']' -> {
                    flags |= M3CodeTextFacts.HAS_REGEX_META | M3CodeTextFacts.HAS_CLASS_BRACKET;
                    regexMeta++;
                    punctuation++;
                }
                case '=' -> {
                    flags |= M3CodeTextFacts.HAS_ASSIGN;
                    punctuation++;
                }
                case '<', '>', '!' -> {
                    flags |= M3CodeTextFacts.HAS_COMPARISON;
                    punctuation++;
                }
                case ',' -> {
                    flags |= M3CodeTextFacts.HAS_COMMA;
                    punctuation++;
                }
                case ':' -> {
                    flags |= M3CodeTextFacts.HAS_COLON;
                    punctuation++;
                    if (index + 1 < end && units[index + 1] == ':') {
                        flags |= M3CodeTextFacts.HAS_METHOD_REFERENCE;
                    }
                }
                case '&' -> {
                    flags |= M3CodeTextFacts.HAS_AMP_PIPE;
                    punctuation++;
                }
                case '-' -> {
                    punctuation++;
                    if (index + 1 < end && units[index + 1] == '>') {
                        flags |= M3CodeTextFacts.HAS_ARROW;
                    }
                }
                default -> {
                    if (Character.isWhitespace(unit)) {
                        flags |= M3CodeTextFacts.HAS_WHITESPACE;
                    }
                }
            }
        }
        return Integer.toUnsignedLong(flags)
                | ((long) Math.min(semicolons, 255) << 16)
                | ((long) Math.min(braces, 255) << 24)
                | ((long) Math.min(parens, 255) << 32)
                | ((long) Math.min(backslashes, 255) << 40)
                | ((long) Math.min(regexMeta, 255) << 48)
                | ((long) Math.min(punctuation, 255) << 56);
    }
}
