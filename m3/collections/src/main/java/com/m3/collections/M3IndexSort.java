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
package com.m3.collections;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.IntFunction;

/** Stable, primitive-index merge sort; never sorts or copies its payload owner. */
final class M3IndexSort {
    private M3IndexSort() { }

    @SuppressWarnings("unchecked")
    static <E> void stable(int[] positions, IntFunction<? extends E> valueAt,
                           Comparator<? super E> comparator) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(valueAt, "valueAt");
        int length = positions.length;
        if (length < 2) return;
        int[] scratch = new int[length];
        int[] source = positions;
        int[] destination = scratch;
        // long arithmetic excludes overflow for even the largest Java arrays.
        for (long width = 1; width < length; width <<= 1) {
            for (long base = 0; base < length; base += width * 2) {
                int mid = (int) Math.min(length, base + width);
                int high = (int) Math.min(length, base + width * 2);
                int left = (int) base;
                int right = mid;
                int out = left;
                while (left < mid && right < high) {
                    E a = valueAt.apply(source[left]);
                    E b = valueAt.apply(source[right]);
                    int order = comparator == null
                            ? ((Comparable<? super E>) a).compareTo(b)
                            : comparator.compare(a, b);
                    // Left side wins ties: equal keys retain input encounter order.
                    destination[out++] = order <= 0 ? source[left++] : source[right++];
                }
                while (left < mid) destination[out++] = source[left++];
                while (right < high) destination[out++] = source[right++];
            }
            int[] previous = source;
            source = destination;
            destination = previous;
        }
        if (source != positions) System.arraycopy(source, 0, positions, 0, length);
    }
}
