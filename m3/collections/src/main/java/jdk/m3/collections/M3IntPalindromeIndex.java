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
 * Linear Manacher precompute over exact integer symbols; O(1) half-open palindrome queries.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntPalindromeIndex.java}
 * blob {@code e8583305e1412ccd225032c2103ad396090bcf43} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntPalindromeIndex {
    private final int[] odd;
    private final int[] even;
    private final int longestStart;
    private final int longestLength;
    private final long count;

    public M3IntPalindromeIndex(int[] symbols) {
        int[] values = Objects.requireNonNull(symbols, "symbols").clone();
        int n = values.length;
        odd = new int[n]; even = new int[n];
        for (int i = 0, left = 0, right = -1; i < n; i++) {
            int radius = i > right ? 1 : Math.min(odd[left + (right - i)], right - i + 1);
            while (radius <= i && radius < n - i && values[i - radius] == values[i + radius]) { radius++; }
            odd[i] = radius;
            if (i + radius - 1 > right) { left = i - radius + 1; right = i + radius - 1; }
        }
        for (int i = 0, left = 0, right = -1; i < n; i++) {
            int radius = i > right ? 0 : Math.min(even[left + (right - i) + 1], right - i + 1);
            while (radius < i && radius < n - i && values[i - radius - 1] == values[i + radius]) { radius++; }
            even[i] = radius;
            if (i + radius - 1 > right) { left = i - radius; right = i + radius - 1; }
        }
        int bestStart = 0, bestLength = 0;
        long total = 0;
        for (int i = 0; i < n; i++) {
            total += odd[i] + (long) even[i];
            int length = odd[i] + (odd[i] - 1), start = i - odd[i] + 1;
            if (length > bestLength || length == bestLength && start < bestStart) {
                bestStart = start; bestLength = length;
            }
            length = even[i] * 2; start = i - even[i];
            if (length > bestLength || length == bestLength && start < bestStart) {
                bestStart = start; bestLength = length;
            }
        }
        longestStart = bestStart; longestLength = bestLength; count = total;
    }

    public int size() { return odd.length; }
    public int oddRadius(int center) { return odd[Objects.checkIndex(center, size())]; }
    public int evenRadius(int rightCenter) { return even[Objects.checkIndex(rightCenter, size())]; }
    public int longestStart() { return longestStart; }
    public int longestLength() { return longestLength; }
    /** Number of nonempty palindromic occurrences (not distinct spellings). */
    public long count() { return count; }
    public boolean isPalindrome(int from, int to) {
        Objects.checkFromToIndex(from, to, size());
        int length = to - from;
        if (length == 0) { return true; }
        int center = from + length / 2;
        return (length & 1) == 0 ? even[center] >= length / 2 : odd[center] > length / 2;
    }
}
