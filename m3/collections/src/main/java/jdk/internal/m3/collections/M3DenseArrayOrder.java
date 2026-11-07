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

import java.util.Comparator;

/**
 * Array sizing and stable primitive-index ordering; never creates entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseArrayOrder.java}
 * blob {@code 99a13aab2cd55af84baa6e46683e121744d6c44e} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3DenseArrayOrder {
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private M3DenseArrayOrder() { }

    static int capacity(int oldCapacity, int required) {
        if (required < 0 || required > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("array capacity overflow: " + required);
        }
        long grown = Math.max(8L, (long) oldCapacity + (oldCapacity >>> 1) + 1L);
        return (int) Math.min(MAX_ARRAY_SIZE, Math.max(grown, required));
    }

    static int sum(int size, int extra) {
        long total = (long) size + extra;
        if (size < 0 || extra < 0 || total > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("array size overflow");
        }
        return (int) total;
    }

    /** Stable mergesort of indices, not boxed indices or key/value tuples. */
    static int[] order(Object[] keys, int size, Comparator<Object> comparator) {
        int[] source = new int[size];
        int[] target = new int[size];
        for (int i = 0; i < size; i++) {
            source[i] = i;
        }
        for (int width = 1; width < size; width = width >= size - width ? size : width * 2) {
            for (int start = 0; start < size;) {
                int middle = (int) Math.min((long) size, (long) start + width);
                int end = (int) Math.min((long) size, (long) middle + width);
                int left = start;
                int right = middle;
                int out = start;
                while (left < middle && right < end) {
                    if (comparator.compare(keys[source[left]], keys[source[right]]) <= 0) {
                        target[out++] = source[left++];
                    } else {
                        target[out++] = source[right++];
                    }
                }
                while (left < middle) {
                    target[out++] = source[left++];
                }
                while (right < end) {
                    target[out++] = source[right++];
                }
                start = end;
            }
            int[] swap = source;
            source = target;
            target = swap;
        }
        return source;
    }
}
