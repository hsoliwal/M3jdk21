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
}
