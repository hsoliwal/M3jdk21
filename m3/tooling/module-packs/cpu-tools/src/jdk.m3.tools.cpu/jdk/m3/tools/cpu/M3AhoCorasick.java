// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Multiple exact UTF-16 literal searches with a primitive-trie/Aho-Corasick automaton. */
public final class M3AhoCorasick {
    private static final int MAX_PATTERNS = 4096;
    private static final int MAX_CODE_UNITS = 65_536;
    private static final int MAX_TEXT_UNITS = 8_000_000;

    private M3AhoCorasick() { }

    /** Immutable shared machine; line/text owner is never retained or flattened. */
    public static final class Compiled {
        private final int count;
        private final int[] head, edgeNext, edgeTarget, fail, outputHead, outputNext;
        private final char[] edgeChar;
        private final int[] outputId, width, outputSuffix;
        private Compiled(int count, int[] head, int[] edgeNext, int[] edgeTarget,
                         char[] edgeChar, int[] fail, int[] outputHead, int[] outputNext,
                         int[] outputId, int[] width, int[] outputSuffix) {
            this.count = count;
            this.head = head;
            this.edgeNext = edgeNext;
            this.edgeTarget = edgeTarget;
            this.edgeChar = edgeChar;
            this.fail = fail;
            this.outputHead = outputHead;
            this.outputNext = outputNext;
            this.outputId = outputId;
            this.width = width;
            this.outputSuffix = outputSuffix;
        }

        /** Return first UTF-16 code-unit index per query (String.indexOf oracle), -1 if absent. */
        public int[] firstOffsets(CharSequence text) {
            Objects.requireNonNull(text, "text");
            if (text.length() > MAX_TEXT_UNITS) throw new IllegalArgumentException("text budget");
            int[] first = new int[count]; Arrays.fill(first, -1);
            int pending = count;
            for (int p = 0; p < count; p++) if (width[p] == 0) {
                first[p] = 0;
                pending--;
            }
            int state = 0;
            for (int i = 0; i < text.length() && pending > 0; i++) {
                char value = text.charAt(i);
                int next = transition(head, edgeNext, edgeChar, edgeTarget, state, value);
                while (next < 0 && state != 0) {
                    state = fail[state];
                    next = transition(head, edgeNext, edgeChar, edgeTarget, state, value);
                }
                state = next < 0 ? 0 : next;
                int s = outputHead[state] >= 0 ? state : outputSuffix[state];
                while (s > 0) {
                    for (int out = outputHead[s]; out >= 0; out = outputNext[out]) {
                        int query = outputId[out];
                        if (first[query] < 0) {
                            first[query] = i + 1 - width[query];
                            pending--;
                        }
                    }
                    s = outputSuffix[s];
                }
            }
            return first;
        }
    }

    /** Pattern may be a nonflattenable M3StringPiece-like CharSequence. */
    public static Compiled compile(CharSequence[] patterns) {
        Objects.requireNonNull(patterns, "patterns");
        if (patterns.length > MAX_PATTERNS) throw new IllegalArgumentException("pattern count budget");
        int total = 0;
        for (CharSequence pattern : patterns) {
            Objects.requireNonNull(pattern, "pattern");
            total = Math.addExact(total, pattern.length());
        }
        if (total > MAX_CODE_UNITS) throw new IllegalArgumentException("pattern size budget");
        int[] head = new int[total + 1], target = new int[total], next = new int[total],
                fail = new int[total + 1], outputHead = new int[total + 1],
                outputNext = new int[patterns.length], outputId = new int[patterns.length],
                widths = new int[patterns.length], outputSuffix = new int[total + 1];
        char[] code = new char[total];
        Arrays.fill(head, -1); Arrays.fill(outputHead, -1);
        int nodes = 1, edges = 0, emitted = 0;
        for (int p = 0; p < patterns.length; p++) {
            CharSequence pattern = patterns[p];
            widths[p] = pattern.length();
            if (pattern.length() == 0) continue;
            int state = 0;
            for (int i = 0; i < pattern.length(); i++) {
                char ch = pattern.charAt(i);
                int t = transition(head, next, code, target, state, ch);
                if (t < 0) {
                    t = nodes++;
                    code[edges] = ch;
                    target[edges] = t;
                    next[edges] = head[state];
                    head[state] = edges++;
                }
                state = t;
            }
            outputId[emitted] = p;
            outputNext[emitted] = outputHead[state];
            outputHead[state] = emitted++;
        }
        int[] queue = new int[nodes];
        int front = 0, back = 0;
        for (int e = head[0]; e >= 0; e = next[e]) queue[back++] = target[e];
        while (front < back) {
            int state = queue[front++];
            for (int e = head[state]; e >= 0; e = next[e]) {
                int f = fail[state];
                int t = transition(head, next, code, target, f, code[e]);
                while (t < 0 && f > 0) {
                    f = fail[f];
                    t = transition(head, next, code, target, f, code[e]);
                }
                fail[target[e]] = t < 0 ? 0 : t;
                int parent = fail[target[e]];
                outputSuffix[target[e]] = outputHead[parent] >= 0 ? parent : outputSuffix[parent];
                queue[back++] = target[e];
            }
        }
        return new Compiled(patterns.length, Arrays.copyOf(head, nodes),
                Arrays.copyOf(next, edges), Arrays.copyOf(target, edges),
                Arrays.copyOf(code, edges), Arrays.copyOf(fail, nodes),
                Arrays.copyOf(outputHead, nodes), Arrays.copyOf(outputNext, emitted),
                Arrays.copyOf(outputId, emitted), widths, Arrays.copyOf(outputSuffix, nodes));
    }

    private static int transition(int[] head, int[] next, char[] code, int[] target,
                                  int state, char value) {
        for (int e = head[state]; e >= 0; e = next[e])
            if (code[e] == value) return target[e];
        return -1;
    }
}
