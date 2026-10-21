/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package jdk.internal.mindex;

import java.util.Objects;

/**
 * M3JDK-owned Object[] compaction over immutable deletion-mask bits.
 * No extra element array, per-element wrapper, or native Java reference transfer.
 *
 * <p>Delete bits are relative to {@code from}; zero bits are survivors.
 * ArrayList and Vector retain their original predicate, fail-fast, locking,
 * and reference-clearing contracts. System.arraycopy uses VM GC barriers.
 */
public final class M3CollectionMaskCompactor {
    private M3CollectionMaskCompactor() { }

    /** Returns the exclusive survivor end; never touches the discarded tail. */
    public static int compact(Object[] values, int from, int to, long[] deleted) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(deleted, "deleted");
        Objects.checkFromToIndex(from, to, values.length);
        int length = to - from;
        if (length == 0) return from;
        int words = ((length - 1) >>> 6) + 1;
        if (deleted.length < words) throw new IllegalArgumentException("mask too short");
        if (length < 512 || removalRuns(deleted, length) > Math.max(4, length >>> 7)) {
            int write = from;
            for (int read = 0; read < length; read++) {
                if ((deleted[read >>> 6] & (1L << (read & 63))) == 0) {
                    values[write++] = values[from + read];
                }
            }
            return write;
        }
        int read = 0;
        int write = from;
        while (read < length) {
            int nextSurvivor = next(deleted, read, length, false);
            if (nextSurvivor >= length) break;
            int nextDeletion = next(deleted, nextSurvivor, length, true);
            int span = nextDeletion - nextSurvivor;
            if (write != from + nextSurvivor) {
                System.arraycopy(values, from + nextSurvivor, values, write, span);
            }
            write += span;
            read = nextDeletion;
        }
        return write;
    }

    /** Count runs across word boundaries only until the scalar dispatch is decided. */
    private static int removalRuns(long[] deleted, int length) {
        int words = ((length - 1) >>> 6) + 1;
        int count = 0;
        int threshold = Math.max(4, length >>> 7);
        long preceding = 0L;
        for (int word = 0; word < words; word++) {
            long bits = deleted[word];
            int tail = length & 63;
            if (word == words - 1 && tail != 0) bits &= (1L << tail) - 1L;
            count += Long.bitCount(bits & ~((bits << 1) | preceding));
            // This count is used only for the threshold decision in compact().
            if (count > threshold) return count;
            preceding = bits >>> 63;
        }
        return count;
    }

    /** Find next removed/retained bit, skipping complete mask words. */
    private static int next(long[] bits, int from, int limit, boolean deleted) {
        int word = from >>> 6;
        long candidates = deleted ? bits[word] : ~bits[word];
        candidates &= -1L << (from & 63);
        for (;;) {
            if (candidates != 0L) {
                int index = (word << 6) + Long.numberOfTrailingZeros(candidates);
                return Math.min(index, limit);
            }
            word++;
            if (((long) word << 6) >= limit) return limit;
            candidates = deleted ? bits[word] : ~bits[word];
        }
    }
}
