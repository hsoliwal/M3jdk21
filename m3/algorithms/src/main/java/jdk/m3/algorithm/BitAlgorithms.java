// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;

//FLLDCIM: RELOCATE - donor blob 53798ecec92c at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class BitAlgorithms {
    private BitAlgorithms() {
    }

    public static int hammingWeight(int value, ProgressMonitor monitor) {
        monitor.checkCanceled();
        int count = 0;
        while (value != 0) {
            monitor.checkCanceled();
            value &= value - 1;
            count++;
        }
        return count;
    }

    public static int reverseBits(int value, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "reverse-bits", Integer.SIZE);
        try {
            int result = 0;
            for (int bit = 0; bit < Integer.SIZE; bit++) {
                AlgorithmSupport.tick(monitor);
                result = (result << 1) | (value & 1);
                value >>>= 1;
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    public static int singleNumberTriples(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "single-number-triplicate", values.length);
        try {
            int ones = 0;
            int twos = 0;
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                ones = (ones ^ value) & ~twos;
                twos = (twos ^ value) & ~ones;
            }
            return ones;
        } finally {
            monitor.done();
        }
    }

    public static int missingNumber(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "xor-missing-number", values.length);
        try {
            int result = values.length;
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
                result ^= index ^ values[index];
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    //FLLDCIM: FOLD - member PrimitiveBitAlgorithms.isPowerOfTwo from donor blob e34b3eb5615f (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBitAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-bit-fold/FOLD_MAP.tsv
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    //FLLDCIM: FOLD - member PrimitiveBitAlgorithms.addWithoutPlus from donor blob e34b3eb5615f (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBitAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-bit-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBitAlgorithms.rangeBitwiseAnd from donor blob e34b3eb5615f (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBitAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-bit-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member PrimitiveBitAlgorithms.twoSingleNumbers from donor blob e34b3eb5615f (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveBitAlgorithms.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-bit-fold/FOLD_MAP.tsv
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

    //FLLDCIM: FOLD - member BitOpsBasic.setBit from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Set bit at position i. */
    public static int setBit(int n, int i) {
        return n | (1 << i);
    }

    //FLLDCIM: FOLD - member BitOpsBasic.clearBit from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Clear bit at position i. */
    public static int clearBit(int n, int i) {
        return n & ~(1 << i);
    }

    //FLLDCIM: FOLD - member BitOpsBasic.toggleBit from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Toggle bit at position i. */
    public static int toggleBit(int n, int i) {
        return n ^ (1 << i);
    }

    //FLLDCIM: FOLD - member BitOpsBasic.isSet from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Check if bit at position i is set. */
    public static boolean isSet(int n, int i) {
        return (n & (1 << i)) != 0;
    }

    //FLLDCIM: FOLD - member BitOpsBasic.getBit from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Get bit at position i (0 or 1). */
    public static int getBit(int n, int i) {
        return (n >> i) & 1;
    }

    //FLLDCIM: FOLD - member BitOpsBasic.updateBit from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Update bit at position i to value (0 or 1). */
    public static int updateBit(int n, int i, int value) {
        return (n & ~(1 << i)) | (value << i);
    }

    //FLLDCIM: FOLD - member BitOpsBasic.clearMsbToI from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Clear all bits from most significant to i (inclusive). */
    public static int clearMsbToI(int n, int i) {
        return n & ((1 << i) - 1);
    }

    //FLLDCIM: FOLD - member BitOpsBasic.clearIToLsb from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Clear all bits from i to 0 (inclusive). */
    public static int clearIToLsb(int n, int i) {
        return i >= Integer.SIZE - 1 ? 0 : n & (~0 << (i + 1));
    }

    //FLLDCIM: FOLD - member BitOpsBasic.highestSetBit from donor blob c3ca587e8d7e (donor blob c3ca587e8d7e at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Highest set bit position (0-indexed from right). */
    public static int highestSetBit(int n) {
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    //FLLDCIM: FOLD - member BitOpsPower.nextPowerOfTwo from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Next power of 2 >= n. */
    public static int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }

    //FLLDCIM: FOLD - member BitOpsPower.prevPowerOfTwo from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Previous power of 2 <= n. */
    public static int prevPowerOfTwo(int n) {
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n - (n >> 1);
    }

    //FLLDCIM: FOLD - member BitOpsPower.roundUpPow2 from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Round up to next power of 2. */
    public static int roundUpPow2(int n) {
        return isPowerOfTwo(n) ? n : nextPowerOfTwo(n);
    }

    //FLLDCIM: FOLD - member BitOpsPower.isolateRightmost1 from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Isolate rightmost 1-bit. */
    public static int isolateRightmost1(int n) {
        return n & (-n);
    }

    //FLLDCIM: FOLD - member BitOpsPower.turnOffRightmost1 from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Turn off rightmost 1-bit. */
    public static int turnOffRightmost1(int n) {
        return n & (n - 1);
    }

    //FLLDCIM: FOLD - member BitOpsPower.turnOnRightmost0 from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Turn on rightmost 0-bit. */
    public static int turnOnRightmost0(int n) {
        return n | (n + 1);
    }

    //FLLDCIM: FOLD - member BitOpsPower.isolateRightmost0 from donor blob 4901b187ccc6 (donor blob 4901b187ccc6 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Isolate rightmost 0-bit. */
    public static int isolateRightmost0(int n) {
        return ~n & (n + 1);
    }
    //FLLDCIM: FOLD - member MathsBits.hammingDistance from donor blob 0d2ac952f60b (donor blob 0d2ac952f60b at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/bit-ops-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/bit-ops-fold/FOLD_MAP.tsv
    /** Hamming distance (differing bits). */
    public static int hammingDistance(int x, int y) {
        return Integer.bitCount(x ^ y);
    }
}
