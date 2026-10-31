// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;

/** Standalone Java21/JDK TreeSet differential proof, callable by a source-sealed CI recipe. */
public final class M3LongSortedSnapshotSelfTest {
    private static long assertions;
    private static final long FALLBACK = 443_798_166_492L;

    private M3LongSortedSnapshotSelfTest() {}

    public static void main(String[] arguments) {
        long[] boundaries = {Long.MIN_VALUE, -100, -2, 0, 3, 5, Long.MAX_VALUE};
        MutableSorted source = new MutableSorted();
        for (long value : boundaries) source.add(value);
        AtomicInteger evaluated = new AtomicInteger();
        LongPredicate predicate = value -> { evaluated.incrementAndGet(); return value != -2 && value != 3; };
        M3LongSortedSnapshot selected = M3LongSortedSnapshot.filtered(source, predicate);
        eq(boundaries.length, evaluated.get(), "predicate one evaluation per backing item");
        source.clear();
        array(new long[] {Long.MIN_VALUE, -100, 0, 5, Long.MAX_VALUE}, selected.toArray(), "snapshot detached");
        eq(0, selected.rank(Long.MIN_VALUE), "first rank");
        eq(4, selected.rank(Long.MAX_VALUE), "last rank");
        eq(4, selected.countRange(Long.MIN_VALUE, Long.MAX_VALUE), "count range excludes max");
        eq(Long.MIN_VALUE, selected.select(0), "min select");
        eq(Long.MAX_VALUE, selected.select(4), "max select");
        eq(4, selected.indexOf(Long.MAX_VALUE), "max index");
        eq(-1, selected.indexOf(3), "hidden absent");
        eq(443_798_166_492L, selected.lower(Long.MIN_VALUE, FALLBACK), "lower empty fallback");
        eq(Long.MIN_VALUE, selected.floor(Long.MIN_VALUE, FALLBACK), "floor min");
        eq(Long.MAX_VALUE, selected.ceiling(Long.MAX_VALUE, FALLBACK), "ceiling max");
        eq(FALLBACK, selected.higher(Long.MAX_VALUE, FALLBACK), "higher empty fallback");
        array(new long[] {0, 5}, selected.subSet(-1, 10).toArray(), "range subset");
        array(new long[] {Long.MIN_VALUE, -100}, selected.headSet(0).toArray(), "range head");
        array(new long[] {5, Long.MAX_VALUE}, selected.tailSet(1).toArray(), "range tail");
        eq(selected.retainedPrimitiveBytes(), selected.subSet(-1,10).retainedPrimitiveBytes(), "share retained primitive body");
        selected.toArray()[0] = 77;
        eq(Long.MIN_VALUE, selected.first(), "returned array defensive");
        eq(0, evaluated.get() - boundaries.length, "no predicate during query");
        expect(UnsupportedOperationException.class, () -> selected.clear(), "immutable clear");
        expect(UnsupportedOperationException.class, () -> selected.add(7), "immutable add");
        expect(UnsupportedOperationException.class, () -> selected.remove(5), "immutable remove");
        expect(UnsupportedOperationException.class, () -> selected.filterInPlace(v -> true), "immutable retain");
        expect(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromSorted(new long[] {7,7}), "duplicate rejected");
        expect(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromSorted(new long[] {7,6}), "inversion rejected");
        expect(IndexOutOfBoundsException.class, () -> selected.select(5), "rank out of range");
        expect(IllegalArgumentException.class, () -> selected.subSet(3,2), "reversed range refused");
        M3LongSortedSnapshot empty = M3LongSortedSnapshot.fromSorted(new long[0]);
        expect(NoSuchElementException.class, empty::first, "empty first");
        expect(NoSuchElementException.class, empty::last, "empty last");
        eq(0, empty.rank(2), "empty rank");
        var iter = empty.iterator();
        expect(NoSuchElementException.class, iter::nextLong, "empty primitive iterator");
        M3LongSortedSnapshot mask = M3LongSortedSnapshot.fromAlignedMask(
                new long[] {-4,-1,0,1,7},new byte[] {1,0,3,2,7},1);
        array(new long[] {-4,0,7}, mask.toArray(), "existing mask compactor reusable");
        expect(IllegalArgumentException.class,
                () -> M3LongSortedSnapshot.fromAlignedMask(new long[]{1}, new byte[0],1), "unaligned mask");
        expect(IllegalArgumentException.class,
                () -> M3LongSortedSnapshot.fromAlignedMask(new long[]{1}, new byte[]{1},256), "invalid mask");
        int traits = selected.longSpliterator().characteristics();
        ok((traits & (Spliterator.SORTED|Spliterator.DISTINCT|Spliterator.SIZED|Spliterator.SUBSIZED|Spliterator.IMMUTABLE))
                == (Spliterator.SORTED|Spliterator.DISTINCT|Spliterator.SIZED|Spliterator.SUBSIZED|Spliterator.IMMUTABLE), "honest snapshot characteristics");
        eq(null, selected.longSpliterator().getComparator(), "natural comparator");
        array(selected.toArray(), selected.parallelLongStream().toArray(), "parallel split maintains order");

        Random random = new Random(0x3a5e17L);
        for (int round=0; round<256; round++) {
            MutableSorted mutable = new MutableSorted();
            for (int index=0; index<100 + round; index++) mutable.add(random.nextLong(500)-250L);
            M3LongSortedSnapshot snapshot = M3LongSortedSnapshot.filtered(mutable,v -> (v & 3L)==0);
            TreeSet<Long> oracle = new TreeSet<>();
            for(long v: mutable.set) if((v & 3L)==0) oracle.add(v);
            long[] expected=oracle.stream().mapToLong(Long::longValue).toArray();
            array(expected,snapshot.toArray(),"random membership");
            for(int i=0;i<300;i++) {
                long key=random.nextLong(600)-300L;
                eq(oracle.headSet(key,false).size(),snapshot.rank(key),"random rank");
                eq(oracle.contains(key),snapshot.contains(key),"random contains");
                eq(orFallback(oracle.lower(key)),snapshot.lower(key,FALLBACK),"random lower");
                eq(orFallback(oracle.floor(key)),snapshot.floor(key,FALLBACK),"random floor");
                eq(orFallback(oracle.ceiling(key)),snapshot.ceiling(key,FALLBACK),"random ceiling");
                eq(orFallback(oracle.higher(key)),snapshot.higher(key,FALLBACK),"random higher");
            }
            for (int i=0;i<expected.length;i++) eq(expected[i],snapshot.select(i),"random select");
        }
        System.out.println("M3_LONG_SORTED_SNAPSHOT_PASS|assertions=" + assertions + "|randomRounds=256");
    }

    private static long orFallback(Long value){return value == null ? FALLBACK : value;}
    private static void ok(boolean condition,String label){assertions++; if(!condition)throw new AssertionError(label);}
    private static void eq(long expected,long actual,String label){ok(expected==actual,label+" expected="+expected+" actual="+actual);}
    private static void eq(boolean expected,boolean actual,String label){ok(expected==actual,label);}
    private static void eq(Object expected,Object actual,String label){ok(Objects.equals(expected,actual),label);}
    private static void array(long[] expected,long[] actual,String label){assertions++; if(!Arrays.equals(expected,actual)) throw new AssertionError(label+" expected="+Arrays.toString(expected)+" actual="+Arrays.toString(actual));}
    private static void expect(Class<? extends Throwable> clazz,Runnable callback,String label){ assertions++; try{callback.run();}catch(Throwable found){if(clazz.isInstance(found))return;throw new AssertionError(label+" wrong exception",found);} throw new AssertionError(label+" did not reject");}

    private static final class MutableSorted implements M3LongSortedSet {
        private final NavigableSet<Long> set=new TreeSet<>();
        @Override public int size(){return set.size();}
        @Override public boolean contains(long value){return set.contains(value);}
        @Override public boolean add(long value){return set.add(value);}
        @Override public boolean remove(long value){return set.remove(value);}
        @Override public void clear(){set.clear();}
        @Override public int filterInPlace(LongPredicate keep){int before=set.size();set.removeIf(v->!keep.test(v));return before-set.size();}
        @Override public long[] toArray(){return set.stream().mapToLong(Long::longValue).toArray();}
        @Override public M3LongIterator iterator(){var cursor=set.iterator();return new M3LongIterator(){@Override public boolean hasNext(){return cursor.hasNext();}@Override public long nextLong(){return cursor.next();}};}
        @Override public long first(){return set.first();}
        @Override public long last(){return set.last();}
        @Override public long lower(long value,long fallback){return orFallback(set.lower(value));}
        @Override public long floor(long value,long fallback){return orFallback(set.floor(value));}
        @Override public long ceiling(long value,long fallback){return orFallback(set.ceiling(value));}
        @Override public long higher(long value,long fallback){return orFallback(set.higher(value));}
    }
}
