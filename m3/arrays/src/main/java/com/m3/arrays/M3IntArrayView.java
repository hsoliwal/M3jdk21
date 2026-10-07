// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

/** Immutable logical int-array view with explicit mutable-array materialization. */
public interface M3IntArrayView {
    int length();

    int intAt(int index);

    M3IntArrayView slice(int start, int end);

    int segmentCount();

    default void copyTo(int sourceStart, int[] target, int targetStart, int count) {
        java.util.Objects.requireNonNull(target, "target");
        java.util.Objects.checkFromIndexSize(sourceStart, count, length());
        java.util.Objects.checkFromIndexSize(targetStart, count, target.length);
        for (int index = 0; index < count; index++) {
            target[targetStart + index] = intAt(sourceStart + index);
        }
    }

    default int[] copy() {
        int[] result = new int[length()];
        copyTo(0, result, 0, result.length);
        return result;
    }
}
