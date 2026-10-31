// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

/** Immutable logical long-array view with explicit mutable-array materialization. */
public interface M3LongArrayView {
    int length();

    long longAt(int index);

    M3LongArrayView slice(int start, int end);


    default void copyTo(int sourceStart, long[] target, int targetStart, int count) {
        java.util.Objects.requireNonNull(target, "target");
        java.util.Objects.checkFromIndexSize(sourceStart, count, length());
        java.util.Objects.checkFromIndexSize(targetStart, count, target.length);
        for (int index = 0; index < count; index++) {
            target[targetStart + index] = longAt(sourceStart + index);
        }
    }

    default long[] copy() {
        long[] result = new long[length()];
        copyTo(0, result, 0, result.length);
        return result;
    }
}
