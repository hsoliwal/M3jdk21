/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/** Differential source contract: sorted interval algebra and unsigned rank/select. */
public final class M3LongRangeAlgebraContract {
    private static final int DOMAIN = 256;
    private static final long OFFSET = 128L;
    private static long checks;

    private M3LongRangeAlgebraContract() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> error, Runnable call, String description) {
        boolean refused = false;
        try { call.run(); }
        catch (Throwable thrown) {
            if (!error.isInstance(thrown)) {
                throw new AssertionError(description + " unexpected " + thrown, thrown);
            }
            refused = true;
        }
        check(refused, description);
    }

    private static M3LongRangeSet ranges(BitSet expected) {
        M3LongRangeSet actual = new M3LongRangeSet();
        for (int start = expected.nextSetBit(0); start >= 0; ) {
            int end = expected.nextClearBit(start);
            actual.add(start - OFFSET, end - OFFSET);
            start = expected.nextSetBit(end);
        }
        return actual;
    }

    private static void compare(M3LongRangeSet actual, BitSet reference, String reason) {
        actual.verifyInvariants();
        int rank = 0;
        for (int i = 0; i < DOMAIN; i++) {
            long point = i - OFFSET;
            check(actual.contains(point) == reference.get(i), reason + " membership " + point);
            check(actual.rank(point).equals(BigInteger.valueOf(rank)), reason + " rank " + point);
            if (reference.get(i)) {
                check(actual.select(BigInteger.valueOf(rank)) == point, reason + " select " + rank);
                rank++;
            }
        }
        check(actual.cardinality().equals(BigInteger.valueOf(rank)), reason + " total count");
        check(actual.rank(DOMAIN - OFFSET).equals(BigInteger.valueOf(rank)), reason + " upper bound");
        check(actual.rank(-10000L).equals(BigInteger.ZERO), reason + " before lower bound");
        check(actual.rank(10000L).equals(BigInteger.valueOf(rank)), reason + " after upper bound");
        M3LongRangeSet snapshot = actual.snapshot();
        check(Arrays.equals(snapshot.startsToArray(), actual.startsToArray()), reason + " starts detached");
        check(Arrays.equals(snapshot.endsToArray(), actual.endsToArray()), reason + " ends detached");
        expect(IndexOutOfBoundsException.class, () -> actual.select(BigInteger.valueOf(-1)), reason + " negative select");
        expect(IndexOutOfBoundsException.class, () -> actual.select(actual.cardinality()), reason + " end select");
        expect(NullPointerException.class, () -> actual.select(null), reason + " null ordinal");
    }

    private static void indexedAndSetAlgebra() {
        Random random = new Random(0x4d3352494e444558L);
        for (int trial = 0; trial < 900; trial++) {
            BitSet left = new BitSet(DOMAIN);
            BitSet right = new BitSet(DOMAIN);
            for (int op = 0; op < 50; op++) {
                int x = random.nextInt(DOMAIN + 1);
                int y = random.nextInt(DOMAIN + 1);
                int low = Math.min(x, y), high = Math.max(x, y);
                BitSet target = random.nextBoolean() ? left : right;
                target.set(low, high, random.nextBoolean());
            }
            M3LongRangeSet a = ranges(left);
            M3LongRangeSet b = ranges(right);
            if (trial % 7 == 0) {
                compare(a, left, "left-" + trial);
                compare(b, right, "right-" + trial);
            }
            BitSet u = (BitSet) left.clone(); u.or(right);
            BitSet i = (BitSet) left.clone(); i.and(right);
            BitSet d = (BitSet) left.clone(); d.andNot(right);
            BitSet xor = (BitSet) left.clone(); xor.xor(right);
            BitSet complement = (BitSet) left.clone(); complement.flip(0, DOMAIN);
            M3LongRangeSet union = a.union(b);
            M3LongRangeSet intersect = a.intersection(b);
            M3LongRangeSet difference = a.difference(b);
            M3LongRangeSet symmetric = a.symmetricDifference(b);
            M3LongRangeSet complementOfA = a.complement(-128, 128);
            for (int k = 0; k < DOMAIN; k++) {
                long key = k - OFFSET;
                check(union.contains(key) == u.get(k), "union trial " + trial + " key " + key);
                check(intersect.contains(key) == i.get(k), "intersection trial " + trial + " key " + key);
                check(difference.contains(key) == d.get(k), "difference trial " + trial + " key " + key);
                check(symmetric.contains(key) == xor.get(k), "symmetric trial " + trial + " key " + key);
                check(complementOfA.contains(key) == complement.get(k), "complement trial " + trial + " key " + key);
            }
            check(a.union(a).cardinality().equals(a.cardinality()), "idempotent union");
            check(a.intersection(a).cardinality().equals(a.cardinality()), "idempotent intersect");
            check(a.difference(a).isEmpty(), "self difference");
            check(a.symmetricDifference(a).isEmpty(), "self xor");
            check(a.complement(-128, -128).isEmpty(), "empty clip");
            // Independent result must not alias mutable input owners.
            a.add(-1000, -900);
            check(union.rank(-500).equals(BigInteger.ZERO), "detached union snapshot");
            union.verifyInvariants();
            intersect.verifyInvariants();
            difference.verifyInvariants();
            symmetric.verifyInvariants();
            complementOfA.verifyInvariants();
        }
    }

    private static void unsignedWrapAndInvalidation() {
        M3LongRangeSet huge = new M3LongRangeSet();
        huge.add(Long.MIN_VALUE, Long.MAX_VALUE);
        BigInteger all = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger half = BigInteger.ONE.shiftLeft(63);
        check(huge.cardinality().equals(all), "unsigned cardinality 2^64-1");
        check(huge.rank(0L).equals(half), "unsigned cross-zero rank");
        check(huge.select(half) == 0L, "unsigned cross-zero select");
        check(huge.select(BigInteger.ZERO) == Long.MIN_VALUE, "first signed coordinate");
        check(huge.select(all.subtract(BigInteger.ONE)) == Long.MAX_VALUE - 1, "last signed coordinate");
        expect(IndexOutOfBoundsException.class, () -> huge.select(all), "unsigned 2^64-1 select refusal");
        huge.remove(-5, 5);
        check(huge.cardinality().equals(all.subtract(BigInteger.TEN)), "prefix invalidated on removal");
        check(huge.rank(5).equals(half.subtract(BigInteger.valueOf(5))), "rank after split");
        check(huge.select(half.subtract(BigInteger.valueOf(5))) == 5, "select after split");
        huge.add(-5, 5);
        check(huge.cardinality().equals(all), "prefix invalidated on merge");
        huge.clear();
        check(huge.rank(Long.MAX_VALUE).equals(BigInteger.ZERO), "prefix invalidated on clear");
        check(huge.cardinality().equals(BigInteger.ZERO), "clear exact count");
        M3LongRangeSet negative = new M3LongRangeSet();
        negative.add(Long.MIN_VALUE, -1);
        check(negative.rank(-2).equals(half.subtract(BigInteger.valueOf(2))), "negative-only rank");
        check(negative.select(half.subtract(BigInteger.valueOf(2))) == -2, "negative-only select");
        M3LongRangeSet empty = new M3LongRangeSet();
        check(empty.union(negative).cardinality().equals(negative.cardinality()), "union empty");
        check(empty.intersection(negative).isEmpty(), "intersection empty");
        check(empty.difference(negative).isEmpty(), "difference empty");
        check(negative.complement(Long.MIN_VALUE, Long.MAX_VALUE).contains(Long.MAX_VALUE - 1), "bounded complement extreme");
        expect(IllegalArgumentException.class, () -> negative.complement(2, 1), "reversed complement");
        expect(NullPointerException.class, () -> negative.union(null), "null union");
        expect(NullPointerException.class, () -> negative.intersection(null), "null intersection");
        expect(NullPointerException.class, () -> negative.difference(null), "null difference");
        expect(NullPointerException.class, () -> negative.symmetricDifference(null), "null XOR");
    }

    public static void run() {
        checks = 0;
        indexedAndSetAlgebra();
        unsignedWrapAndInvalidation();
        System.out.println("PASS M3LongRangeAlgebraContract checks=" + checks + " trials=900");
    }

    public static void main(String[] args) { run(); }
}
