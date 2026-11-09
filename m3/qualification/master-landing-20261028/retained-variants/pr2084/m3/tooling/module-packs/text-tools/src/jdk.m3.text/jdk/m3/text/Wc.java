// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Unix4j-compatible {@code wc}: lines, words and chars in UTF-16 units.
 *
 * <p>Semantics are ported from Unix4j wc (MIT, tools4j/unix4j
 * 0a127602907978efbff7ea297bd553274ba70a8e; see legal/NOTICE.txt): text splits at
 * CR, LF, CRLF and LFCR; a line counts 1; words are runs of units for which
 * {@link Character#isWhitespace(char)} is false, inside line content only; chars
 * count content plus ending; a lone empty line counts as zero lines.
 *
 * <p>Three paths share that contract. {@link #countJava} is the pure-Java reference;
 * {@link #countNative} runs the libm3textio kernel over direct UTF-16 or Latin-1
 * windows; lines cross JNI as SoA lanes (one UTF-16 buffer plus content-length and
 * ending-length {@code int[]} lanes), never one object per line. {@link #count}
 * selects a path with {@value #MODE_PROPERTY} ({@code java}, {@code native} or
 * {@code oracle}); oracle runs both and fails on any disagreement.
 */
public final class Wc {
    /** System property selecting {@code java} (default), {@code native} or {@code oracle}. */
    public static final String MODE_PROPERTY = "jdk.m3.text.wc.mode";
    /** Default UTF-16 units per native window. */
    public static final int DEFAULT_WINDOW_UNITS = 8192;
    private static final int MAX_DIRECT_UNITS = Integer.MAX_VALUE / 2;

    private Wc() { }

    /** Path selection for {@link #count(CharSequence)} and {@link #count(TextFrame, Limits)}. */
    public enum Mode { JAVA, NATIVE, ORACLE }

    //FLLDCIM: IDL - atom wc.format: Counts record and Unix4j fixSizeString output line
    /** Totals; {@link #format} renders the Unix4j wc output line. */
    public record Counts(long lines, long words, long chars) {
        public Counts {
            if (lines < 0 || words < 0 || chars < 0) throw new IllegalArgumentException("negative wc count");
        }

        /**
         * Unix4j wc output line without its ending: selected counters in lines, words,
         * chars order (none selected means all), right-aligned to the widest count plus
         * two, or unpadded when exactly one counter is shown.
         */
        public String format(boolean showLines, boolean showWords, boolean showChars) {
            boolean all = !showLines && !showWords && !showChars;
            long[] values = new long[3];
            int n = 0;
            if (all || showLines) values[n++] = lines;
            if (all || showWords) values[n++] = words;
            if (all || showChars) values[n++] = chars;
            int width = 0;
            for (int i = 0; i < n; i++) width = Math.max(width, Long.toString(values[i]).length());
            if (n > 1) width += 2;
            StringBuilder out = new StringBuilder(width * n);
            for (int i = 0; i < n; i++) {
                String digits = Long.toString(values[i]);
                out.repeat(' ', width - digits.length()).append(digits);
            }
            return out.toString();
        }
    }

    //FLLDCIM: IOP - atom wc.facade: mode switch, native windows/lanes, feed/finish, oracle equality
    public static Mode mode() {
        String value = System.getProperty(MODE_PROPERTY, "java");
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "java" -> Mode.JAVA;
            case "native" -> Mode.NATIVE;
            case "oracle" -> Mode.ORACLE;
            default -> throw new IllegalArgumentException("invalid " + MODE_PROPERTY + ": " + value);
        };
    }

    /** True when libm3textio loads; the failure itself is reported by the native paths. */
    public static boolean nativeAvailable() {
        try {
            WcNative.ensureLoaded();
            return true;
        } catch (UnsatisfiedLinkError absent) {
            return false;
        }
    }

    public static Counts count(CharSequence text) {
        Objects.requireNonNull(text, "text");
        return switch (mode()) {
            case JAVA -> countJava(text);
            case NATIVE -> countNative(text, DEFAULT_WINDOW_UNITS, false);
            case ORACLE -> agree(countJava(text), countNative(text, DEFAULT_WINDOW_UNITS, false));
        };
    }

    /** Counts an existing frame with its own line framing. */
    public static Counts count(TextFrame frame, Limits limits) {
        limits.check(frame);
        return switch (mode()) {
            case JAVA -> countJava(frame, limits);
            case NATIVE -> countNative(frame, Integer.MAX_VALUE, false, limits);
            case ORACLE -> agree(countJava(frame, limits), countNative(frame, Integer.MAX_VALUE, false, limits));
        };
    }

    /** Pure-Java reference: Unix4j splitLines framing, then per-line counters. */
    public static Counts countJava(CharSequence text) {
        return countLanesJava(split(Objects.requireNonNull(text, "text")));
    }

    public static Counts countJava(TextFrame frame, Limits limits) {
        limits.check(frame);
        return countLanesJava(lanes(frame));
    }

    /** Native stream kernel; windows may split words, lines and CRLF pairs. */
    public static Counts countNative(CharSequence text, int windowUnits, boolean bigEndian) {
        Objects.requireNonNull(text, "text");
        if (windowUnits < 1 || windowUnits > MAX_DIRECT_UNITS) throw new IllegalArgumentException("window units");
        WcNative.ensureLoaded();
        int length = text.length();
        ByteBuffer window = ByteBuffer.allocateDirect(Math.max(1, Math.min(windowUnits, length)) * 2)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        long[] state = WcNative.newState();
        for (int at = 0; at < length; ) {
            int units = Math.min(windowUnits, length - at);
            window.clear();
            for (int i = 0; i < units; i++) window.putChar(text.charAt(at + i));
            WcNative.count0(window, 0, units, 2, bigEndian, false, state);
            at += units;
        }
        return finish(state);
    }

    /** Native lane kernel over the frame's own lines. */
    public static Counts countNative(TextFrame frame, int linesPerCall, boolean bigEndian, Limits limits) {
        limits.check(frame);
        return countLanesNative(lanes(frame), linesPerCall, bigEndian);
    }

    /** Native lane kernel over Unix4j splitLines framing (Java builds the lanes). */
    public static Counts countNativeLanes(CharSequence text, int linesPerCall, boolean bigEndian) {
        return countLanesNative(split(Objects.requireNonNull(text, "text")), linesPerCall, bigEndian);
    }

    /** Fresh caller-owned kernel state for {@link #feed} and {@link #finish}. */
    public static long[] newState() {
        return WcNative.newState();
    }

    /**
     * Feeds one window of {@code units} code units starting at {@code byteOffset} of a
     * direct buffer: UTF-16 for width 2 (byte order by {@code bigEndian}) or Latin-1
     * for width 1. State carries in-word, pending CR/LF and open-line facts across calls.
     */
    public static void feed(ByteBuffer window, int byteOffset, int units, int width, boolean bigEndian, long[] state) {
        Objects.requireNonNull(state, "state");
        WcNative.ensureLoaded();
        WcNative.count0(window, byteOffset, units, width, bigEndian, false, state);
    }

    /** Closes a fed stream and returns its totals; the state then refuses further input. */
    public static Counts finish(long[] state) {
        Objects.requireNonNull(state, "state");
        WcNative.ensureLoaded();
        WcNative.count0(null, 0, 0, 2, false, true, state);
        return new Counts(state[WcNative.LINES], state[WcNative.WORDS], state[WcNative.CHARS]);
    }

    private static Counts agree(Counts reference, Counts accelerated) {
        if (!reference.equals(accelerated)) {
            throw new IllegalStateException("M3 JNI wc disagrees with Java reference: "
                    + reference + " != " + accelerated);
        }
        return reference;
    }

    private static Counts countLanesNative(Lanes lanes, int linesPerCall, boolean bigEndian) {
        if (linesPerCall < 1) throw new IllegalArgumentException("lines per call");
        int length = lanes.units().length();
        if (length > MAX_DIRECT_UNITS) throw new IllegalArgumentException("text exceeds one direct UTF-16 buffer");
        WcNative.ensureLoaded();
        ByteBuffer utf16 = ByteBuffer.allocateDirect(Math.max(1, length) * 2)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < length; i++) utf16.putChar(lanes.units().charAt(i));
        long[] state = WcNative.newState();
        int byteOffset = 0;
        for (int from = 0; from < lanes.size(); ) {
            int to = from + Math.min(linesPerCall, lanes.size() - from);
            WcNative.countLines0(utf16, byteOffset, bigEndian, lanes.content(), lanes.ending(), from, to, false, state);
            for (int i = from; i < to; i++) byteOffset += 2 * (lanes.content()[i] + lanes.ending()[i]);
            from = to;
        }
        WcNative.countLines0(utf16, byteOffset, bigEndian, lanes.content(), lanes.ending(), 0, 0, true, state);
        return new Counts(state[WcNative.LINES], state[WcNative.WORDS], state[WcNative.CHARS]);
    }

    //FLLDCIM: TOP - atom wc.reference: Java oracle path (splitLines lanes, CounterType counters)
    /** Line framing as packed SoA lanes over one UTF-16 sequence; one object per frame, not per line. */
    private record Lanes(CharSequence units, int[] content, int[] ending, int size) { }

    /** Port of Unix4j StringUtil.splitLines: CR, LF, CRLF or LFCR end a line. */
    private static Lanes split(CharSequence s) {
        int[] content = new int[16];
        int[] ending = new int[16];
        int size = 0;
        int start = 0;
        int index = 0;
        int length = s.length();
        while (index < length) {
            char ch = s.charAt(index);
            if (ch == '\n' || ch == '\r') {
                int endingStart = index++;
                if (index < length) {
                    char next = s.charAt(index);
                    if (next != ch && (next == '\n' || next == '\r')) index++;
                }
                if (size == content.length) {
                    content = Arrays.copyOf(content, size * 2);
                    ending = Arrays.copyOf(ending, size * 2);
                }
                content[size] = endingStart - start;
                ending[size++] = index - endingStart;
                start = index;
            } else {
                index++;
            }
        }
        if (start < length) {
            if (size == content.length) {
                content = Arrays.copyOf(content, size + 1);
                ending = Arrays.copyOf(ending, size + 1);
            }
            content[size] = length - start;
            ending[size++] = 0;
        }
        return new Lanes(s, content, ending, size);
    }

    private static Lanes lanes(TextFrame frame) {
        int size = frame.lines().size();
        int[] content = new int[size];
        int[] ending = new int[size];
        StringBuilder units = new StringBuilder();
        for (int i = 0; i < size; i++) {
            TextLine line = frame.lines().get(i);
            content[i] = line.content().length();
            ending[i] = line.ending().length();
            units.append(line.content()).append(line.ending());
        }
        return new Lanes(units, content, ending, size);
    }

    /** Port of Unix4j CounterType (Lines, Words, Chars) and Counters.dontCountSingleEmptyLine. */
    private static Counts countLanesJava(Lanes lanes) {
        long lines = 0;
        long words = 0;
        long chars = 0;
        boolean lastLineWasEmpty = false;
        int at = 0;
        for (int line = 0; line < lanes.size(); line++) {
            int length = lanes.content()[line];
            int lineWords = 0;
            int wordLength = 0;
            for (int i = 0; i < length; i++) {
                if (Character.isWhitespace(lanes.units().charAt(at + i))) {
                    if (wordLength > 0) lineWords++;
                    wordLength = 0;
                } else {
                    wordLength++;
                }
            }
            words += wordLength > 0 ? lineWords + 1 : lineWords;
            lines++;
            chars += length + lanes.ending()[line];
            lastLineWasEmpty = length == 0;
            at += length + lanes.ending()[line];
        }
        if (lastLineWasEmpty && lines == 1) lines = 0;
        return new Counts(lines, words, chars);
    }
}
