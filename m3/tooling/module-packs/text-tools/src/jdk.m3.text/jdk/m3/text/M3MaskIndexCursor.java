// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Allocation-free traversal of an M3 native selection bit-lane.
 * The bit number is the source row index; callbacks observe increasing original
 * coordinates. No JNI pointer, String, boxed Integer or per-row object is retained.
 * This class does not authorize mutation of its caller-owned arrays.
 */
final class M3MaskIndexCursor {
    private M3MaskIndexCursor() { }

    /** Enumerate set bits only; empty and sparse selections require no row scan. */
    static void forEach(long[] selected, int rowCount, IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        validate(selected, rowCount);
        for (int wordIndex = 0; wordIndex < selected.length; wordIndex++) {
            long word = selected[wordIndex];
            while (word != 0L) {
                int row = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                consumer.accept(row);
                word &= word - 1;
            }
        }
    }

    /** Fail closed if JNI manufactures a row not present in the incoming mask. */
    static void requireSubset(long[] candidate, long[] prior, int rowCount) {
        validate(prior, rowCount);
        validate(candidate, rowCount);
        for (int i = 0; i < prior.length; i++) {
            if ((candidate[i] & ~prior[i]) != 0L)
                throw new IllegalStateException("native selection includes unselected row");
        }
    }

    private static void validate(long[] words, int rowCount) {
        Objects.requireNonNull(words, "selection mask");
        if (rowCount < 0 || words.length != ((long) rowCount + 63L) / 64L)
            throw new IllegalArgumentException("invalid selection mask geometry");
        int remainder = rowCount & 63;
        if (remainder != 0 && (words[words.length - 1] >>> remainder) != 0L)
            throw new IllegalStateException("selection contains bits beyond final row");
    }
}
