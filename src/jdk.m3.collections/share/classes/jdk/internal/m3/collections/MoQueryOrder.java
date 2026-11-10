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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Deterministic Mo-style offline ordering for half-open range queries.
 *
 * <p>The result is only a query permutation. The caller owns add/remove/query state, so this
 * kernel imposes no boxed query object or aggregation semantics.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/MoQueryOrder.java}
 * blob {@code 65690ffd7b505460f2a32090276b00faeb4d1c80} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MoQueryOrder {
    private MoQueryOrder() {}

    public static int[] order(int domainLength, int[] left, int[] right) {
        if (domainLength < 0) {
            throw new IllegalArgumentException("domainLength");
        }
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) {
            throw new IllegalArgumentException("query lane length mismatch");
        }
        int blockSize = Math.max(1, integerSqrt(Math.max(1, domainLength)));
        return order(domainLength, left, right, blockSize);
    }

    /**
     * Orders by left block, alternating right direction between adjacent blocks.
     * This reduces cursor resets without changing exact query semantics.
     */
    public static int[] order(
            int domainLength,
            int[] left,
            int[] right,
            int blockSize) {
        if (domainLength < 0) {
            throw new IllegalArgumentException("domainLength");
        }
        if (blockSize <= 0) {
            throw new IllegalArgumentException("blockSize");
        }
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) {
            throw new IllegalArgumentException("query lane length mismatch");
        }
        int[] order = new int[left.length];
        for (int i = 0; i < left.length; i++) {
            Objects.checkFromToIndex(left[i], right[i], domainLength);
            order[i] = i;
        }
        if (order.length > 1) {
            quickSort(order, 0, order.length - 1, left, right, blockSize);
        }
        return order;
    }

    public static long totalMovement(
            int[] order,
            int[] left,
            int[] right) {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) {
            throw new IllegalArgumentException("query lane length mismatch");
        }
        long movement = 0L;
        int currentLeft = 0;
        int currentRight = 0;
        for (int query : order) {
            Objects.checkIndex(query, left.length);
            movement = Math.addExact(
                    movement,
                    Math.abs((long) left[query] - currentLeft));
            movement = Math.addExact(
                    movement,
                    Math.abs((long) right[query] - currentRight));
            currentLeft = left[query];
            currentRight = right[query];
        }
        return movement;
    }

    private static void quickSort(
            int[] order,
            int low,
            int high,
            int[] left,
            int[] right,
            int blockSize) {
        while (low < high) {
            int i = low;
            int j = high;
            int pivot = order[(low + high) >>> 1];
            while (i <= j) {
                while (compare(order[i], pivot, left, right, blockSize) < 0) {
                    i++;
                }
                while (compare(order[j], pivot, left, right, blockSize) > 0) {
                    j--;
                }
                if (i <= j) {
                    int swap = order[i];
                    order[i++] = order[j];
                    order[j--] = swap;
                }
            }
            if (j - low < high - i) {
                if (low < j) {
                    quickSort(order, low, j, left, right, blockSize);
                }
                low = i;
            } else {
                if (i < high) {
                    quickSort(order, i, high, left, right, blockSize);
                }
                high = j;
            }
        }
    }

    private static int compare(
            int first,
            int second,
            int[] left,
            int[] right,
            int blockSize) {
        int firstBlock = left[first] / blockSize;
        int secondBlock = left[second] / blockSize;
        if (firstBlock != secondBlock) {
            return Integer.compare(firstBlock, secondBlock);
        }
        int byRight = (firstBlock & 1) == 0
                ? Integer.compare(right[first], right[second])
                : Integer.compare(right[second], right[first]);
        if (byRight != 0) {
            return byRight;
        }
        int byLeft = Integer.compare(left[first], left[second]);
        return byLeft != 0 ? byLeft : Integer.compare(first, second);
    }

    private static int integerSqrt(int value) {
        int root = (int) Math.sqrt(value);
        while ((long) (root + 1) * (root + 1) <= value) {
            root++;
        }
        while ((long) root * root > value) {
            root--;
        }
        return root;
    }
}
