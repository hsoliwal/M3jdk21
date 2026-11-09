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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Caller-owned bit decisions, one bit per source position rather than one object per entry.
 * Evaluate predicate decisions before native compaction/selection. Predicates run in ascending
 * absolute-index order and must not mutate source, mask or output. On failure the mask is
 * unspecified; no collection/output mutation is performed by this evaluator.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedAdmissionMask.java}
 * blob {@code 4b2505e558d32451608ea0e2de56082e9f9c8ac7} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedAdmissionMask {
    private PackedAdmissionMask() { }

    public static int wordsRequired(int count) {
        if (count < 0) { throw new IndexOutOfBoundsException("negative count"); }
        return (int) (((long) count + 63L) >>> 6);
    }

    public static int evaluate(int from, int to, IntPredicate admission, long[] decisions) {
        Objects.requireNonNull(admission, "admission");
        Objects.checkFromToIndex(from, to, Integer.MAX_VALUE);
        require(decisions, to - from);
        Arrays.fill(decisions, 0, wordsRequired(to - from), 0L);
        int accepted = 0;
        for (int i = from; i < to; i++) {
            if (admission.test(i)) {
                int bit = i - from;
                decisions[bit >>> 6] |= 1L << (bit & 63);
                accepted++;
            }
        }
        return accepted;
    }

    public static int count(long[] decisions, int count) {
        require(decisions, count);
        int whole = count >>> 6;
        int accepted = 0;
        for (int i = 0; i < whole; i++) { accepted += Long.bitCount(decisions[i]); }
        int tail = count & 63;
        if (tail != 0) { accepted += Long.bitCount(decisions[whole] & ((1L << tail) - 1L)); }
        return accepted;
    }

    static void require(long[] decisions, int count) {
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromIndexSize(0, wordsRequired(count), decisions.length);
    }

    static boolean selected(long[] decisions, int bit) {
        return (decisions[bit >>> 6] & (1L << (bit & 63))) != 0;
    }

    /** First selected relative position at/after from, or count. Caller validates the stable mask. */
    static int nextSelected(long[] decisions, int from, int count) {
        if (from >= count) { return count; }
        int word = from >>> 6;
        int words = wordsRequired(count);
        long bits = decisions[word] & (-1L << (from & 63));
        while (true) {
            if (bits != 0) {
                int selected = (word << 6) + Long.numberOfTrailingZeros(bits);
                return Math.min(selected, count);
            }
            if (++word == words) { return count; }
            bits = decisions[word];
        }
    }
}
