/*******************************************************************************
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
package jdk.m3.collections;

import java.util.Objects;

/**
 * Fused primitive scan kernels for MAT columns.
 *
 * <p>No streams, iterators, boxing or intermediate row objects are used. Candidate bitmaps skip
 * non-candidate rows word-by-word; selected rows are processed with integer arithmetic only.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPrimitiveKernel.java}
 * blob {@code 93f4ecd85938ae76c7b798acec714bcd46137341} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatPrimitiveKernel {
    private M3MatPrimitiveKernel() { }

    public static M3MatBits selectFlags(
            M3MatFlagVector flags, long requireAll, long requireAny, long rejectAny) {
        Objects.requireNonNull(flags, "flags");
        return selectFlags(null, flags, requireAll, requireAny, rejectAny);
    }

    public static M3MatBits selectFlags(
            M3MatBits candidates,
            M3MatFlagVector flags,
            long requireAll,
            long requireAny,
            long rejectAny) {
        Objects.requireNonNull(flags, "flags");
        int size = flags.size();
        if (candidates != null && candidates.size() != size) {
            throw new IllegalArgumentException("candidate/flag universe mismatch");
        }
        int words = (size + 63) >>> 6;
        long[] selected = new long[words];
        for (int word = 0; word < words; word++) {
            long pending = candidates == null ? validWord(size, word) : candidates.word(word);
            long out = 0L;
            while (pending != 0L) {
                int bit = Long.numberOfTrailingZeros(pending);
                int row = (word << 6) + bit;
                long value = flags.getUnchecked(row);
                if (matches(value, requireAll, requireAny, rejectAny)) {
                    out |= 1L << bit;
                }
                pending &= pending - 1L;
            }
            selected[word] = out;
        }
        return M3MatBits.ofWords(size, selected);
    }

    public static int countFlags(
            M3MatBits candidates,
            M3MatFlagVector flags,
            long requireAll,
            long requireAny,
            long rejectAny) {
        Objects.requireNonNull(flags, "flags");
        int size = flags.size();
        if (candidates != null && candidates.size() != size) {
            throw new IllegalArgumentException("candidate/flag universe mismatch");
        }
        int count = 0;
        int words = (size + 63) >>> 6;
        for (int word = 0; word < words; word++) {
            long pending = candidates == null ? validWord(size, word) : candidates.word(word);
            while (pending != 0L) {
                int bit = Long.numberOfTrailingZeros(pending);
                int row = (word << 6) + bit;
                if (matches(flags.getUnchecked(row), requireAll, requireAny, rejectAny)) count++;
                pending &= pending - 1L;
            }
        }
        return count;
    }

    /**
     * One-pass flag + signed-int range filter. This replaces separate filter/filter/bitmap stages.
     */
    public static M3MatBits selectFlagsAndIntRange(
            M3MatBits candidates,
            M3MatFlagVector flags,
            M3MatIntVector values,
            long requireAll,
            long requireAny,
            long rejectAny,
            int minInclusive,
            int maxExclusive) {
        Objects.requireNonNull(flags, "flags");
        Objects.requireNonNull(values, "values");
        if (maxExclusive < minInclusive) {
            throw new IllegalArgumentException("invalid int range");
        }
        int size = flags.size();
        if (values.size() != size
                || candidates != null && candidates.size() != size) {
            throw new IllegalArgumentException("kernel universe mismatch");
        }

        int words = (size + 63) >>> 6;
        long[] selected = new long[words];
        for (int word = 0; word < words; word++) {
            long pending = candidates == null ? validWord(size, word) : candidates.word(word);
            long out = 0L;
            while (pending != 0L) {
                int bit = Long.numberOfTrailingZeros(pending);
                int row = (word << 6) + bit;
                long flag = flags.getUnchecked(row);
                int value = values.getUnchecked(row);
                if (matches(flag, requireAll, requireAny, rejectAny)
                        && value >= minInclusive
                        && value < maxExclusive) {
                    out |= 1L << bit;
                }
                pending &= pending - 1L;
            }
            selected[word] = out;
        }
        return M3MatBits.ofWords(size, selected);
    }

    public static long sumLong(M3MatLongVector values, M3MatBits selection) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(selection, "selection");
        if (values.size() != selection.size()) {
            throw new IllegalArgumentException("sum universe mismatch");
        }
        long sum = 0L;
        for (int row = selection.nextSetBit(0); row >= 0; row = selection.nextSetBit(row + 1)) {
            sum = Math.addExact(sum, values.getUnchecked(row));
        }
        return sum;
    }

    public static int[] gatherInt(M3MatIntVector values, M3MatBits selection) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(selection, "selection");
        if (values.size() != selection.size()) {
            throw new IllegalArgumentException("gather universe mismatch");
        }
        int[] result = new int[selection.cardinality()];
        int cursor = 0;
        for (int row = selection.nextSetBit(0); row >= 0; row = selection.nextSetBit(row + 1)) {
            result[cursor++] = values.getUnchecked(row);
        }
        return result;
    }

    public static int[] selectedRows(M3MatBits selection) {
        Objects.requireNonNull(selection, "selection");
        return selection.toIntArray();
    }

    private static boolean matches(
            long value, long requireAll, long requireAny, long rejectAny) {
        return (value & requireAll) == requireAll
                && (requireAny == 0L || (value & requireAny) != 0L)
                && (value & rejectAny) == 0L;
    }

    private static long validWord(int size, int word) {
        int words = (size + 63) >>> 6;
        if (word < 0 || word >= words) throw new IndexOutOfBoundsException("bitmap word");
        if (word + 1 < words || (size & 63) == 0) return -1L;
        return (1L << (size & 63)) - 1L;
    }
}
