// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

/** Canonical integer/bit problem shapes with no allocation in the hot path. */
//FLLDCIM: RELOCATE - donor blob 7e3c11097e78 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class PrimitiveBitAlgorithms {
    private PrimitiveBitAlgorithms() {}

    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    public static int addWithoutPlus(int left, int right) {
        int a = left;
        int b = right;
        while (b != 0) {
            int carry = (a & b) << 1;
            a ^= b;
            b = carry;
        }
        return a;
    }

    public static int rangeBitwiseAnd(int left, int right) {
        if (left < 0 || right < left) {
            throw new IllegalArgumentException("requires 0<=left<=right");
        }
        int shift = 0;
        while (left != right) {
            left >>>= 1;
            right >>>= 1;
            shift++;
        }
        return left << shift;
    }

    public static int[] twoSingleNumbers(int[] values) {
        if (values == null) throw new NullPointerException("values");
        int xor = 0;
        for (int value : values) xor ^= value;
        if (xor == 0) throw new IllegalArgumentException("input does not contain two distinct singles");
        int bit = xor & -xor;
        int first = 0;
        int second = 0;
        for (int value : values) {
            if ((value & bit) == 0) first ^= value;
            else second ^= value;
        }
        return first <= second ? new int[] {first, second} : new int[] {second, first};
    }
}
