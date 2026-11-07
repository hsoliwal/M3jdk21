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
package jdk.internal.m3.collections;

import java.util.Objects;
import jdk.m3.collections.M3IntSuffixArray;

/**
 * Distinct-substring order statistics over the canonical signed-int suffix-array snapshot.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/IntSubstringOrderIndex.java}
 * blob {@code ee01290806d9781ae28e7b4e7c75ad310b483ada} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntSubstringOrderIndex {
    private final M3IntSuffixArray suffixes;
    private final long[] cumulative;

    public M3IntSubstringOrderIndex(int[] tokens) { this(new M3IntSuffixArray(tokens)); }

    /** Reuses the same immutable owner; only the cumulative-count lane is added. */
    public M3IntSubstringOrderIndex(M3IntSuffixArray suffixes) {
        this.suffixes = Objects.requireNonNull(suffixes, "suffixes");
        cumulative = new long[Math.addExact(suffixes.length(), 1)];
        for (int rank = 0; rank < suffixes.length(); rank++) {
            cumulative[rank + 1] = cumulative[rank]
                    + suffixes.length() - suffixes.suffixAtRank(rank) - suffixes.lcpAtRank(rank);
        }
    }

    public M3IntSuffixArray suffixArray() { return suffixes; }
    public int length() { return suffixes.length(); }
    public long distinctCount() { return cumulative[length()]; }

    /** Lower-bound rank among distinct nonempty spellings; also defined for absent patterns. */
    public long rankOf(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length == 0) { return 0; }
        int rank = boundary(pattern, false);
        if (rank == length()) { return distinctCount(); }
        int start = suffixes.suffixAtRank(rank), matched = 0;
        while (matched < pattern.length && matched < length() - start
                && suffixes.tokenAt(start + matched) == pattern[matched]) { matched++; }
        int shorter = matched == pattern.length ? matched - 1 : matched;
        return cumulative[rank] + Math.max(0, shorter - suffixes.lcpAtRank(rank));
    }

    /** Distinct nonempty spellings beginning with the pattern; empty pattern counts every spelling. */
    public long countWithPrefix(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length == 0) { return distinctCount(); }
        int first = boundary(pattern, false), after = boundary(pattern, true);
        if (first == after) { return 0; }
        return cumulative[after] - cumulative[first] - (pattern.length - 1L - suffixes.lcpAtRank(first));
    }

    /** Overlapping source positions, rather than distinct spellings; empty occurs at n+1 boundaries. */
    public long occurrenceCount(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return pattern.length == 0 ? (long) length() + 1 : boundary(pattern, true) - boundary(pattern, false);
    }
    public int selectedLength(long ordinal) {
        int rank = selectedRank(ordinal);
        return suffixes.lcpAtRank(rank) + 1 + (int) (ordinal - cumulative[rank]);
    }
    /** A valid source representative, not necessarily the earliest occurrence of that spelling. */
    public int selectedStart(long ordinal) { return suffixes.suffixAtRank(selectedRank(ordinal)); }

    /** Writes one selected spelling after complete bounds validation; returns its length. */
    public int select(long ordinal, int[] output, int offset) {
        Objects.requireNonNull(output, "output");
        int rank = selectedRank(ordinal);
        int count = suffixes.lcpAtRank(rank) + 1 + (int) (ordinal - cumulative[rank]);
        Objects.checkFromIndexSize(offset, count, output.length);
        int start = suffixes.suffixAtRank(rank);
        for (int i = 0; i < count; i++) { output[offset + i] = suffixes.tokenAt(start + i); }
        return count;
    }

    private int selectedRank(long ordinal) {
        Objects.checkIndex(ordinal, distinctCount());
        int low = 0, high = length();
        while (low < high) {
            int middle = low + (high - low) / 2;
            if (cumulative[middle + 1] <= ordinal) { low = middle + 1; } else { high = middle; }
        }
        return low;
    }
    private int boundary(int[] pattern, boolean afterMatches) {
        int low = 0, high = length();
        while (low < high) {
            int middle = low + (high - low) / 2, start = suffixes.suffixAtRank(middle);
            int available = length() - start, common = Math.min(available, pattern.length), compared = 0;
            for (int i = 0; i < common; i++) {
                compared = Integer.compare(suffixes.tokenAt(start + i), pattern[i]);
                if (compared != 0) { break; }
            }
            if (compared == 0 && pattern.length > available) { compared = -1; }
            if (compared < 0 || afterMatches && compared == 0) { low = middle + 1; } else { high = middle; }
        }
        return low;
    }
}
