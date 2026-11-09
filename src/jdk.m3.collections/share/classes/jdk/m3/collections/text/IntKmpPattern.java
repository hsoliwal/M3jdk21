/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.text;

import java.util.Objects;

/**
 * Precompiled Knuth-Morris-Pratt matcher over integer symbols.
 *
 * <p>The pattern and failure function are retained in two int[] lanes. Matching
 * allocates no per-character state and accepts token ids directly.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntKmpPattern.java}
 * blob {@code e9b8518ea17545225c05b5ac054eda43dfa843ca} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class IntKmpPattern {

    @FunctionalInterface
    public interface MatchConsumer {
        void accept(int startInclusive, int endExclusive);
    }

    private final int[] pattern;
    private final int[] failure;

    public IntKmpPattern(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        this.pattern = pattern.clone();
        this.failure = buildFailure(this.pattern);
    }

    public int length() {
        return pattern.length;
    }

    public boolean isEmpty() {
        return pattern.length == 0;
    }

    public long payloadBytes() {
        return (long) (pattern.length + failure.length) * Integer.BYTES;
    }

    public int symbolAt(int index) {
        return pattern[index];
    }

    public int indexOf(int[] text) {
        return indexOf(text, 0);
    }

    public int indexOf(int[] text, int fromInclusive) {
        Objects.requireNonNull(text, "text");
        if (fromInclusive < 0 || fromInclusive > text.length) {
            throw new IndexOutOfBoundsException(fromInclusive);
        }
        if (pattern.length == 0) return fromInclusive;
        if (pattern.length > text.length - fromInclusive) return -1;
        // The precomputed one-symbol shape needs no failure-function machine.
        if (pattern.length == 1) {
            int wanted = pattern[0];
            for (int i = fromInclusive; i < text.length; i++) {
                if (text[i] == wanted) return i;
            }
            return -1;
        }

        int matched = 0;
        for (int i = fromInclusive; i < text.length; i++) {
            while (matched > 0 && text[i] != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (text[i] == pattern[matched]) matched++;
            if (matched == pattern.length) return i - pattern.length + 1;
        }
        return -1;
    }

    /** Counts overlapping matches. Empty pattern matches at every boundary. */
    public int count(int[] text) {
        Objects.requireNonNull(text, "text");
        if (pattern.length == 0) return Math.addExact(text.length, 1);
        if (pattern.length > text.length) return 0;
        if (pattern.length == 1) {
            int wanted = pattern[0];
            int matches = 0;
            for (int value : text) if (value == wanted) matches++;
            return matches;
        }

        int matches = 0;
        int matched = 0;
        for (int value : text) {
            while (matched > 0 && value != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (value == pattern[matched]) matched++;
            if (matched == pattern.length) {
                matches++;
                matched = failure[matched - 1];
            }
        }
        return matches;
    }

    /** Emits overlapping matches in ascending start order. */
    public void scan(int[] text, MatchConsumer consumer) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(consumer, "consumer");
        if (pattern.length == 0) {
            emitEmptyBoundaries(0, text.length, consumer);
            return;
        }
        if (pattern.length > text.length) return;
        if (pattern.length == 1) {
            int wanted = pattern[0];
            for (int i = 0; i < text.length; i++) {
                if (text[i] == wanted) consumer.accept(i, i + 1);
            }
            return;
        }

        int matched = 0;
        for (int i = 0; i < text.length; i++) {
            while (matched > 0 && text[i] != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (text[i] == pattern[matched]) matched++;
            if (matched == pattern.length) {
                int end = i + 1;
                consumer.accept(end - pattern.length, end);
                matched = failure[matched - 1];
            }
        }
    }

    /**
     * Emits all inclusive empty-pattern boundaries, stopping before int rollover.
     * Coordinates are caller-validated for the public full-array scan; this
     * internal entry allows high-index tests without allocating a huge array.
     */
    static void emitEmptyBoundaries(int fromInclusive, int toInclusive,
            MatchConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (fromInclusive < 0 || toInclusive < fromInclusive) {
            throw new IndexOutOfBoundsException("empty-pattern range");
        }
        for (int index = fromInclusive; ; index++) {
            consumer.accept(index, index);
            if (index == toInclusive) return;
        }
    }

    public int[] copyPattern() {
        return pattern.clone();
    }

    public int[] copyFailureFunction() {
        return failure.clone();
    }

    private static int[] buildFailure(int[] pattern) {
        int[] failure = new int[pattern.length];
        int matched = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (matched > 0 && pattern[i] != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (pattern[i] == pattern[matched]) matched++;
            failure[i] = matched;
        }
        return failure;
    }
}
