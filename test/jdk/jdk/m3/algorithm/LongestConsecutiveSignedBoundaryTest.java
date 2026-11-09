// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Signed integer overflow boundary for the first-party consecutive-run HashSet. */
//FLLDCIM: RELOCATE - donor blob 4c0d7669e6e7 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
final class LongestConsecutiveSignedBoundaryTest {
    private static final int[] EXTREMES = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2,
        -1, 0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };

    @Test
    void mathematicalMinMaxDoNotCreateWraparoundPredecessors() {
        ProgressMonitor none = ProgressMonitor.none();
        assertEquals(2, ArrayAlgorithms.longestConsecutive(
                new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE}, none));
        assertEquals(3, ArrayAlgorithms.longestConsecutive(
                new int[] {Integer.MIN_VALUE + 2, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, Integer.MIN_VALUE + 1}, none));
        assertEquals(2, ArrayAlgorithms.longestConsecutive(
                new int[] {Integer.MIN_VALUE + 1, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE}, none));
        assertEquals(1, ArrayAlgorithms.longestConsecutive(
                new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE}, none));
        assertEquals(0, ArrayAlgorithms.longestConsecutive(new int[0], none));
    }

    @Test
    void boundedExhaustiveAndSeededFullIntegerDomain() {
        int cases = 0;
        for (int length = 0; length <= 5; length++) {
            int combinations = 1;
            for (int j = 0; j < length; j++) combinations *= EXTREMES.length;
            for (int mask = 0; mask < combinations; mask++) {
                int[] values = new int[length];
                int remainder = mask;
                for (int j = 0; j < length; j++) {
                    values[j] = EXTREMES[remainder % EXTREMES.length];
                    remainder /= EXTREMES.length;
                }
                verify(values);
                cases++;
            }
        }
        Random random = new Random(0x8a7e_2026L);
        for (int iteration = 0; iteration < 60_000; iteration++) {
            int[] values = new int[random.nextInt(45)];
            for (int j = 0; j < values.length; j++) {
                values[j] = random.nextBoolean()
                        ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextInt();
            }
            verify(values);
            cases++;
        }
        assertEquals(97_449, cases);
    }

    private static void verify(int[] values) {
        int[] snapshot = values.clone();
        assertEquals(oracle(values),
                ArrayAlgorithms.longestConsecutive(values, ProgressMonitor.none()));
        assertArrayEquals(snapshot, values);
    }

    /** Independent sorted mathematical oracle; long subtraction cannot wrap at either bound. */
    private static int oracle(int[] values) {
        if (values.length == 0) return 0;
        int[] copy = values.clone();
        Arrays.sort(copy);
        int best = 1, length = 1;
        for (int i = 1; i < copy.length; i++) {
            if (copy[i] == copy[i - 1]) continue;
            length = (long) copy[i] - copy[i - 1] == 1L ? length + 1 : 1;
            best = Math.max(best, length);
        }
        return best;
    }

    @Test
    void callbacksAndCancellationRemainExplicit() {
        final class Monitor implements ProgressMonitor {
            int begins, done, worked, checks, taskTotal;
            boolean cancel;
            @Override public boolean isCanceled() { checks++; return cancel; }
            @Override public void beginTask(String name, int total) {
                begins++;
                assertEquals("longest-consecutive", name);
                taskTotal = total;
            }
            @Override public void worked(int amount) { worked += amount; }
            @Override public void done() { done++; }
        }
        Monitor monitor = new Monitor();
        int[] values = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1,
                Integer.MAX_VALUE, Integer.MIN_VALUE + 1};
        assertEquals(2, ArrayAlgorithms.longestConsecutive(values, monitor));
        assertEquals(1, monitor.begins);
        assertEquals(1, monitor.done);
        assertEquals(values.length * 2, monitor.taskTotal);
        Set<Integer> distinct = new HashSet<>();
        for (int x : values) distinct.add(x);
        assertEquals(values.length + distinct.size(), monitor.worked);
        assertTrue(monitor.checks > monitor.worked);

        Monitor cancelled = new Monitor();
        cancelled.cancel = true;
        assertThrows(CancellationException.class,
                () -> ArrayAlgorithms.longestConsecutive(values, cancelled));
        assertEquals(0, cancelled.begins);
        assertEquals(0, cancelled.done);
    }
}
