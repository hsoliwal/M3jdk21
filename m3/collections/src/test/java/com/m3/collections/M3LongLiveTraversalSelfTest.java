/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * Standalone regression on the actual target interfaces and wrapper implementations.
 * MutableSorted is a deliberately instrumented test owner, not a replacement runtime class.
 * Its iterator snapshots on acquisition, making premature acquisition directly observable.
 */
public final class M3LongLiveTraversalSelfTest {
    private static long checks;
    private static long digest = 0x4d334c495645L;
    private static final int SORTED = Spliterator.ORDERED | Spliterator.SORTED
            | Spliterator.DISTINCT | Spliterator.NONNULL;
    private M3LongLiveTraversalSelfTest() { }
    @FunctionalInterface private interface Action { void run(); }

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    private static void same(long[] actual, long[] expected, String label) {
        check(Arrays.equals(actual, expected), label + " actual=" + Arrays.toString(actual)
                + " expected=" + Arrays.toString(expected));
        for (long value : actual) digest = Long.rotateLeft(digest, 7) ^ value;
        digest ^= actual.length;
    }
    private static void expect(Class<? extends Throwable> type, Action action, String label) {
        try { action.run(); }
        catch (Throwable error) {
            check(type.isInstance(error), label + " wrong failure: " + error);
            return;
        }
        throw new AssertionError(label + " did not fail");
    }
    private static long[] drain(Spliterator.OfLong spliterator) {
        return StreamSupport.longStream(spliterator, false).toArray();
    }
    private static long fallback(Long value) { return value == null ? 0x12345678L : value; }

    private static final class MutableSorted implements M3LongSortedSet {
        final TreeSet<Long> values = new TreeSet<>();
        int opens, sizes, reads, polls;
        RuntimeException openFailure;
        String id;
        List<String> trace;
        MutableSorted(long... values) { for (long value : values) this.values.add(value); }
        public int size() { sizes++; return values.size(); }
        public boolean contains(long value) { return values.contains(value); }
        public void clear() { values.clear(); }
        public long[] toArray() { return values.stream().mapToLong(Long::longValue).toArray(); }
        public boolean add(long value) { return values.add(value); }
        public boolean remove(long value) { return values.remove(value); }
        public long first() { return values.first(); }
        public long last() { return values.last(); }
        public long floor(long value, long defaultValue) { Long x=values.floor(value); return x==null?defaultValue:x; }
        public long ceiling(long value, long defaultValue) { Long x=values.ceiling(value); return x==null?defaultValue:x; }
        public long lower(long value, long defaultValue) { Long x=values.lower(value); return x==null?defaultValue:x; }
        public long higher(long value, long defaultValue) { Long x=values.higher(value); return x==null?defaultValue:x; }
        public int filterInPlace(LongPredicate keep) {
            Objects.requireNonNull(keep, "keep");
            int before=values.size(); values.removeIf(v -> !keep.test(v)); return before-values.size();
        }
        public M3LongIterator iterator() {
            opens++;
            if (trace != null) trace.add(id + ":open");
            if (openFailure != null) { RuntimeException e=openFailure; openFailure=null; throw e; }
            long[] snapshot = toArray();
            return new M3LongIterator() {
                int index;
                public boolean hasNext() { polls++; return index < snapshot.length; }
                public long nextLong() {
                    if (index == snapshot.length) throw new NoSuchElementException();
                    reads++;
                    long value=snapshot[index++];
                    if (trace != null) trace.add(id + ":" + value);
                    return value;
                }
            };
        }
    }

    private static M3LongCollection view(String family, MutableSorted a, MutableSorted b, LongPredicate p) {
        return switch(family) {
            case "default" -> a;
            case "filtered" -> new M3LongFilteredView(a,p);
            case "sorted" -> M3LongSortedViews.filter(a,p);
            case "unmodifiable" -> M3LongSortedViews.unmodifiable(a);
            case "range" -> M3LongSortedViews.subSet(M3LongSortedViews.filter(a,p),-8,20);
            case "head" -> M3LongSortedViews.headSet(M3LongSortedViews.filter(a,p),20);
            case "tail" -> M3LongSortedViews.tailSet(M3LongSortedViews.filter(a,p),-8);
            case "concat" -> M3LongCompositeViews.concat(a,b,a);
            case "union" -> M3LongCompositeViews.sortedUnion(M3LongSortedViews.filter(a,p),b);
            case "nested" -> M3LongSortedViews.unmodifiable(M3LongSortedViews.filter(
                    M3LongCompositeViews.sortedUnion(M3LongSortedViews.filter(a,p),b),v->true));
            default -> throw new IllegalArgumentException(family);
        };
    }
    private static long[] expected(String family, MutableSorted a, MutableSorted b, long threshold) {
        LongStream stream=Arrays.stream(a.toArray());
        if (List.of("filtered","sorted","range","head","tail","union","nested").contains(family)) {
            stream=stream.filter(v->v>=threshold);
        }
        if (family.equals("range")) stream=stream.filter(v->v>=-8 && v<20);
        if (family.equals("head")) stream=stream.filter(v->v<20);
        if (family.equals("tail")) stream=stream.filter(v->v>=-8);
        if (family.equals("concat")) return LongStream.concat(LongStream.concat(
                Arrays.stream(a.toArray()),Arrays.stream(b.toArray())),Arrays.stream(a.toArray())).toArray();
        if (family.equals("union") || family.equals("nested")) {
            return LongStream.concat(stream,Arrays.stream(b.toArray())).sorted().distinct().toArray();
        }
        return stream.toArray();
    }
    private static void lazy(String family) {
        MutableSorted a=new MutableSorted(-3,1,5), b=new MutableSorted(0,1,8);
        long[] threshold={Long.MIN_VALUE}; int[] predicateCalls={0};
        M3LongCollection view=view(family,a,b,v->{predicateCalls[0]++;return v>=threshold[0];});
        Spliterator.OfLong cursor=view.longSpliterator();
        check(a.opens+b.opens==0 && predicateCalls[0]==0,"LAZY_CONSTRUCTION:"+family);
        int flags=family.equals("concat")?Spliterator.ORDERED|Spliterator.NONNULL
                :List.of("default","filtered").contains(family)?Spliterator.NONNULL:SORTED;
        check(cursor.characteristics()==flags,"METADATA:"+family);
        check(cursor.estimateSize()==Long.MAX_VALUE && cursor.getExactSizeIfKnown()==-1,"UNKNOWN_SIZE:"+family);
        check(cursor.trySplit()==null,"NO_SPLIT_BUFFER:"+family);
        if ((flags&Spliterator.SORTED)!=0) check(cursor.getComparator()==null,"NATURAL_COMPARATOR:"+family);
        else expect(IllegalStateException.class,cursor::getComparator,"UNSORTED_COMPARATOR:"+family);
        expect(NullPointerException.class,()->cursor.tryAdvance((LongConsumer)null),"NULL_ACTION:"+family);
        expect(NullPointerException.class,()->cursor.forEachRemaining((LongConsumer)null),"NULL_BULK:"+family);
        check(a.opens+b.opens==0 && predicateCalls[0]==0 && a.sizes+b.sizes==0,"METADATA_IS_LAZY:"+family);
        check(view.longStream().limit(0).count()==0,"ZERO_LIMIT:"+family);
        try (LongStream unopened=view.parallelLongStream()) { check(unopened.isParallel(),"PARALLEL_FLAG"); }
        check(a.opens+b.opens==0 && predicateCalls[0]==0,"UNUSED_STREAM_IS_LAZY:"+family);
        a.add(2); a.add(17); a.remove(-3); b.clear(); b.add(7); threshold[0]=2;
        same(drain(cursor),expected(family,a,b,2),"PRE_BIND_MUTATION:"+family);
        check(a.sizes+b.sizes==0,"NO_SIZE_SCAN:"+family);
        check(cursor.characteristics()==flags,"STABLE_METADATA:"+family);
        // A separate stream must bind separately and observe a subsequent owner mutation.
        LongStream parallel=view.parallelLongStream(); a.add(19); b.add(-1);
        same(parallel.toArray(),expected(family,a,b,2),"PARALLEL_LATE_BIND:"+family);
    }
    private static void failures() {
        MutableSorted source=new MutableSorted(1,2,3);
        Spliterator.OfLong cursor=M3LongSortedViews.unmodifiable(source).longSpliterator();
        RuntimeException fault=new IllegalStateException("injected acquisition failure");source.openFailure=fault;
        try { cursor.tryAdvance((LongConsumer)v->{}); throw new AssertionError("acquisition did not fail"); }
        catch (RuntimeException e) { check(e==fault,"SOURCE_EXCEPTION_IDENTITY"); }
        check(source.opens==1 && source.reads==0,"FAILED_BIND_NO_READ");
        same(drain(cursor),new long[]{1,2,3},"RETRY_FAILED_ACQUISITION");
        check(source.opens==2,"RETRY_BINDS_ONCE");
        source=new MutableSorted(1,2,3);
        Spliterator.OfLong actionCursor=M3LongSortedViews.filter(source,v->true).longSpliterator();
        try { actionCursor.tryAdvance((LongConsumer)v->{throw fault;}); throw new AssertionError("action did not fail"); }
        catch (RuntimeException e) { check(e==fault,"ACTION_EXCEPTION_IDENTITY"); }
        same(drain(actionCursor),new long[]{2,3},"ACTION_FAILURE_CONSUMES_ONCE");
    }
    private static void callbacks() {
        List<String> trace=new ArrayList<>();
        MutableSorted a=new MutableSorted(1,2),b=new MutableSorted(1,3),c=new MutableSorted(0,4);
        MutableSorted[] inputs={a,b,c};
        for(int i=0;i<inputs.length;i++){inputs[i].trace=trace;inputs[i].id=""+(char)('A'+i);}
        M3LongIterator iterator=M3LongCompositeViews.sortedUnion(inputs).iterator();
        check(trace.equals(List.of("A:open","A:1","B:open","B:1","C:open","C:0")),"MERGE_OPEN_ORDER");
        check(iterator.hasNext() && iterator.hasNext(),"LOOKAHEAD_IDEMPOTENT");
        check(iterator.nextLong()==0,"SIGNED_HEAP_FIRST");
        List<Long> out=new ArrayList<>();iterator.forEachRemaining(out::add);
        check(out.equals(List.of(1L,2L,3L,4L)),"DISTINCT_MERGE");
        check(trace.equals(List.of("A:open","A:1","B:open","B:1","C:open","C:0","C:4","A:2","B:3")),"MERGE_TIE_AND_READ_ORDER");
        check(!iterator.hasNext() && !iterator.hasNext(),"TERMINAL_ITERATOR");
        expect(NoSuchElementException.class,iterator::nextLong,"ITERATOR_EXHAUSTION");
        MutableSorted owner=new MutableSorted(-2,-1,0,1,2); List<String> observed=new ArrayList<>();
        M3LongSortedSet nested=M3LongSortedViews.filter(M3LongSortedViews.filter(owner,
                v->{observed.add("A"+v);return v>=0;}),v->{observed.add("B"+v);return (v&1)==0;});
        same(nested.toArray(),new long[]{0,2},"FILTER_COMPOSITION");
        check(observed.equals(List.of("A-2","A-1","A0","B0","A1","B1","A2","B2")),"PREDICATE_SHORT_CIRCUIT_ORDER");
    }
    private static void seededParity() {
        Random random=new Random(0x4d334c415a59L);
        for (int trial=0;trial<600;trial++) {
            int k=trial<4?trial:random.nextInt(24);
            MutableSorted[] inputs=new MutableSorted[k]; TreeSet<Long> oracle=new TreeSet<>();
            for(int i=0;i<k;i++) {
                inputs[i]=new MutableSorted();
                int n=random.nextInt(24);
                for(int j=0;j<n;j++) {
                    long value=random.nextInt(51)-25;
                    if(j==0 && (i&3)==0)value=Long.MIN_VALUE;
                    if(j==1 && (i&3)==1)value=Long.MAX_VALUE;
                    inputs[i].add(value); oracle.add(value);
                }
            }
            M3LongSortedSet union=M3LongCompositeViews.sortedUnion(inputs);
            long[] expected=oracle.stream().mapToLong(Long::longValue).toArray();
            same(union.toArray(),expected,"UNION_ARRAY:"+trial);
            same(union.longStream().toArray(),expected,"UNION_STREAM:"+trial);
            same(union.parallelLongStream().toArray(),expected,"UNION_PARALLEL:"+trial);
            check(union.size()==expected.length && union.isEmpty()==oracle.isEmpty(),"UNION_SIZE:"+trial);
            if(oracle.isEmpty()) {
                expect(NoSuchElementException.class,union::first,"EMPTY_FIRST");
                expect(NoSuchElementException.class,union::last,"EMPTY_LAST");
            } else check(union.first()==oracle.first() && union.last()==oracle.last(),"FIRST_LAST");
            for(long q:new long[]{Long.MIN_VALUE,Long.MIN_VALUE+1,-26,-1,0,1,26,Long.MAX_VALUE-1,Long.MAX_VALUE}) {
                check(union.contains(q)==oracle.contains(q),"CONTAINS");
                check(union.lower(q,0x12345678L)==fallback(oracle.lower(q)),"LOWER");
                check(union.floor(q,0x12345678L)==fallback(oracle.floor(q)),"FLOOR");
                check(union.ceiling(q,0x12345678L)==fallback(oracle.ceiling(q)),"CEILING");
                check(union.higher(q,0x12345678L)==fallback(oracle.higher(q)),"HIGHER");
            }
            M3LongSortedSet filtered=M3LongSortedViews.filter(union,v->(v&1)==0);
            same(filtered.toArray(),Arrays.stream(expected).filter(v->(v&1)==0).toArray(),"FILTERED_UNION");
            same(M3LongSortedViews.subSet(filtered,-8,9).longStream().toArray(),
                    Arrays.stream(expected).filter(v->(v&1)==0 && v>=-8 && v<9).toArray(),"RANGED_UNION");
            M3LongCollection concat=M3LongCompositeViews.concat(inputs);
            LongStream expectedConcat=LongStream.empty();
            for(MutableSorted input:inputs)expectedConcat=LongStream.concat(expectedConcat,Arrays.stream(input.toArray()));
            long[] concatenated=expectedConcat.toArray();
            same(concat.toArray(),concatenated,"CONCAT_DUPLICATES");
            same(new M3LongFilteredView(concat,v->v>=0).longStream().toArray(),
                    Arrays.stream(concatenated).filter(v->v>=0).toArray(),"GENERIC_CONCAT_FILTER");
        }
    }
    private static void mutations() {
        MutableSorted owner=new MutableSorted(-3,-2,-1,0,1,2,3);
        M3LongSortedSet even=M3LongSortedViews.filter(owner,v->(v&1)==0);
        check(!even.remove(1) && owner.contains(1),"HIDDEN_REMOVE");
        expect(IllegalArgumentException.class,()->even.add(5),"REJECTED_ADD");
        check(even.add(4) && owner.contains(4),"ACCEPTED_ADD");
        check(even.filterInPlace(v->v>=0)==1 && !owner.contains(-2) && owner.contains(-3),"FILTER_IN_PLACE_HIDDEN");
        even.clear();same(owner.toArray(),new long[]{-3,-1,1,3},"CLEAR_PRESERVES_HIDDEN");
        M3LongSortedSet readOnly=M3LongSortedViews.unmodifiable(owner);
        check(readOnly==M3LongSortedViews.unmodifiable(readOnly),"UNMODIFIABLE_IDEMPOTENT");
        expect(UnsupportedOperationException.class,()->readOnly.add(99),"READ_ONLY_ADD");
        expect(UnsupportedOperationException.class,()->readOnly.remove(99),"READ_ONLY_MISSING_REMOVE");
        expect(UnsupportedOperationException.class,readOnly::clear,"READ_ONLY_CLEAR");
        expect(UnsupportedOperationException.class,()->readOnly.filterInPlace(null),"READ_ONLY_NULL_FILTER");
        M3LongSortedSet union=M3LongCompositeViews.sortedUnion(owner);
        expect(UnsupportedOperationException.class,union::clear,"UNION_CLEAR");
        expect(UnsupportedOperationException.class,()->union.remove(99),"UNION_REMOVE");
        expect(UnsupportedOperationException.class,()->union.add(99),"UNION_ADD");
        expect(UnsupportedOperationException.class,()->union.filterInPlace(v->true),"UNION_FILTER");
        M3LongSortedSet[] sources={owner}; M3LongSortedSet detachedInputs=M3LongCompositeViews.sortedUnion(sources);
        sources[0]=new MutableSorted(100);same(detachedInputs.toArray(),owner.toArray(),"INPUT_ARRAY_CLONED");
        same(M3LongSortedViews.subSet(owner,0,0).toArray(),new long[0],"EMPTY_RANGE");
        expect(IllegalArgumentException.class,()->M3LongSortedViews.subSet(owner,1,-1),"REVERSED_RANGE");
        expect(NullPointerException.class,()->M3LongCompositeViews.sortedUnion((M3LongSortedSet[])null),"NULL_INPUTS");
        expect(NullPointerException.class,()->M3LongCompositeViews.concat(owner,null),"NULL_MEMBER");
    }
    public static void main(String[] args) {
        if(args.length==2 && args[0].equals("--lazy")) {
            lazy(args[1]);System.out.println("LAZY_PASS family="+args[1]+" checks="+checks);return;
        }
        if(args.length>0 && args[0].equals("--lazy-all")) {
            for(String family:List.of("default","filtered","sorted","unmodifiable","range","head","tail","concat","union","nested"))lazy(family);
            failures();System.out.println("LAZY_ALL_PASS checks="+checks);return;
        }
        callbacks();seededParity();mutations();
        if(args.length==0 || !args[0].equals("--parity")) {
            for(String family:List.of("default","filtered","sorted","unmodifiable","range","head","tail","concat","union","nested"))lazy(family);
            failures();
        }
        System.out.println("M3_LONG_LIVE_PASS checks="+checks+" parityDigest="+Long.toUnsignedString(digest,16));
    }
}
