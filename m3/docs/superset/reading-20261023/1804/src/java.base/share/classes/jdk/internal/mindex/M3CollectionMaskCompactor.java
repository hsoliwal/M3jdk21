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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        // Preserve the old hot scalar and long-run paths.  Word-based copying
        // is admitted only for sparse (or nearly all) deletion states, after the
        // callback phase has already prepared a complete immutable mask.
        if (length < 512) return compactScalar(values, from, length, deleted);
        long statistics = maskStatistics(deleted, length);
        int runs = (int) (statistics >>> 32);
        int removed = (int) statistics;
        if (runs <= Math.max(4, length >>> 7)) {
            return compactRuns(values, from, length, deleted);
=======
        if (length < 512 || removalRuns(deleted, length) > Math.max(4, length >>> 7)) {
            // Prefix before the first deletion is already at its final physical slots.
            // Do not issue redundant reference stores or GC barriers for those entries.
            int firstRemoved = next(deleted, 0, length, true);
            if (firstRemoved == length) return to;
            int write = from + firstRemoved;
            for (int read = firstRemoved + 1; read < length; read++) {
                if ((deleted[read >>> 6] & (1L << (read & 63))) == 0) {
                    values[write++] = values[from + read];
                }
            }
            return write;
>>>>>>> c47360525fb384da9aa45adfbe5c38812efe617e
        }
        if (removed <= length / 8 || removed >= length - length / 64) {
            return compactWords(values, from, length, deleted);
        }
        return compactScalar(values, from, length, deleted);
    }

    private static int compactScalar(Object[] values, int from, int length, long[] deleted) {
        int write = from;
        for (int read = 0; read < length; read++) {
            if ((deleted[read >>> 6] & (1L << (read & 63))) == 0) {
                values[write++] = values[from + read];
            }
        }
        return write;
    }

    private static int compactRuns(Object[] values, int from, int length, long[] deleted) {
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

    /** 64-bit rejection-word compaction; no extra Java payload array or JNI calls. */
    private static int compactWords(Object[] values, int from, int length, long[] deleted) {
        int write = from;
        for (int base = 0; base < length; ) {
            int chunk = Math.min(64, length - base);
            long full = -1L >>> (64 - chunk);
            long mask = deleted[base >>> 6] & full;
            int start = from + base;
            if (mask == 0L) {
                System.arraycopy(values, start, values, write, chunk);
                write += chunk;
            } else if (mask != full && Long.bitCount(mask) <= 4) {
                int consumed = 0;
                do {
                    int lost = Long.numberOfTrailingZeros(mask);
                    int run = lost - consumed;
                    if (run != 0) {
                        System.arraycopy(values, start + consumed, values, write, run);
                        write += run;
                    }
                    consumed = lost + 1;
                    mask &= mask - 1;
                } while (mask != 0L);
                int remaining = chunk - consumed;
                if (remaining != 0) {
                    System.arraycopy(values, start + consumed, values, write, remaining);
                    write += remaining;
                }
            } else {
                for (int bit = 0; bit < chunk; bit++) {
                    if ((mask & (1L << bit)) == 0L) {
                        values[write++] = values[start + bit];
                    }
                }
            }
            base += chunk;
        }
        return write;
    }

    /** Count deletion runs, including ones crossing the 64-bit boundary. */
    private static long maskStatistics(long[] deleted, int length) {
        int words = ((length - 1) >>> 6) + 1;
        int count = 0;
        int removed = 0;
        long preceding = 0L;
        for (int word = 0; word < words; word++) {
            long bits = deleted[word];
            int tail = length & 63;
            if (word == words - 1 && tail != 0) bits &= (1L << tail) - 1L;
            count += Long.bitCount(bits & ~((bits << 1) | preceding));
            removed += Long.bitCount(bits);
            preceding = bits >>> 63;
        }
        return ((long) count << 32) | Integer.toUnsignedLong(removed);
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
