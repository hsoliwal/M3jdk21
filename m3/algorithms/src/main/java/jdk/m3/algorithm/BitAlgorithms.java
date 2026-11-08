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
}
