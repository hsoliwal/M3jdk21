// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Test-only, immutable, sparse UTF-16 Aho-Corasick search atom. All offsets,
 * transitions, failure links and terminal flags are stored in primitive arrays.
 * A source word is never retained after compilation and no per-node/Entry object
 * survives the compilation pass. Match indexes are UTF-16 code-unit positions
 * equivalent to {@code String.substring(from, to).indexOf(literal) + from}.
 *
 * <p>This is not a substitute for arbitrary java.util.regex Pattern/Matcher:
 * malformed UTF-16, literal quote behavior, matcher regions/bounds, flags,
 * captures and zero-width patterns have independent contracts. Only a qualified
 * first-index *literal* optimization may consider this candidate.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-sparse-java-first-completion-20261026/M3SparseUtf16LiteralPlan.java.txt blob baa2148760ba; recipe m3/ports/precompute/recipe/utf16-stitch-20261106/relocate.py v3
public final class M3SparseUtf16LiteralPlan {
    private static final int MAX_STATES = 1
            + M3LiteralBatchCandidate.MAX_NEEDLES * M3LiteralBatchCandidate.MAX_NEEDLE_UNITS;
    private static final int MAX_REGIONS = 64;
    private final int[] offsets;
    private final char[] labels;
    private final int[] targets;
    private final int[] failures;
    private final int[] terminals;
    private final int[] lengths;

    private M3SparseUtf16LiteralPlan(int[] offsets, char[] labels, int[] targets,
            int[] failures, int[] terminals, int[] lengths) {
        this.offsets = offsets;
        this.labels = labels;
        this.targets = targets;
        this.failures = failures;
        this.terminals = terminals;
        this.lengths = lengths;
    }

    /** Accepts exact nonempty UTF-16 literal sequences; nonzero regex flags refuse. */
    public static M3SparseUtf16LiteralPlan compile(List<String> needles, int regexFlags) {
        Objects.requireNonNull(needles, "needles");
        if (regexFlags != 0) {
            throw new IllegalArgumentException("invalid literal-plan size or regex flags");
        }
        // Admit one stable source geometry before any native or trie metadata.
        int count = needles.size();
        if (count == 0 || count > M3LiteralBatchCandidate.MAX_NEEDLES) {
            throw new IllegalArgumentException("invalid literal-plan size or regex flags");
        }
        int total = 0;
        // Seal source values at the start of compilation. A computed List can
        // return different entries across separate passes; the immutable trie
        // and its lengths must always describe the same captured literals.
        String[] source = new String[count];
        int[] lengths = new int[count];
        for (int i = 0; i < count; i++) {
            String needle = Objects.requireNonNull(needles.get(i), "needle");
            source[i] = needle;
            int length = needle.length();
            if (length == 0 || length > M3LiteralBatchCandidate.MAX_NEEDLE_UNITS) {
                throw new IllegalArgumentException("literal UTF-16 length not in [1, 63]");
            }
            total = Math.addExact(total, length);
            lengths[i] = length;
        }
        int[] heads = new int[total + 1];
        Arrays.fill(heads, -1);
        char[] unsortedLabels = new char[total];
        int[] unsortedTargets = new int[total];
        int[] siblings = new int[total];
        int[] terminals = new int[total + 1];
        int states = 1, edges = 0;
        // File-local serial atomization: construct a finite, shared-prefix trie.
        for (int i = 0; i < count; i++) {
            String word = source[i];
            int state = 0;
            for (int j = 0; j < word.length(); j++) {
                char c = word.charAt(j);
                int found = findRaw(heads, siblings, unsortedLabels, state, c);
                if (found >= 0) {
                    state = unsortedTargets[found];
                } else {
                    int nextState = states++;
                    unsortedLabels[edges] = c;
                    unsortedTargets[edges] = nextState;
                    siblings[edges] = heads[state];
                    heads[state] = edges++;
                    state = nextState;
                }
            }
            terminals[state] |= 1 << i;
        }
        if (states > MAX_STATES || edges >= MAX_STATES) {
            throw new IllegalStateException("UTF-16 trie exceeded its geometry budget");
        }
        int[] failure = new int[states];
        int[] queue = new int[states];
        int first = 0, end = 0;
        for (int edge = heads[0]; edge >= 0; edge = siblings[edge]) {
            queue[end++] = unsortedTargets[edge];
        }
        while (first < end) {
            int state = queue[first++];
            for (int edge = heads[state]; edge >= 0; edge = siblings[edge]) {
                int child = unsortedTargets[edge];
                char symbol = unsortedLabels[edge];
                int fallback = failure[state];
                int corresponding = findRaw(heads, siblings, unsortedLabels,
                        fallback, symbol);
                while (corresponding < 0 && fallback != 0) {
                    fallback = failure[fallback];
                    corresponding = findRaw(heads, siblings, unsortedLabels,
                            fallback, symbol);
                }
                failure[child] = corresponding < 0 ? 0 : unsortedTargets[corresponding];
                terminals[child] |= terminals[failure[child]];
                queue[end++] = child;
            }
        }
        if (end != states - 1) throw new IllegalStateException("unreachable trie state");
        // Canonical CSR metadata makes every runtime lookup allocation-free.
        int[] offsets = new int[states + 1];
        for (int state = 0; state < states; state++) {
            for (int edge = heads[state]; edge >= 0; edge = siblings[edge]) {
                offsets[state + 1]++;
            }
        }
        for (int state = 1; state <= states; state++) offsets[state] += offsets[state - 1];
        char[] labels = new char[edges];
        int[] targets = new int[edges];
        int[] cursor = offsets.clone();
        for (int state = 0; state < states; state++) {
            for (int edge = heads[state]; edge >= 0; edge = siblings[edge]) {
                int at = cursor[state]++;
                labels[at] = unsortedLabels[edge];
                targets[at] = unsortedTargets[edge];
            }
            // Small per-state insertion sort, no boxed objects and no second graph.
            for (int i = offsets[state] + 1; i < offsets[state + 1]; i++) {
                char label = labels[i];
                int target = targets[i];
                int j = i - 1;
                while (j >= offsets[state] && labels[j] > label) {
                    labels[j + 1] = labels[j];
                    targets[j + 1] = targets[j];
                    j--;
                }
                labels[j + 1] = label;
                targets[j + 1] = target;
            }
        }
        return new M3SparseUtf16LiteralPlan(offsets, labels, targets, failure,
                Arrays.copyOf(terminals, states), lengths);
    }

    private static int findRaw(int[] head, int[] next, char[] symbols,
            int state, char symbol) {
        for (int e = head[state]; e >= 0; e = next[e]) {
            if (symbols[e] == symbol) return e;
        }
        return -1;
    }

    private int direct(int state, char symbol) {
        int low = offsets[state], high = offsets[state + 1];
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (labels[mid] < symbol) low = mid + 1;
            else high = mid;
        }
        return low < offsets[state + 1] && labels[low] == symbol
                ? targets[low] : -1;
    }

    private int advance(int state, char symbol) {
        while (true) {
            int target = direct(state, symbol);
            if (target >= 0) return target;
            if (state == 0) return 0;
            state = failures[state];
        }
    }

    private void checkRegion(char[] input, int from, int to) {
        Objects.requireNonNull(input, "input");
        Objects.checkFromToIndex(from, to, input.length);
        if (to - from > M3LiteralBatchCandidate.MAX_REGION_UNITS) {
            throw new IllegalArgumentException("sparse UTF-16 region exceeds budget");
        }
    }

    private void scan(char[] text, int from, int to, int[] output, int offset) {
        Arrays.fill(output, offset, offset + lengths.length, -1);
        int state = 0;
        int unresolved = lengths.length;
        for (int at = from; at < to; at++) {
            state = advance(state, text[at]);
            int mask = terminals[state];
            while (mask != 0) {
                int bit = Integer.numberOfTrailingZeros(mask);
                if (output[offset + bit] < 0) {
                    output[offset + bit] = at - lengths[bit] + 1;
                    if (--unresolved == 0) return;
                }
                mask &= mask - 1;
            }
        }
    }

    /** First UTF-16 positions in [from,to), independent of source mutations. */
    public int[] firstJava(char[] input, int from, int to) {
        checkRegion(input, from, to);
        int[] result = new int[lengths.length];
        scan(input, from, to, result, 0);
        return result;
    }

    /** Flat [region][needle] row-major selected positions, all geometry preflighted. */
    public int[] firstJavaBatch(char[] input, int[] from, int[] to) {
        validateBatch(input, from, to);
        int[] result = new int[Math.multiplyExact(from.length, lengths.length)];
        for (int i = 0; i < from.length; i++) {
            scan(input, from[i], to[i], result, i * lengths.length);
        }
        return result;
    }

    private void validateBatch(char[] input, int[] from, int[] to) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.length == 0 || from.length > MAX_REGIONS || from.length != to.length) {
            throw new IllegalArgumentException("invalid Unicode region batch");
        }
        for (int i = 0; i < from.length; i++) checkRegion(input, from[i], to[i]);
    }

    /** Explicit test-only C17 JNI oracle; no fallback or source pointer retention. */
    public int[] firstNativeBatch(char[] input, int[] from, int[] to) {
        validateBatch(input, from, to);
        if (!NativeLibrary.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3_utf16_sparse_probe JNI library unavailable");
        }
        int[] values = nativeBatch(input, from, to, offsets, labels,
                targets, failures, terminals, lengths);
        if (values == null || values.length != from.length * lengths.length) {
            throw new IllegalStateException("invalid sparse UTF-16 JNI output shape");
        }
        for (int region = 0; region < from.length; region++) {
            for (int p = 0; p < lengths.length; p++) {
                int at = values[region * lengths.length + p];
                if (at < -1 || at >= 0
                        && (at < from[region] || at > to[region] - lengths[p])) {
                    throw new IllegalStateException("invalid native Unicode literal first-index");
                }
            }
        }
        return values;
    }

    /** Native ABI is limited to this candidate, never exported to a public JDK. */
    static native int[] nativeBatch(char[] input, int[] from, int[] to,
            int[] offsets, char[] labels, int[] targets, int[] failures,
            int[] terminals, int[] lengths);

    public static boolean nativeAvailable() { return NativeLibrary.AVAILABLE; }

    private static final class NativeLibrary {
        static final boolean AVAILABLE = load();
        static boolean load() {
            try {
                System.loadLibrary("m3_utf16_sparse_probe");
                return true;
            } catch (UnsatisfiedLinkError absent) {
                return false;
            }
        }
    }

    public int patternCount() { return lengths.length; }
    public int stateCount() { return failures.length; }
    public int transitionCells() { return labels.length; }
    public long primitiveBytes() {
        return 4L * (offsets.length + targets.length + failures.length
                + terminals.length + lengths.length) + 2L * labels.length;
    }
}
