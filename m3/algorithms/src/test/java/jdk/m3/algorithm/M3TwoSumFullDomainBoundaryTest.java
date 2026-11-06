// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exact mathematical two-sum oracle over the complete signed int domain. */
//FLLDCIM: RELOCATE - donor blob 01a4c12a6b72 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
final class M3TwoSumFullDomainBoundaryTest {
    private static final int[] DOMAIN = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1,
        Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    private static final int[] TARGETS = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -3, -1, 0, 1, 3,
        Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };

    @Test
    void wrappedComplementsMustNotCreateMathematicallyImpossibleMatches() {
        assertArrayEquals(new int[] {-1, -1}, M3ArrayAlgorithms.twoSum(
                new int[] {Integer.MAX_VALUE, 1}, Integer.MIN_VALUE,
                M3ProgressMonitor.none()));
        assertArrayEquals(new int[] {-1, -1}, M3ArrayAlgorithms.twoSum(
                new int[] {-1, Integer.MIN_VALUE}, Integer.MAX_VALUE,
                M3ProgressMonitor.none()));
        assertArrayEquals(new int[] {0, 1}, M3ArrayAlgorithms.twoSum(
                new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}, -1,
                M3ProgressMonitor.none()));
        assertArrayEquals(new int[] {0, 1}, M3ArrayAlgorithms.twoSum(
                new int[] {3, 3}, 6, M3ProgressMonitor.none()));
    }

    @Test
    void independentLongArithmeticOracleCoversBoundaryAndSeededArrays() {
        int count = 0;
        for (int length = 0; length <= 5; length++) {
            int combinations = 1;
            for (int j = 0; j < length; j++) combinations *= DOMAIN.length;
            for (int k = 0; k < combinations; k++) {
                int[] values = new int[length];
                int rem = k;
                for (int j = 0; j < length; j++) {
                    values[j] = DOMAIN[rem % DOMAIN.length];
                    rem /= DOMAIN.length;
                }
                for (int target : TARGETS) {
                    check(values, target);
                    count++;
                }
            }
        }
        Random random = new Random(0x5e21_2026L);
        for (int iteration = 0; iteration < 40_000; iteration++) {
            int[] values = new int[random.nextInt(38)];
            for (int i = 0; i < values.length; i++) {
                values[i] = random.nextBoolean()
                        ? DOMAIN[random.nextInt(DOMAIN.length)] : random.nextInt();
            }
            int target = random.nextBoolean()
                    ? TARGETS[random.nextInt(TARGETS.length)] : random.nextInt();
            check(values, target);
            count++;
        }
        assertEquals(216_472, count);
    }

    private static void check(int[] values, int target) {
        int[] input = values.clone();
        assertArrayEquals(earliestPair(values, target),
                M3ArrayAlgorithms.twoSum(values, target, M3ProgressMonitor.none()),
                "target=" + target + ", values=" + Arrays.toString(values));
        assertArrayEquals(input, values, "source values changed");
    }

    /** O(n²) source-independent mathematical oracle: earliest right, then earliest left. */
    private static int[] earliestPair(int[] values, int target) {
        for (int right = 0; right < values.length; right++) {
            for (int left = 0; left < right; left++) {
                if ((long) values[left] + values[right] == target)
                    return new int[] {left, right};
            }
        }
        return new int[] {-1, -1};
    }

    @Test
    void callbackAndCancellationOrderIsPreserved() {
        final class Trace implements M3ProgressMonitor {
            private final StringBuilder events = new StringBuilder();
            private boolean cancel;
            @Override public boolean isCanceled() {
                events.append("C;");
                return cancel;
            }
            @Override public void beginTask(String name, int total) {
                events.append("B").append(name).append(':').append(total).append(';');
            }
            @Override public void worked(int amount) {
                events.append("W").append(amount).append(';');
            }
            @Override public void done() {
                events.append("D;");
            }
        }
        Trace normal = new Trace();
        assertArrayEquals(new int[] {0, 1},
                M3ArrayAlgorithms.twoSum(new int[] {2, 7, 11, 15}, 9, normal));
        assertEquals("C;Btwo-sum:4;C;W1;C;W1;D;", normal.events.toString());

        Trace midCancel = new Trace();
        M3ProgressMonitor monitor = new M3ProgressMonitor() {
            @Override public boolean isCanceled() {
                return midCancel.cancel;
            }
            @Override public void beginTask(String name, int total) {
                midCancel.beginTask(name, total);
            }
            @Override public void worked(int amount) {
                midCancel.worked(amount);
                midCancel.cancel = true;
            }
            @Override public void done() {
                midCancel.done();
            }
        };
        assertThrows(CancellationException.class,
                () -> M3ArrayAlgorithms.twoSum(new int[] {1, 2, 3}, 19, monitor));
        assertEquals("Btwo-sum:3;W1;D;", midCancel.events.toString());
    }
}
