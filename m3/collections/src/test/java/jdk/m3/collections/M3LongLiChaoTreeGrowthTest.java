/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Preserves envelope behavior when primitive child lanes are replaced during insertion. */
class M3LongLiChaoTreeGrowthTest {

    @Test
    void minimumEnvelopeRetainsRightChildAcrossGrowth() {
        checkFirstGrowth(M3LongLiChaoTree.Mode.MIN, false);
    }

    @Test
    void minimumEnvelopeRetainsLeftChildAcrossGrowth() {
        checkFirstGrowth(M3LongLiChaoTree.Mode.MIN, true);
    }

    @Test
    void maximumEnvelopeRetainsRightChildAcrossGrowth() {
        checkFirstGrowth(M3LongLiChaoTree.Mode.MAX, false);
    }

    @Test
    void maximumEnvelopeRetainsLeftChildAcrossGrowth() {
        checkFirstGrowth(M3LongLiChaoTree.Mode.MAX, true);
    }

    @Test
    void repeatedGrowthMatchesNaiveEnvelopeAfterEveryInsertion() {
        for (M3LongLiChaoTree.Mode mode : M3LongLiChaoTree.Mode.values()) {
            M3LongLiChaoTree tree = new M3LongLiChaoTree(0, 255, mode);
            long direction = mode == M3LongLiChaoTree.Mode.MIN ? 1L : -1L;
            for (int inserted = 0; inserted < 128; inserted++) {
                tree.addLine(-2L * inserted * direction,
                        (long) inserted * inserted * direction);
                for (int x = 0; x <= 255; x++) {
                    long expected = mode == M3LongLiChaoTree.Mode.MIN
                            ? Long.MAX_VALUE : Long.MIN_VALUE;
                    for (int line = 0; line <= inserted; line++) {
                        long value = ((long) line * line - 2L * line * x) * direction;
                        expected = mode == M3LongLiChaoTree.Mode.MIN
                                ? Math.min(expected, value) : Math.max(expected, value);
                    }
                    assertEquals(expected, tree.query(x),
                            mode + " insertion=" + inserted + " x=" + x);
                }
            }
        }
    }

    @Test
    void fullSignedDomainRetainsExactEndpointAndMidpointBehavior() {
        long[] points = {
            Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1,
            Long.MAX_VALUE - 1, Long.MAX_VALUE
        };
        for (M3LongLiChaoTree.Mode mode : M3LongLiChaoTree.Mode.values()) {
            M3LongLiChaoTree tree = new M3LongLiChaoTree(Long.MIN_VALUE, Long.MAX_VALUE, mode);
            tree.addLine(0, 0);
            tree.addLine(1, 0);
            for (long x : points) {
                long expected = mode == M3LongLiChaoTree.Mode.MIN
                        ? Math.min(0, x) : Math.max(0, x);
                assertEquals(expected, tree.query(x), mode + " x=" + x);
            }
        }
    }

    @Test
    void singletonDomainsRetainExtremeConstantValues() {
        for (M3LongLiChaoTree.Mode mode : M3LongLiChaoTree.Mode.values()) {
            for (long x : new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}) {
                M3LongLiChaoTree tree = new M3LongLiChaoTree(x, x, mode);
                tree.addLine(0, Long.MIN_VALUE);
                tree.addLine(0, Long.MAX_VALUE);
                long expected = mode == M3LongLiChaoTree.Mode.MIN
                        ? Long.MIN_VALUE : Long.MAX_VALUE;
                assertEquals(expected, tree.query(x));
            }
        }
    }

    @Test
    void constructorAndQueryFailuresRemainExplicit() {
        assertThrows(IllegalArgumentException.class,
                () -> new M3LongLiChaoTree(1, 0, M3LongLiChaoTree.Mode.MIN));
        assertThrows(NullPointerException.class,
                () -> new M3LongLiChaoTree(0, 1, null));
        M3LongLiChaoTree tree = new M3LongLiChaoTree(-1, 1, M3LongLiChaoTree.Mode.MIN);
        assertThrows(NoSuchElementException.class, () -> tree.query(0));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.query(-2));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.query(2));
    }

    @Test
    void multiplicationAndAdditionOverflowStillThrow() {
        for (M3LongLiChaoTree.Mode mode : M3LongLiChaoTree.Mode.values()) {
            M3LongLiChaoTree multiplication = new M3LongLiChaoTree(2, 2, mode);
            multiplication.addLine(Long.MAX_VALUE, 0);
            assertThrows(ArithmeticException.class, () -> multiplication.query(2));

            M3LongLiChaoTree addition = new M3LongLiChaoTree(1, 1, mode);
            addition.addLine(1, Long.MAX_VALUE);
            assertThrows(ArithmeticException.class, () -> addition.query(1));

            M3LongLiChaoTree insertion = new M3LongLiChaoTree(2, 2, mode);
            insertion.addLine(0, 0);
            assertThrows(ArithmeticException.class,
                    () -> insertion.addLine(Long.MAX_VALUE, 0));
        }
    }

    private static void checkFirstGrowth(M3LongLiChaoTree.Mode mode, boolean left) {
        M3LongLiChaoTree tree = new M3LongLiChaoTree(0, 65535, mode);
        long direction = mode == M3LongLiChaoTree.Mode.MIN ? 1L : -1L;
        for (int i = 0; i < 16; i++) {
            long slope = direction * (left ? i : -i);
            long intercept = direction * (left ? -i : i * 65534L);
            tree.addLine(slope, intercept);
            long x = left ? 0 : 65535;
            assertEquals(-i * direction, tree.query(x),
                    mode + " branch=" + (left ? "left" : "right") + " insertion=" + i);
        }
        assertEquals(16, tree.lineCount());
    }
}
