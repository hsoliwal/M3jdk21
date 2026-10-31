// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Objects;

/** Canonical Java implementation of the packed code/regex lexical feature kernel. */
public final class M3CodeTextSignalBatchJava implements M3CodeTextSignalBatch {
    public static final M3CodeTextSignalBatchJava INSTANCE =
            new M3CodeTextSignalBatchJava();

    private M3CodeTextSignalBatchJava() {}

    @Override
    public String name() {
        return "java-cpu";
    }

    @Override
    public boolean accelerated() {
        return false;
    }

    @Override
    public long[] analyze(
            char[] units,
            int[] offsets,
            int[] lengths,
            Limits limits,
            M3Progress monitor) {
        validate(units, offsets, lengths, limits);
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        long[] result = new long[offsets.length];
        progress.begin("M3 code-text lexical signals", result.length);
        try {
            for (int row = 0; row < result.length; row++) {
                if ((row & 255) == 0) progress.checkCanceled();
                result[row] = pack(units, offsets[row], lengths[row]);
                progress.worked(1);
            }
            progress.checkCanceled();
            return result;
        } finally {
            progress.done();
        }
    }

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
        long transfer =
                Math.addExact(
                        2L * units.length,
                        Math.addExact(
                                4L * offsets.length,
                                Math.addExact(4L * lengths.length, 8L * offsets.length)));
        if (transfer > limits.maxTransferBytes()) {
            throw new IllegalArgumentException("code-text transfer budget exceeded");
        }
        for (int row = 0; row < offsets.length; row++) {
            int offset = offsets[row];
            int length = lengths[row];
            if (offset < 0
                    || length < 0
                    || (long) offset + length > units.length) {
                throw new IllegalArgumentException("code-text row outside packed image");
            }
        }
    }

    static long pack(char[] units, int offset, int length) {
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
                    flags |= M3CodeTextSignals.HAS_BRACE;
                    braces++;
                    regexMeta++;
                    punctuation++;
                }
                case '(', ')' -> {
                    flags |= M3CodeTextSignals.HAS_PAREN;
                    parens++;
                    regexMeta++;
                    punctuation++;
                }
                case ';' -> {
                    flags |= M3CodeTextSignals.HAS_SEMICOLON;
                    semicolons++;
                    punctuation++;
                }
                case '\\' -> {
                    flags |= M3CodeTextSignals.HAS_BACKSLASH;
                    backslashes++;
                    punctuation++;
                }
                case '.', '*', '+', '?', '|', '^', '$' -> {
                    flags |= M3CodeTextSignals.HAS_REGEX_META;
                    regexMeta++;
                    punctuation++;
                    if (unit == '*' || unit == '+' || unit == '?') {
                        flags |= M3CodeTextSignals.HAS_QUANTIFIER;
                    }
                    if (unit == '.') flags |= M3CodeTextSignals.HAS_DOT;
                    if (unit == '|') flags |= M3CodeTextSignals.HAS_AMP_PIPE;
                }
                case '[', ']' -> {
                    flags |= M3CodeTextSignals.HAS_REGEX_META;
                    flags |= M3CodeTextSignals.HAS_CLASS_BRACKET;
                    regexMeta++;
                    punctuation++;
                }
                case '=' -> {
                    flags |= M3CodeTextSignals.HAS_ASSIGN;
                    punctuation++;
                }
                case '<', '>', '!' -> {
                    flags |= M3CodeTextSignals.HAS_COMPARISON;
                    punctuation++;
                }
                case ',' -> {
                    flags |= M3CodeTextSignals.HAS_COMMA;
                    punctuation++;
                }
                case ':' -> {
                    flags |= M3CodeTextSignals.HAS_COLON;
                    punctuation++;
                    if (index + 1 < end && units[index + 1] == ':') {
                        flags |= M3CodeTextSignals.HAS_METHOD_REFERENCE;
                    }
                }
                case '&' -> {
                    flags |= M3CodeTextSignals.HAS_AMP_PIPE;
                    punctuation++;
                }
                case '-' -> {
                    punctuation++;
                    if (index + 1 < end && units[index + 1] == '>') {
                        flags |= M3CodeTextSignals.HAS_ARROW;
                    }
                }
                default -> {
                    if (Character.isWhitespace(unit)) {
                        flags |= M3CodeTextSignals.HAS_WHITESPACE;
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
