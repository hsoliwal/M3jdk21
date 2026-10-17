// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Transient stable-selection mask over source-row indices. Packed primitive words
 * replace one byte-sized boolean for each element, without retaining Entries/Nodes.
 * No object payload is stored here; this does not replace generation-tagged M3
 * object-state slots or introduce a second collection owner.
 *
 * Writes from different keyed workers may share one word, so mark is atomic.
 * Compaction must start only after every marking worker has completed; it is
 * stable in captured source order and produces one exact-sized result array.
 * Not a concurrent clear/delete API; masks are single-use per terminal call.
 */
final class M3SelectionBits {
    private static final VarHandle WORD = MethodHandles.arrayElementVarHandle(long[].class);
    private final long[] words;
    private final int length;

    M3SelectionBits(int length) {
        if (length < 0) throw new IllegalArgumentException("negative row count");
        this.length = length;
        this.words = new long[(length >>> 6) + ((length & 63) == 0 ? 0 : 1)];
    }

    /** Atomic, idempotent set of exactly one captured source row. */
    void mark(int row) {
        Objects.checkIndex(row, length);
        WORD.getAndBitwiseOr(words, row >>> 6, 1L << (row & 63));
    }

    /** Visibility test for qualification and safe atom reuse. */
    boolean contains(int row) {
        Objects.checkIndex(row, length);
        return (((long) WORD.getVolatile(words, row >>> 6)) & (1L << (row & 63))) != 0L;
    }

    int count() {
        int n = 0;
        for (int i = 0; i < words.length; i++) {
            n += Long.bitCount((long) WORD.getVolatile(words, i));
        }
        return n; // every legal mark is a different bit among at most length rows
    }

    /** Stable compaction after writers quiesce, using only set-bit index scans. */
    Object[] compact(Object[] items) {
        Objects.requireNonNull(items);
        if (items.length != length) throw new IllegalArgumentException("source length changed");
        Object[] selected = new Object[count()];
        int cursor = 0;
        for (int word = 0; word < words.length; word++) {
            long bits = (long) WORD.getVolatile(words, word);
            while (bits != 0L) {
                int row = (word << 6) + Long.numberOfTrailingZeros(bits);
                selected[cursor++] = items[row];
                bits &= bits - 1L;
            }
        }
        if (cursor != selected.length) throw new IllegalStateException("mask changed during compaction");
        return selected;
    }
}
