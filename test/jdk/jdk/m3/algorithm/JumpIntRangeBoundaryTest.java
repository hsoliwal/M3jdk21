// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Mathematical BFS oracle; no jump-greedy implementation is copied from donors. */
//FLLDCIM: RELOCATE - donor blob 24c4e27d2324 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
final class JumpIntRangeBoundaryTest {
    private static final int[] DOMAIN = {0, 1, 2, 3, Integer.MAX_VALUE};

    @Test
    void signedOverflowCannotHideReachableEnd() {
        int[] overflow = {1, Integer.MAX_VALUE, 0};
        assertTrue(IntervalGreedyAlgorithms.canReachEnd(overflow, ProgressMonitor.none()));
        assertEquals(2, IntervalGreedyAlgorithms.minimumJumps(
                overflow, ProgressMonitor.none()));
        assertFalse(IntervalGreedyAlgorithms.canReachEnd(
                new int[] {0, Integer.MAX_VALUE, 0}, ProgressMonitor.none()));
        assertEquals(-1, IntervalGreedyAlgorithms.minimumJumps(
                new int[] {0, Integer.MAX_VALUE, 0}, ProgressMonitor.none()));
        assertEquals(0, IntervalGreedyAlgorithms.minimumJumps(
                new int[0], ProgressMonitor.none()));
        assertTrue(IntervalGreedyAlgorithms.canReachEnd(
                new int[0], ProgressMonitor.none()));
    }

    @Test
    void exhaustiveBoundaryAndSeededGreedyVsIndependentBfs() {
        int count = 0;
        for (int length = 0; length <= 7; length++) {
            int combinations = 1;
            for (int i = 0; i < length; i++) combinations *= DOMAIN.length;
            for (int mask = 0; mask < combinations; mask++) {
                int[] values = new int[length];
                int remainder = mask;
                for (int i = 0; i < length; i++) {
                    values[i] = DOMAIN[remainder % DOMAIN.length];
                    remainder /= DOMAIN.length;
                }
                verify(values);
                count++;
            }
        }

        Random random = new Random(0x20261105L);
        for (int k = 0; k < 30_000; k++) {
            int[] values = new int[random.nextInt(17)];
            for (int i = 0; i < values.length; i++) {
                int p = random.nextInt(10);
                values[i] = p == 0 ? Integer.MAX_VALUE
                        : p == 1 ? Integer.MAX_VALUE - 1 : random.nextInt(9);
            }
            verify(values);
            count++;
        }
        assertEquals(127_656, count);
    }

    private static void verify(int[] values) {
        int[] saved = values.clone();
        int expected = mathematicalMinimum(values);
        assertEquals(expected >= 0, IntervalGreedyAlgorithms.canReachEnd(
                values, ProgressMonitor.none()), Arrays.toString(values));
        assertEquals(expected, IntervalGreedyAlgorithms.minimumJumps(
                values, ProgressMonitor.none()), Arrays.toString(values));
        assertArrayEquals(saved, values, "input mutated");
    }

    /** Independent O(n²) BFS; bounded visited queue and long end-exclusive arithmetic. */
    private static int mathematicalMinimum(int[] values) {
        int n = values.length;
        if (n <= 1) return 0;
        int[] distances = new int[n];
        Arrays.fill(distances, -1);
        int[] queue = new int[n];
        int read = 0, write = 0;
        queue[write++] = 0;
        distances[0] = 0;
        while (read < write) {
            int at = queue[read++];
            long last = Math.min((long) n - 1L, (long) at + values[at]);
            for (int next = at + 1; next <= last; next++) {
                if (distances[next] >= 0) continue;
                distances[next] = distances[at] + 1;
                queue[write++] = next;
            }
        }
        return distances[n - 1];
    }

    @Test
    void progressAndCancellationContractsAreRetained() {
        class Monitor implements ProgressMonitor {
            int begins, worked, done, checks;
            boolean canceled;
            @Override public boolean isCanceled() { checks++; return canceled; }
            @Override public void beginTask(String name, int size) {
                begins++;
                assertTrue(name.equals("jump-game") || name.equals("minimum-jumps"));
                assertEquals(5, size);
            }
            @Override public void worked(int count) { worked += count; }
            @Override public void done() { done++; }
        }
        Monitor reach = new Monitor();
        int[] sample = {2, 3, 1, 1, 4};
        assertTrue(IntervalGreedyAlgorithms.canReachEnd(sample, reach));
        assertEquals(1, reach.begins);
        assertEquals(2, reach.worked);
        assertEquals(1, reach.done);
        Monitor min = new Monitor();
        assertEquals(2, IntervalGreedyAlgorithms.minimumJumps(sample, min));
        assertEquals(1, min.begins);
        assertEquals(3, min.worked);
        assertEquals(1, min.done);

        Monitor early = new Monitor();
        early.canceled = true;
        assertThrows(CancellationException.class,
                () -> IntervalGreedyAlgorithms.canReachEnd(sample, early));
        assertEquals(0, early.begins);
        assertEquals(0, early.done);

        Monitor during = new Monitor() {
            @Override public void worked(int count) {
                super.worked(count);
                canceled = true;
            }
        };
        assertThrows(CancellationException.class,
                () -> IntervalGreedyAlgorithms.minimumJumps(sample, during));
        assertEquals(1, during.begins);
        assertEquals(1, during.worked);
        assertEquals(1, during.done);
    }
}
