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
 * Linear palindrome-radius index over an int sequence.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/IntManacher.java}
 * blob {@code 203530f60318b9f66248cd2ce7e7f6f3095d3621} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntManacher {
    private final int length;
    private final int[] odd;
    private final int[] even;

    public M3IntManacher(int[] values) {
        Objects.requireNonNull(values, "values");
        length = values.length;
        odd = new int[length];
        even = new int[length];

        for (int i = 0, left = 0, right = -1; i < length; i++) {
            int radius = i > right ? 1 : Math.min(odd[left + right - i], right - i + 1);
            while (i - radius >= 0
                    && i + radius < length
                    && values[i - radius] == values[i + radius]) {
                radius++;
            }
            odd[i] = radius;
            if (i + radius - 1 > right) {
                left = i - radius + 1;
                right = i + radius - 1;
            }
        }

        for (int i = 0, left = 0, right = -1; i < length; i++) {
            int radius = i > right ? 0 : Math.min(even[left + right - i + 1], right - i + 1);
            while (i - radius - 1 >= 0
                    && i + radius < length
                    && values[i - radius - 1] == values[i + radius]) {
                radius++;
            }
            even[i] = radius;
            if (i + radius - 1 > right) {
                left = i - radius;
                right = i + radius - 1;
            }
        }
    }

    public int length() { return length; }
    public int oddRadius(int center) { return odd[center]; }
    public int evenRadius(int rightCenter) { return even[rightCenter]; }
    public int[] oddRadii() { return odd.clone(); }
    public int[] evenRadii() { return even.clone(); }

    public int longestPalindromeLength() {
        int best = 0;
        for (int value : odd) best = Math.max(best, (value << 1) - 1);
        for (int value : even) best = Math.max(best, value << 1);
        return best;
    }

    public boolean isPalindrome(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > length) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
        int span = toExclusive - fromInclusive;
        if (span <= 1) return true;
        if ((span & 1) != 0) {
            int center = (fromInclusive + toExclusive - 1) >>> 1;
            return odd[center] >= (span + 1) >>> 1;
        }
        int rightCenter = (fromInclusive + toExclusive) >>> 1;
        return even[rightCenter] >= (span >>> 1);
    }

    public long payloadBytes() {
        return (long) (odd.length + even.length) * Integer.BYTES;
    }
}
