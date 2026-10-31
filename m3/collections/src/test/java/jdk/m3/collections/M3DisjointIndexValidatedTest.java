/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Partition-array oracle; no pointer-node donor or extra runtime dependencies. */
final class M3DisjointIndexValidatedTest {
    @Test void seededUnionFindRetainsRootMembershipSizesAndRevision() {
        for (int trial = 0; trial < 7; trial++) {
            final int n = 64;
            M3DisjointIndex actual = new M3DisjointIndex();
            int[] labels = new int[n];
            for (int i = 0; i < n; i++) {
                assertEquals(i, actual.add(i & 3));
                labels[i] = i;
            }
            assertEquals(n, actual.componentCount());
            Random rng = new Random(0x684FL + trial);
            int components = n;
            for (int step = 0; step < 5000; step++) {
                int left = rng.nextInt(n);
                int right = rng.nextInt(n);
                boolean same = labels[left] == labels[right];
                long revision = actual.revision();
                if ((step & 3) < 2) {
                    assertEquals(!same, actual.union(left, right));
                    if (!same) {
                        int prior = labels[right], replacement = labels[left];
                        for (int i = 0; i < n; i++) {
                            if (labels[i] == prior) labels[i] = replacement;
                        }
                        components--;
                    }
                    assertEquals(revision + (same ? 0L : 1L), actual.revision());
                } else {
                    assertEquals(same, actual.connected(left, right));
                    assertEquals(revision, actual.revision());
                }
                assertEquals(components, actual.componentCount());
                int expectedSize = 0;
                for (int label : labels) if (label == labels[left]) expectedSize++;
                assertEquals(expectedSize, actual.componentSize(left));
                assertEquals(labels[left] == labels[right],
                        actual.find(left) == actual.find(right));
            }
            long stableRevision = actual.revision();
            assertThrows(IndexOutOfBoundsException.class, () -> actual.union(0, n));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.connected(-1, 0));
            assertEquals(stableRevision, actual.revision());
            actual.ensureCapacity(n + 40);
            assertEquals(stableRevision, actual.revision());
            boolean[] expected = new boolean[n];
            int[] left = new int[n], right = new int[n];
            for (int i = 0; i < n; i++) {
                left[i] = i;
                right[i] = n - i - 1;
                expected[i] = labels[left[i]] == labels[right[i]];
            }
            assertArrayEquals(expected,
                    actual.connectedBatch(left, right, M3DisjointIndex.Backend.JAVA));
            assertEquals(stableRevision, actual.revision());
        }
    }
}
