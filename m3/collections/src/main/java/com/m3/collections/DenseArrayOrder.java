// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Comparator;

/** Array sizing and stable primitive-index ordering; never creates entry objects. */
final class DenseArrayOrder {
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private DenseArrayOrder() { }

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

    /** Index-only specialization of the same merge atom; existing object entry is unchanged.
     * Checkpoints occur at merge boundaries and at most 1024 emitted indices apart.
     */
    static int[] order(int size, java.util.function.IntBinaryOperator comparator, Runnable checkpoint) {
        java.util.Objects.requireNonNull(comparator);
        java.util.Objects.requireNonNull(checkpoint);
        checkpoint.run();
        if (size < 0 || size > MAX_ARRAY_SIZE) throw new IllegalArgumentException("invalid size");
        int[] source = new int[size], target = new int[size];
        for (int i = 0; i < size; i++) {
            if ((i & 1023) == 0) checkpoint.run();
            source[i] = i;
        }
        for (int width = 1; width < size; width = width >= size - width ? size : width * 2) {
            for (int start = 0; start < size;) {
                checkpoint.run();
                int middle = (int) Math.min((long) size, (long) start + width);
                int end = (int) Math.min((long) size, (long) middle + width);
                int left = start, right = middle;
                for (int out = start; out < end; out++) {
                    if ((out & 1023) == 0) checkpoint.run();
                    if (left < middle && (right == end
                            || comparator.applyAsInt(source[left], source[right]) <= 0)) {
                        target[out] = source[left++];
                    } else {
                        target[out] = source[right++];
                    }
                }
                start = end;
            }
            int[] swap = source; source = target; target = swap;
        }
        checkpoint.run();
        return source;
    }
}
