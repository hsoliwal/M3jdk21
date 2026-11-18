// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import jdk.internal.m3.collections.LongCompositeViews;

/** Executable contract oracle for explicit primitive collection operator capabilities. */
public final class LongCompositeOperatorDefaultsSelfTest {
    private static int checks;

    private LongCompositeOperatorDefaultsSelfTest() { }

    public static void main(String[] args) {
        sequenceAdditionIsNotSetUnion();
        defaultSetAlgebraIsLiveAndReadOnly();
        defaultOverrideAndNullContracts();
        randomizedSetOracle();
        System.out.println("PASS M3LongCompositeOperatorDefaults checks=" + checks);
    }

    private static LongTreeSet set(long... values) {
        LongTreeSet result = new LongTreeSet();
        for (long value : values) result.add(value);
        return result;
    }

    private static void sequenceAdditionIsNotSetUnion() {
        LongTreeSet left = set(-2, 1, 3);
        LongTreeSet right = set(1, 4);
        LongCollection concatenated = LongCompositeViews.asSequenceOperator(left).plus(right);
        LongSortedSet united = LongCompositeViews.asSetOperator(left).or(right);
        array(concatenated.toArray(), -2, 1, 3, 1, 4);
        array(united.toArray(), -2, 1, 3, 4);
        check(concatenated.size() == 5);
        check(united.size() == 4);
        left.add(-3);
        right.add(5);
        array(concatenated.toArray(), -3, -2, 1, 3, 1, 4, 5);
        array(united.toArray(), -3, -2, 1, 3, 4, 5);
        throwsType(UnsupportedOperationException.class, concatenated::clear);
        throwsType(UnsupportedOperationException.class, united::clear);
    }

    private static void defaultSetAlgebraIsLiveAndReadOnly() {
        LongTreeSet left = set(Long.MIN_VALUE, -1, 1, 2, 7);
        LongTreeSet right = set(-1, 2, 3, Long.MAX_VALUE);
        LongCompositeViews.SetOperator algebra = LongCompositeViews.asSetOperator(left);
        LongSortedSet intersection = algebra.and(right);
        LongSortedSet difference = algebra.minus(right);
        LongSortedSet symmetricDifference = algebra.xor(right);
        array(intersection.toArray(), -1, 2);
        array(difference.toArray(), Long.MIN_VALUE, 1, 7);
        array(symmetricDifference.toArray(), Long.MIN_VALUE, 1, 3, 7, Long.MAX_VALUE);
        right.remove(-1);
        right.add(7);
        array(intersection.toArray(), 2, 7);
        array(difference.toArray(), Long.MIN_VALUE, -1, 1);
        array(symmetricDifference.toArray(), Long.MIN_VALUE, -1, 1, 3, Long.MAX_VALUE);
        throwsType(UnsupportedOperationException.class, () -> intersection.add(10));
        throwsType(UnsupportedOperationException.class, () -> difference.remove(1));
        throwsType(UnsupportedOperationException.class, symmetricDifference::clear);
    }

    private static void defaultOverrideAndNullContracts() {
        LongTreeSet left = set(1);
        LongTreeSet right = set(2);
        LongCompositeViews.SequenceOperator overridden = new LongCompositeViews.SequenceOperator() {
            @Override public LongCollection source() { return left; }
            @Override public LongCollection plus(LongCollection ignored) { return right; }
        };
        check(overridden.plus(left) == right);
        LongCompositeViews.SetOperator overriddenSet = new LongCompositeViews.SetOperator() {
            @Override public LongSortedSet source() { return left; }
            @Override public LongSortedSet or(LongSortedSet ignored) { return right; }
        };
        check(overriddenSet.or(left) == right);
        throwsType(NullPointerException.class,
                () -> LongCompositeViews.asSequenceOperator(null));
        throwsType(NullPointerException.class,
                () -> LongCompositeViews.asSetOperator(null));
        throwsType(NullPointerException.class,
                () -> LongCompositeViews.asSequenceOperator(left).plus(null));
        LongCompositeViews.SetOperator algebra = LongCompositeViews.asSetOperator(left);
        throwsType(NullPointerException.class, () -> algebra.or(null));
        throwsType(NullPointerException.class, () -> algebra.and(null));
        throwsType(NullPointerException.class, () -> algebra.minus(null));
        throwsType(NullPointerException.class, () -> algebra.xor(null));
        LongCompositeViews.SequenceOperator invalid = () -> null;
        throwsType(NullPointerException.class, () -> invalid.plus(right));
    }

    private static void randomizedSetOracle() {
        Random random = new Random(0x4D335345544F5053L);
        for (int trial = 0; trial < 200; trial++) {
            LongTreeSet a = set();
            LongTreeSet b = set();
            TreeSet<Long> oracleA = new TreeSet<>();
            TreeSet<Long> oracleB = new TreeSet<>();
            for (int i = 0; i < 30; i++) {
                long value = random.nextInt(61) - 30;
                if ((i & 1) == 0) {
                    a.add(value);
                    oracleA.add(value);
                } else {
                    b.add(value);
                    oracleB.add(value);
                }
            }
            LongCompositeViews.SetOperator algebra = LongCompositeViews.asSetOperator(a);
            TreeSet<Long> union = new TreeSet<>(oracleA);
            union.addAll(oracleB);
            array(algebra.or(b).toArray(), primitive(union));
            TreeSet<Long> intersection = new TreeSet<>(oracleA);
            intersection.retainAll(oracleB);
            array(algebra.and(b).toArray(), primitive(intersection));
            TreeSet<Long> difference = new TreeSet<>(oracleA);
            difference.removeAll(oracleB);
            array(algebra.minus(b).toArray(), primitive(difference));
            TreeSet<Long> xor = new TreeSet<>(union);
            xor.removeAll(intersection);
            array(algebra.xor(b).toArray(), primitive(xor));
        }
    }

    private static long[] primitive(TreeSet<Long> values) {
        return values.stream().mapToLong(Long::longValue).toArray();
    }

    private static void array(long[] actual, long... expected) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + ", got " + Arrays.toString(actual));
        }
    }

    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("operator capability contract");
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("expected " + expected.getName(), failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
