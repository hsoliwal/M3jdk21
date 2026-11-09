/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Merge-loss case 12 restore: the trustedSelection fast path of {@link LongBitmapFilterPlan} (ordinals read straight
 * from a privately owned plain BitSet) must give exactly the results of the untrusted byte-mask path, which a BitSet
 * subclass input still takes. Differential over random projections and selections, including the set algebra.
 */
final class LongBitmapFilterPlanTrustedSelectionTest {
    private static final long SEED = 0x4d33545255535431L;
    private static final int ROUNDS = 400;

    /** A BitSet subclass: never trusted, so its plans take the byte-mask path. */
    private static final class ForeignBits extends BitSet {
        private static final long serialVersionUID = 1L;

        ForeignBits(BitSet bits) {
            or(bits);
        }
    }

    private static long[] values(Random random, int n) {
        long[] raw = new long[n];
        for (int i = 0; i < n; i++) {
            raw[i] = random.nextInt(97) - 48L;
        }
        return raw;
    }

    private static BitSet bits(Random random, int n) {
        BitSet bits = new BitSet(n);
        int mode = random.nextInt(4);
        for (int i = 0; i < n; i++) {
            boolean on = switch (mode) {
                case 0 -> random.nextInt(8) == 0;
                case 1 -> random.nextBoolean();
                case 2 -> random.nextInt(8) != 0;
                default -> (i & 3) == 1;
            };
            if (on) {
                bits.set(i);
            }
        }
        return bits;
    }

    private static void same(LongBitmapFilterPlan trusted, LongBitmapFilterPlan foreign, String what) {
        assertEquals(foreign.size(), trusted.size(), what + " size");
        assertArrayEquals(foreign.toArray(), trusted.toArray(), what + " toArray");
        LongIndexProjection a = trusted.projection();
        LongIndexProjection b = foreign.projection();
        assertEquals(b.size(), a.size(), what + " projection size");
        for (int i = 0; i < a.size(); i++) {
            assertEquals(b.get(i), a.get(i), what + " projection get " + i);
            assertEquals(b.physicalIndex(i), a.physicalIndex(i), what + " physical index " + i);
        }
        LongIndexProjection up = trusted.sortedAscending();
        LongIndexProjection upForeign = foreign.sortedAscending();
        for (int i = 0; i < up.size(); i++) {
            assertEquals(upForeign.get(i), up.get(i), what + " sortedAscending " + i);
        }
    }

    @Test
    void trustedFastPathEqualsTheByteMaskPath() {
        Random random = new Random(SEED);
        for (int round = 0; round < ROUNDS; round++) {
            int n = 1 + random.nextInt(round < 40 ? 70 : 1500);
            LongIndexProjection root = LongIndexProjection.snapshot(values(random, n));
            BitSet left = bits(random, n);
            BitSet right = bits(random, n);
            LongBitmapFilterPlan trusted = LongBitmapFilterPlan.fromBits(root, left);
            LongBitmapFilterPlan foreign = LongBitmapFilterPlan.fromBits(root, new ForeignBits(left));
            LongBitmapFilterPlan otherTrusted = LongBitmapFilterPlan.fromBits(root, right);
            LongBitmapFilterPlan otherForeign = LongBitmapFilterPlan.fromBits(root, new ForeignBits(right));
            same(trusted, foreign, "round " + round + " base");
            same(trusted.and(otherTrusted), foreign.and(otherForeign), "round " + round + " and");
            same(trusted.or(otherTrusted), foreign.or(otherForeign), "round " + round + " or");
            same(trusted.xor(otherTrusted), foreign.xor(otherForeign), "round " + round + " xor");
            same(trusted.minus(otherTrusted), foreign.minus(otherForeign), "round " + round + " minus");
            same(trusted.not(), foreign.not(), "round " + round + " not");
            same(trusted.refine(v -> (v & 1L) == 0L), foreign.refine(v -> (v & 1L) == 0L), "round " + round + " refine");
        }
    }

    @Test
    void callerMutationAfterFromBitsDoesNotLeakIntoThePlan() {
        Random random = new Random(SEED ^ 1L);
        LongIndexProjection root = LongIndexProjection.snapshot(values(random, 300));
        BitSet accepted = bits(random, 300);
        LongBitmapFilterPlan plan = LongBitmapFilterPlan.fromBits(root, accepted);
        long[] before = plan.toArray();
        accepted.flip(0, 300);
        assertArrayEquals(before, plan.toArray(), "fromBits must own a private copy of the caller's bits");
    }
}
