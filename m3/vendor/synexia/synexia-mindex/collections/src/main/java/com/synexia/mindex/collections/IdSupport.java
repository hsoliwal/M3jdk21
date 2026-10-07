// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;

final class IdSupport {
    static final int MAX_ID = Integer.MAX_VALUE - 1;
    private static final int MIN_TABLE = 8;

    private IdSupport() { }

    /** Validate a caller-owned ID slice completely before a bulk mutation. */
    static void requireIds(MIndexSpace<?> space, int[] ids, int offset, int length) {
        java.util.Objects.requireNonNull(ids, "ids");
        java.util.Objects.checkFromIndexSize(offset, length, ids.length);
        for (int end = offset + length; offset < end; offset++) {
            space.requireId(ids[offset]);
        }
    }

    static int mix(int value) {
        int x = value;
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    static int mix(long value) {
        return mix((int) (value ^ (value >>> 32)));
    }

    static long mix64(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    static int tableCapacity(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expected size");
        }
        long required = Math.max(MIN_TABLE, (long) expectedSize * 2L);
        int capacity = MIN_TABLE;
        while (capacity < required && capacity < (1 << 30)) {
            capacity <<= 1;
        }
        if (capacity < required) {
            throw new IllegalArgumentException("collection too large");
        }
        return capacity;
    }

    static int bitWordCount(int upperIdExclusive) {
        if (upperIdExclusive < 0) {
            throw new IllegalArgumentException("negative exclusive ID bound");
        }
        return (int) ((upperIdExclusive + 63L) >>> 6);
    }

    static int grown(int current, int needed) {
        if (needed < 0 || needed > MAX_ID) {
            throw new IllegalArgumentException("size outside supported int ID range");
        }
        int capacity = Math.max(8, current);
        while (capacity < needed) {
            int next = capacity + (capacity >>> 1) + 1;
            if (next <= capacity || next > MAX_ID) {
                capacity = MAX_ID;
                break;
            }
            capacity = next;
        }
        if (capacity < needed) {
            throw new IllegalStateException("MIndex collection capacity exhausted");
        }
        return capacity;
    }

    static void checkId(int id) {
        if (id < 0 || id > MAX_ID) {
            throw new IllegalArgumentException("MIndex IDs must be in 0.." + MAX_ID + ": " + id);
        }
    }

    static long sequenceHash(int kind, int[] values, int length) {
        long hash = mix64(0x4d494e444558434fL ^ Integer.toUnsignedLong(kind));
        for (int index = 0; index < length; index++) {
            hash = mix64(hash ^ Integer.toUnsignedLong(values[index]) ^ (0x9e3779b97f4a7c15L * (index + 1L)));
        }
        return mix64(hash ^ length);
    }

    static void sortPairsByFirst(int[] first, int[] second, int length) {
        if (length <= 1) {
            return;
        }
        quickSort(first, second, 0, length - 1);
    }

    private static void quickSort(int[] first, int[] second, int low, int high) {
        while (low < high) {
            int i = low;
            int j = high;
            int pivot = first[(low + high) >>> 1];
            while (i <= j) {
                while (first[i] < pivot) {
                    i++;
                }
                while (first[j] > pivot) {
                    j--;
                }
                if (i <= j) {
                    swap(first, i, j);
                    swap(second, i, j);
                    i++;
                    j--;
                }
            }
            if (j - low < high - i) {
                if (low < j) {
                    quickSort(first, second, low, j);
                }
                low = i;
            } else {
                if (i < high) {
                    quickSort(first, second, i, high);
                }
                high = j;
            }
        }
    }

    static int[] copyOf(int[] values, int length) {
        return Arrays.copyOf(values, length);
    }

    private static void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }
}
