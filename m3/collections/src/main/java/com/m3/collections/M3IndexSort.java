// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Stable primitive-index sorting over the authoritative source owner.
 *
 * <p>The original {@link #stable} call keeps its exact merge-pass and callback
 * sequence, including the index-array state when a callback throws. A separate
 * explicit prepared route may avoid scratch for trusted pure, stable accessors.
 * Neither method sorts or copies the parent's object payload.
 */
final class M3IndexSort {
    private static final int INSERTION_LIMIT = 32;

    private M3IndexSort() { }

    /**
     * Contract-preserving original default: keep comparison/callback order and
     * exception-time physical-index state exactly as in the existing JDK owner.
     */
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

    /**
     * Explicit opt-in fast path ONLY for immutable, side-effect-free, stable
     * valueAt/comparator implementations whose invocation count and order are
     * not observable. Callers without that proof must use {@link #stable}.
     * No JNI callback crossing and no public JDK API changes.
     */
    static <E> void stablePrepared(int[] positions, IntFunction<? extends E> valueAt,
                           Comparator<? super E> comparator) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(valueAt, "valueAt");
        int length = positions.length;
        if (length < 2) return;

        boolean ordered = true;
        for (int i = 1; i < length; i++) {
            if (compare(positions[i - 1], positions[i], valueAt, comparator) > 0) {
                ordered = false;
                break;
            }
        }
        if (ordered) return;
        if (length <= INSERTION_LIMIT) {
            for (int i = 1; i < length; i++) {
                int selected = positions[i];
                int j = i;
                while (j > 0
                        && compare(positions[j - 1], selected, valueAt, comparator) > 0) {
                    positions[j] = positions[j - 1];
                    j--;
                }
                positions[j] = selected;
            }
            return;
        }
        // Reuse the sole original stable merge algorithm for larger lanes.
        stable(positions, valueAt, comparator);
    }

    @SuppressWarnings("unchecked")
    private static <E> int compare(int left, int right,
            IntFunction<? extends E> valueAt, Comparator<? super E> comparator) {
        E a = valueAt.apply(left);
        E b = valueAt.apply(right);
        return comparator == null ? ((Comparable<? super E>) a).compareTo(b)
                : comparator.compare(a, b);
    }
}
