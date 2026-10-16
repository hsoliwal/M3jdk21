/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

/** Deterministic Java21 semantic oracle, also executable without Maven. */
public final class M3LongCompiledMembershipTest {
    private static long checks;
    public static void main(String[] args) {
        M3LongCompiledMembershipTest t = new M3LongCompiledMembershipTest();
        t.bitmapDomainAndSignedExtrema();
        t.sparseValidationAndOwnership();
        t.existingLiveViewUsesPlan();
        t.differentialRandomized();
        System.out.println("M3_COMPILED_MEMBERSHIP_PASS checks=" + checks);
    }

    @Test public void bitmapDomainAndSignedExtrema() {
        long base = Long.MIN_VALUE;
        long[] words = {0x8000000000000001L, 3L};
        M3LongCompiledMembership plan = M3LongCompiledMembership.fromBitmap(base, 128, words);
        words[0] = 0;
        check(plan.cardinality() == 4 && plan.isBitmap(), "bitmap cardinality");
        check(plan.test(Long.MIN_VALUE) && plan.test(Long.MIN_VALUE + 63), "bit positions");
        check(plan.test(Long.MIN_VALUE + 64) && plan.test(Long.MIN_VALUE + 65), "second word");
        check(!plan.test(Long.MAX_VALUE) && !plan.test(Long.MIN_VALUE + 127), "overflow guard");
        reject(IllegalArgumentException.class, () -> M3LongCompiledMembership.fromBitmap(
                Long.MAX_VALUE, 2, new long[]{1L}));
        reject(IllegalArgumentException.class, () -> M3LongCompiledMembership.fromBitmap(
                0, 65, new long[]{1L, 2L}));
        reject(IllegalArgumentException.class, () -> M3LongCompiledMembership.fromBitmap(
                0, 8193, new long[129]));
        reject(IllegalArgumentException.class, () -> M3LongCompiledMembership.fromBitmap(
                0, 128, new long[]{1L}));
    }

    @Test public void sparseValidationAndOwnership() {
        long[] values = {Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE};
        M3LongCompiledMembership plan = M3LongCompiledMembership.fromSortedKeys(values);
        values[0] = 7L;
        check(plan.test(Long.MIN_VALUE) && plan.test(0), "defensive sparse copy");
        check(!plan.test(7L) && plan.test(Long.MAX_VALUE), "sparse extrema");
        check(!plan.isBitmap() && plan.primitiveBytes() == 32, "sparse layout");
        check(!M3LongCompiledMembership.fromSortedKeys(new long[0]).test(4L), "empty");
        reject(IllegalArgumentException.class, () ->
                M3LongCompiledMembership.fromSortedKeys(new long[]{1, 1}));
        reject(IllegalArgumentException.class, () ->
                M3LongCompiledMembership.fromSortedKeys(new long[]{2, 1}));
        reject(NullPointerException.class, () -> M3LongCompiledMembership.fromSortedKeys(null));
        reject(NullPointerException.class, () -> M3LongCompiledMembership.fromBitmap(0, 1, null));
    }

    @Test public void existingLiveViewUsesPlan() {
        MutableLongs source = new MutableLongs(-2, -1, 0, 1, 2, 3);
        LongPredicate predicate = M3LongCompiledMembership.fromBitmap(-2, 6,
                new long[]{(1L << 1) | (1L << 3) | (1L << 5)});
        M3LongFilteredView filtered = new M3LongFilteredView(source, predicate);
        check(Arrays.equals(filtered.toArray(), new long[]{-1, 1, 3}), "no new wrapper engine");
        check(Arrays.equals(filtered.longStream().toArray(), filtered.toArray()), "stream integration");
        source.append(1);
        check(Arrays.equals(filtered.toArray(), new long[]{-1, 1, 3, 1}), "live and duplicates");
    }

    @Test public void differentialRandomized() {
        Random random = new Random(0x4d33434f4d50494cL);
        for (int round = 0; round < 1000; round++) {
            long base = random.nextLong() >>> 1;
            int span = 1 + random.nextInt(511);
            if (base > Long.MAX_VALUE - (span - 1L)) base = Long.MAX_VALUE - span;
            long[] words = new long[(span + 63) >>> 6];
            for (int i=0;i<span;i++) if (random.nextBoolean()) words[i >>> 6] |= 1L << (i & 63);
            M3LongCompiledMembership plan = M3LongCompiledMembership.fromBitmap(base, span, words);
            for (int i=0;i<650;i++) {
                long probe = i < span ? base + i : random.nextLong();
                long diff = probe - base;
                boolean expected = diff >= 0 && diff < span
                        && (words[(int) diff >>> 6] & (1L << ((int) diff & 63))) != 0;
                check(plan.test(probe) == expected, "random bitmap equivalence");
            }
        }
    }

    private static void check(boolean ok, String why) {
        checks++; if (!ok) throw new AssertionError(why);
    }
    private static void reject(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("wrong exception " + t, t);
        }
        throw new AssertionError("expected " + type.getName());
    }
    private static final class MutableLongs implements M3LongCollection {
        private long[] items;
        MutableLongs(long... values) { items = values.clone(); }
        void append(long value) {
            items = Arrays.copyOf(items, items.length + 1);
            items[items.length - 1] = value;
        }
        @Override public int size() { return items.length; }
        @Override public boolean contains(long v) {
            for (long element:items) if (element==v) return true;
            return false;
        }
        @Override public void clear() { items = new long[0]; }
        @Override public long[] toArray() { return items.clone(); }
        @Override public M3LongIterator iterator() {
            return new M3LongIterator() {
                int i;
                @Override public boolean hasNext() { return i < items.length; }
                @Override public long nextLong() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return items[i++];
                }
            };
        }
    }
}
