// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Complete reusable in-memory text operators missing from the lower-level algorithm owner.
 *
 * <p>This class owns no text payload. Public String values remain compatibility facades;
 * frames preserve the canonical M3 String/index/offset boundary supplied by the running JDK.
 * Every operation is bounded by {@link Limits}; callbacks and filesystem effects remain in
 * their explicit host owners.</p>
 */
public final class M3TextOperators {
    /** Output mode for the bounded wc atom. */
    public enum CountMode { DEFAULT, LINES, WORDS, BYTES, SCALARS }

    /** Exact counters before textual formatting. */
    public record Counts(long lines, long words, long bytes, long scalars) { }

    private M3TextOperators() { }

    /** Identity atom with explicit admission. */
    public static TextFrame cat(TextFrame input, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        return input;
    }

    /**
     * Exact bytewise concatenation of framed text without platform normalization.
     * An unterminated final line is fused with the next frame's first line.
     */
    public static TextFrame cat(List<TextFrame> inputs, Limits limits) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(limits, "limits");
        if (inputs.isEmpty() || inputs.size() > limits.maxNodes()) {
            throw new IllegalArgumentException("input count budget");
        }
        long bytes = 0;
        long lines = 0;
        for (TextFrame input : inputs) {
            limits.check(Objects.requireNonNull(input, "input"));
            bytes = Math.addExact(bytes, input.byteCount());
            lines = Math.addExact(lines, input.lines().size());
            long maximumFusions = Math.max(0, inputs.size() - 1L);
            if (bytes > limits.maxBytes()
                    || Math.max(0, lines - maximumFusions) > limits.maxLines()) {
                throw new IllegalArgumentException("concatenation budget");
            }
        }
        List<TextLine> joined = new ArrayList<>();
        for (TextFrame input : inputs) {
            if (input.lines().isEmpty()) continue;
            int start = 0;
            if (!joined.isEmpty() && joined.getLast().ending().isEmpty()) {
                TextLine previous = joined.removeLast();
                TextLine first = input.lines().getFirst();
                joined.add(new TextLine(previous.content() + first.content(), first.ending()));
                start = 1;
            }
            joined.addAll(input.lines().subList(start, input.lines().size()));
        }
        FrameCollector output = new FrameCollector(limits);
        joined.forEach(output);
        return output.frame();
    }

    /** First {@code count} line records. */
    public static TextFrame head(TextFrame input, int count, Limits limits) {
        admitted(input, count, limits);
        return new TextFrame(input.lines().subList(0, Math.min(count, input.lines().size())));
    }

    /** Last {@code count} line records. */
    public static TextFrame tail(TextFrame input, int count, Limits limits) {
        admitted(input, count, limits);
        int size = input.lines().size();
        return new TextFrame(input.lines().subList(size - Math.min(count, size), size));
    }

    /** Adjacent duplicate content with count<TAB>content and the first record ending. */
    public static TextFrame uniqCounted(TextFrame input, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        FrameCollector output = new FrameCollector(limits);
        for (int start = 0; start < input.lines().size();) {
            TextLine first = input.lines().get(start);
            int end = start + 1;
            while (end < input.lines().size()
                    && first.content().equals(input.lines().get(end).content())) {
                end++;
            }
            output.accept(new TextLine((end - start) + "\t" + first.content(), first.ending()));
            start = end;
        }
        return output.frame();
    }

    /** Literal case-sensitive line selection with optional inversion. */
    public static TextFrame grepLiteral(TextFrame input, String literal, boolean invert,
                                        Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        NativeUtf16Literal.Compiled compiled = NativeUtf16Literal.compile(literal);
        FrameCollector output = new FrameCollector(limits);
        for (TextLine line : input.lines()) {
            if (compiled.contains(line.content()) != invert) output.accept(line);
        }
        return output.frame();
    }

    /** Java-regex line selection with optional inversion. Runtime is not a hard CPU bound. */
    public static TextFrame grepRegex(TextFrame input, Pattern regex, boolean invert,
                                      Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        Objects.requireNonNull(regex, "regex");
        FrameCollector output = new FrameCollector(limits);
        for (TextLine line : input.lines()) {
            if (regex.matcher(line.content()).find() != invert) output.accept(line);
        }
        return output.frame();
    }

    /** Counts LF bytes, whitespace-delimited words, UTF-8 bytes, and Unicode scalars. */
    public static Counts counts(TextFrame input, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        long lineFeeds = 0;
        long words = 0;
        long scalars = 0;
        boolean insideWord = false;
        for (TextLine line : input.lines()) {
            for (String part : List.of(line.content(), line.ending())) {
                for (int at = 0; at < part.length();) {
                    int value = part.codePointAt(at);
                    at += Character.charCount(value);
                    scalars = Math.addExact(scalars, 1);
                    if (value == '\n') lineFeeds = Math.addExact(lineFeeds, 1);
                    boolean word = !Character.isWhitespace(value);
                    if (word && !insideWord) words = Math.addExact(words, 1);
                    insideWord = word;
                }
            }
        }
        return new Counts(lineFeeds, words, input.byteCount(), scalars);
    }

    /** Canonical one-line wc formatting. */
    public static TextFrame wc(TextFrame input, CountMode mode, Limits limits) {
        Objects.requireNonNull(mode, "mode");
        Counts count = counts(input, limits);
        String value = switch (mode) {
            case LINES -> Long.toString(count.lines());
            case WORDS -> Long.toString(count.words());
            case BYTES -> Long.toString(count.bytes());
            case SCALARS -> Long.toString(count.scalars());
            case DEFAULT -> count.lines() + "\t" + count.words() + "\t" + count.bytes();
        };
        FrameCollector output = new FrameCollector(limits);
        output.accept(new TextLine(value, "\n"));
        return output.frame();
    }

    /**
     * Produces immutable literal argv batches. No shell, process, reflection, or callback executes.
     * The existing {@link TextTools#xargs(TextFrame, List, int, java.util.function.Consumer, Limits)}
     * effectful host adapter delegates conceptually to this planning contract.
     */
    public static List<List<String>> xargsBatches(TextFrame input, List<String> prefix,
                                                  int batchSize, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        List<String> fixed = List.copyOf(Objects.requireNonNull(prefix, "prefix"));
        if (batchSize < 1 || fixed.size() > limits.maxNodes()) {
            throw new IllegalArgumentException("argument budget");
        }
        long fixedBytes = 0;
        for (String argument : fixed) {
            Canonical.validUnicode(argument);
            fixedBytes = Math.addExact(fixedBytes, Utf8Segments.length(argument));
            if (fixedBytes > limits.maxBytes()) {
                throw new IllegalArgumentException("argument byte budget");
            }
        }
        long outputBytes = 0;
        List<List<String>> batches = new ArrayList<>();
        for (int start = 0; start < input.lines().size();) {
            int end = start + Math.min(batchSize, input.lines().size() - start);
            if ((long) fixed.size() + end - start > limits.maxNodes()) {
                throw new IllegalArgumentException("batch argument count budget");
            }
            List<String> arguments = new ArrayList<>(fixed.size() + end - start);
            arguments.addAll(fixed);
            outputBytes = Math.addExact(outputBytes, fixedBytes);
            for (int index = start; index < end; index++) {
                String argument = input.lines().get(index).content();
                arguments.add(argument);
                outputBytes = Math.addExact(outputBytes, Utf8Segments.length(argument));
            }
            if (outputBytes > limits.maxBytes()) {
                throw new IllegalArgumentException("planned argument byte budget");
            }
            batches.add(List.copyOf(arguments));
            if (batches.size() > limits.maxLines()) {
                throw new IllegalArgumentException("batch count budget");
            }
            start = end;
        }
        return List.copyOf(batches);
    }

    private static void admitted(TextFrame input, int count, Limits limits) {
        if (count < 0) throw new IllegalArgumentException("negative line count");
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
    }
}
