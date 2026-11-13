// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Compiled, single-pass, bounded Aho-Corasick necessary-literal selector.
 *
 * <p>Original Java/M3 String owners stay with the caller. This class retains only
 * primitive automaton lanes and produces an ordinal bitmap. Java regex remains
 * authoritative for accepting matches. A missing optional native library does
 * not poison the class: existing M3 JNI grep remains the native fallback.
 * No JNI pointer, Java heap address, or line object is ever retained by C.
 */
final class M3NativeAho {
    private static final int ABI = 0x00010001;
    private static final int MAX_TERMS = 64;
    private static final int MAX_STATES = 16384;
    private static final int MAX_TOTAL_UNITS = MAX_STATES - 1;
    private static final int MAX_PACKED_UNITS = 1 << 20;

    private final int[] first;
    private final char[] symbol;
    private final int[] target;
    private final int[] sibling;
    private final int[] failure;
    private final long[] outputs;
    private final long requiredMask;
    private final long emptyMask;

    private M3NativeAho(int[] first, char[] symbol, int[] target, int[] sibling,
                        int[] failure, long[] outputs, long requiredMask, long emptyMask) {
        this.first = first;
        this.symbol = symbol;
        this.target = target;
        this.sibling = sibling;
        this.failure = failure;
        this.outputs = outputs;
        this.requiredMask = requiredMask;
        this.emptyMask = emptyMask;
    }

    static M3NativeAho compile(List<String> input) {
        Objects.requireNonNull(input, "required literals");
        List<String> terms = List.copyOf(input);
        if (terms.isEmpty() || terms.size() > MAX_TERMS) {
            throw new IllegalArgumentException("expected 1..64 required literals");
        }
        int units = 0;
        for (String term : terms) {
            units = Math.addExact(units, term.length());
            if (units > MAX_TOTAL_UNITS) throw new IllegalArgumentException("automaton budget");
        }
        int capacity = Math.addExact(units, 1);
        int[] head = new int[capacity];
        Arrays.fill(head, -1);
        char[] chars = new char[units];
        int[] children = new int[units];
        int[] next = new int[units];
        long[] output = new long[capacity];
        int states = 1, edges = 0;
        long empty = 0;
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            long bit = 1L << i;
            if (term.isEmpty()) {
                empty |= bit;
                continue;
            }
            int state = 0;
            for (int j = 0; j < term.length(); j++) {
                char ch = term.charAt(j);
                int child = find(head, chars, children, next, state, ch);
                if (child < 0) {
                    child = states++;
                    chars[edges] = ch;
                    children[edges] = child;
                    next[edges] = head[state];
                    head[state] = edges++;
                }
                state = child;
            }
            output[state] |= bit;
        }
        head = Arrays.copyOf(head, states);
        output = Arrays.copyOf(output, states);
        chars = Arrays.copyOf(chars, edges);
        children = Arrays.copyOf(children, edges);
        next = Arrays.copyOf(next, edges);
        int[] fails = new int[states];
        int[] queue = new int[states];
        int tail = 0;
        for (int edge = head[0]; edge >= 0; edge = next[edge]) {
            queue[tail++] = children[edge];
        }
        for (int at = 0; at < tail; at++) {
            int from = queue[at];
            for (int edge = head[from]; edge >= 0; edge = next[edge]) {
                int to = children[edge];
                int f = fails[from];
                int transition = find(head, chars, children, next, f, chars[edge]);
                while (transition < 0 && f != 0) {
                    f = fails[f];
                    transition = find(head, chars, children, next, f, chars[edge]);
                }
                fails[to] = transition < 0 ? 0 : transition;
                output[to] |= output[fails[to]];
                queue[tail++] = to;
            }
        }
        long required = terms.size() == 64 ? -1L : (1L << terms.size()) - 1L;
        return new M3NativeAho(head, chars, children, next, fails, output, required, empty);
    }

    private static int find(int[] head, char[] chars, int[] target, int[] next,
                            int state, char ch) {
        for (int edge = head[state]; edge >= 0; edge = next[edge]) {
            if (chars[edge] == ch) return target[edge];
        }
        return -1;
    }

    /**
     * Is the real JNI accelerator installed and ABI-compatible?
     * Missing library means use the already-installed M3 native grep.
     * Wrong ABI or broken native module remains a hard linkage failure.
     */
    static boolean available() { return Native.PRESENT; }

    /** Explicit maximum: refusing this optional batch preserves prior JNI path. */
    boolean fits(List<String> lines) {
        long units = 0;
        for (String line : lines) {
            units += Objects.requireNonNull(line, "line").length();
            if (units > MAX_PACKED_UNITS) return false;
        }
        return lines.size() <= 1_000_000;
    }

    /**
     * Selected ordinals as a primitive bitmap. Called only with immutable Strings;
     * caller must use the real Pattern oracle before publishing positives.
     */
    long[] selectNative(List<String> lines) {
        Objects.requireNonNull(lines, "lines");
        if (!fits(lines)) throw new IllegalArgumentException("native Aho window exceeds budget");
        if (!available()) throw new UnsatisfiedLinkError("optional m3unixaho library not installed");
        int count = lines.size();
        int[] offsets = new int[count + 1];
        for (int i = 0; i < count; i++) offsets[i + 1] = offsets[i] + lines.get(i).length();
        char[] packed = new char[offsets[count]];
        for (int i = 0; i < count; i++) {
            // String.getChars keeps the M3 String payload behind its actual owner.
            lines.get(i).getChars(0, lines.get(i).length(), packed, offsets[i]);
        }
        long[] result = select0(packed, offsets, first, symbol, target, sibling,
                failure, outputs, requiredMask, emptyMask);
        if (result == null || result.length != ((count + 63) >>> 6)) {
            throw new IllegalStateException("invalid native Aho result geometry");
        }
        return result;
    }

    private static boolean absent(UnsatisfiedLinkError error) {
        String message = error.getMessage();
        return message != null && (message.startsWith("no m3unixaho in java.library.path:")
                || message.startsWith("Can't load library: "));
    }

    private static final class Native {
        static final boolean PRESENT = load();
        private static boolean load() {
            try {
                System.loadLibrary("m3unixaho");
            } catch (UnsatisfiedLinkError error) {
                if (absent(error)) return false;
                throw error;
            }
            if (abi0() != ABI) throw new UnsatisfiedLinkError("M3 native Aho ABI mismatch");
            return true;
        }
    }

    private static native int abi0();
    private static native long[] select0(char[] packed, int[] offsets,
            int[] heads, char[] symbols, int[] targets, int[] siblings,
            int[] failures, long[] outputs, long requiredMask, long emptyMask);
}
