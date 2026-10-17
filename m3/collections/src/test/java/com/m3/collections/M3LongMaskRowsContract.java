// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

/** Standalone deterministic differential oracle for parent-owned DOOP mask views. */
public final class M3LongMaskRowsContract {
    private static long assertions;
    private M3LongMaskRowsContract() { }

    public static void main(String[] args) {
        if (Boolean.getBoolean("m3.mask.requireNative")
                && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("required first-party M3 mask JNI binary unavailable");
        }
        fixedCases();
        nestedOverlay();
        lateBindingAndDrift();
        randomizedDifferential();
        budgetAndStress();
        System.out.println("M3_MASK_ROWS_ASSERTIONS=" + assertions + " RESULT=PASS");
    }

    private static void fixedCases() {
        long[] values = {Long.MIN_VALUE, 4, 0, 8, -1, Long.MAX_VALUE};
        byte[] flags = {1, 3, 0, 7, (byte) 0xff, 2};
        SimpleOwner parent = new SimpleOwner(values);
        M3LongMaskRows root = M3LongMaskRows.from(parent, flags);
        flags[0] = (byte) 0xff;
        yes(root.sourceStart() == 0 && root.sourceEnd() == values.length, "root source positions");
        eq(values, root.view().toArray(), "zero-mask sees original values");
        eq(new int[]{0, 1, 2, 3, 4, 5}, root.selectedSourceRows(M3IndexMaskCompactor.Backend.JAVA), "all row ids");
        M3LongMaskRows subset = root.requireFlags(1).sourceRange(1, 5);
        eq(new int[] {1,3,4}, subset.selectedSourceRows(M3IndexMaskCompactor.Backend.JAVA), "subset row ids");
        eq(new long[]{4,8,-1}, subset.view().toArray(), "subset values");
        eq(new long[]{-1,4,8}, subset.dag().sortedBounded(3).toArrayBounded(3), "DAG bounded signed sort");
        if(M3IndexMaskCompactor.nativeAvailable()) {
            eq(new int[]{1,3,4}, subset.selectedSourceRows(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "original JNI candidate parity");
        }
        yes(subset.requiredFlags() == 1 && subset.selectedCount() == 3, "mask count");
        yes(subset.view().contains(8) && !subset.view().contains(0), "contains scans visible rows");
        yes(subset.view().longStream().sum() == 11, "LongStream interop");
        yes(subset.dag().toArrayBounded(3).length == 3, "DAG owner adapter");
        eq(new long[]{9}, subset.dag().filter(v -> v >= 8).map(v -> v + 1)
                .limit(1).toArrayBounded(1), "actual M3LongDag primitive filter/map/limit");
        long[] sum = subset.dag().virtualAccumulator(() -> new long[1],
                (acc, value) -> acc[0] += value).evaluate();
        yes(sum[0] == 11, "actual virtual accumulator integrates with mask row facade");
        long finished = subset.dag().virtualCollector(() -> new long[1],
                (acc, value) -> acc[0] += value, acc -> acc[0]).evaluate();
        yes(finished == 11, "actual virtual collector integrates with facade");
        raises(UnsupportedOperationException.class, () -> subset.view().clear(), "read-only mutation");
        raises(IndexOutOfBoundsException.class, () -> subset.sourceRange(0, 4), "cannot escape parent range");
        raises(IndexOutOfBoundsException.class, () -> subset.sourceRange(2, 6), "cannot expand parent end");
        raises(IllegalArgumentException.class, () -> subset.requireFlags(256), "out of range mask");
        raises(IllegalArgumentException.class, () -> subset.requireFlags(-1), "negative mask");
        raises(IllegalArgumentException.class, () -> M3LongMaskRows.from(parent, new byte[2]), "misaligned initial states");
        raises(NullPointerException.class, () -> M3LongMaskRows.from(null, flags), "nonnull owner");
        raises(NullPointerException.class, () -> M3LongMaskRows.from(parent, null), "nonnull byte states");
        raises(NullPointerException.class, () -> subset.selectedSourceRows(null), "nonnull backend");
        if(!M3IndexMaskCompactor.nativeAvailable()) {
            raises(UnsatisfiedLinkError.class, () -> subset.selectedSourceRows(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "native strict requirement");
        }
        M3LongMaskRows empty = subset.sourceRange(3, 3);
        eq(new long[0], empty.view().toArray(), "zero length range");
        yes(empty.view().isEmpty(), "empty view");
        raises(NoSuchElementException.class, () -> empty.view().iterator().nextLong(), "empty iterator next");
    }

    private static void nestedOverlay() {
        long[] values = {0, 1, 2, 3, 4, 5, 6, 7};
        byte[] mask = {(byte)0xff, 0, 1, 2, 3, 4, 5, 7};
        M3LongMaskRows root = M3LongMaskRows.from(new SimpleOwner(values), mask);
        M3LongMaskRows first = root.requireFlags(1).sourceRange(2,8);
        M3LongMaskRows second = first.requireFlags(2).sourceRange(4, 8);
        eq(new long[] {4,7}, second.view().toArray(), "conjunctive composed bits plus offsets");
        eq(new long[] {2,4,6,7}, first.view().toArray(), "derived branch leaves parent unchanged");
        eq(values, root.view().toArray(), "original branch unchanged");
        yes(first.requiredFlags()==1 && second.requiredFlags()==3, "flag OR composition");
        Spliterator.OfLong split = second.view().longSpliterator();
        yes(split.trySplit()==null, "no unsupported parallel split");
        yes((split.characteristics() & Spliterator.SIZED)==0, "live projection is not SIZED");
        yes((split.characteristics() & Spliterator.ORDERED)!=0, "source order kept");
    }

    private static void lateBindingAndDrift() {
        SimpleOwner source = new SimpleOwner(new long[]{8,9,10});
        M3LongMaskRows plan = M3LongMaskRows.from(source, new byte[]{1,1,0}).requireFlags(1);
        LongStream late = plan.view().longStream();
        source.set(0, 70);
        try(late){ eq(new long[]{70,9}, late.toArray(), "late-binding same-count payload update"); }
        source.add(11);
        raises(ConcurrentModificationException.class, plan::selectedCount, "cardinality drift rejects counting");
        raises(ConcurrentModificationException.class, () -> plan.view().iterator(), "cardinality drift rejects iterator");
        raises(ConcurrentModificationException.class, () -> plan.selectedSourceRows(M3IndexMaskCompactor.Backend.JAVA), "cardinality drift rejects indices");
    }

    private static void randomizedDifferential() {
        Random random = new Random(0x67324103L);
        for(int round=0;round<250;round++) {
            int length = random.nextInt(1500);
            long[] payloads = new long[length]; byte[] state = new byte[length];
            for(int i=0;i<length;i++) {
                payloads[i] = switch(i%7) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextLong();
                };
                state[i] = (byte)random.nextInt(256);
            }
            int from = length==0?0:random.nextInt(length+1);
            int to = from+random.nextInt(length-from+1);
            int a = random.nextInt(256), b = random.nextInt(256);
            int bits=a|b;
            M3LongMaskRows selection=M3LongMaskRows.from(new SimpleOwner(payloads), state)
                    .requireFlags(a).requireFlags(b).sourceRange(from,to);
            long[] expected = new long[to-from]; int[] positions=new int[to-from];int written=0;
            for(int i=from;i<to;i++)if(((state[i]&255)&bits)==bits){expected[written]=payloads[i];positions[written++]=i;}
            eq(Arrays.copyOf(expected,written),selection.view().toArray(),"random payloads round="+round);
            eq(Arrays.copyOf(positions,written),selection.selectedSourceRows(M3IndexMaskCompactor.Backend.JAVA),"random ordinals round="+round);
            if(M3IndexMaskCompactor.nativeAvailable()) {
                eq(Arrays.copyOf(positions,written),selection.selectedSourceRows(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "native row-selection parity round="+round);
            }
            yes(selection.selectedCount()==written,"random count round="+round);
            yes(selection.view().longStream().count()==written,"random stream round="+round);
        }
    }

    private static void budgetAndStress() {
        int n=100_000;
        long[] data=new long[n];byte[] state=new byte[n];
        for(int i=0;i<n;i++){data[i]=i;state[i]=(byte)(i%4==0?3:1);}
        SimpleOwner owner=new SimpleOwner(data);
        M3LongMaskRows root=M3LongMaskRows.from(owner,state);
        state[0]=0;
        for(int repeat=0;repeat<50;repeat++){
            M3LongMaskRows p=root.sourceRange(0,n).requireFlags(2);
            yes(p.selectedCount()==n/4,"stress selectedCount");
            int[] rows=p.selectedSourceRows(M3IndexMaskCompactor.Backend.JAVA);
            yes(rows.length==n/4 && rows[0]==0 && rows[rows.length-1]==n-4,"stress stable index");
            if(M3IndexMaskCompactor.nativeAvailable()) {
                eq(rows, p.selectedSourceRows(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "stress original JNI parity");
            }
            long sum=p.view().longStream().sum();
            yes(sum==((long)(n/4-1)*(n/4)/2)*4,"stress primitive sum");
        }
    }

    private static void eq(long[] expected,long[] actual,String msg){assertions++;if(!Arrays.equals(expected,actual))throw new AssertionError(msg);}
    private static void eq(int[] expected,int[] actual,String msg){assertions++;if(!Arrays.equals(expected,actual))throw new AssertionError(msg);}
    private static void yes(boolean good,String msg){assertions++;if(!good)throw new AssertionError(msg);}
    private static void raises(Class<? extends Throwable> clazz,Runnable task,String msg){assertions++;try{task.run();throw new AssertionError(msg+" not rejected");}catch(Throwable e){if(!clazz.isInstance(e))throw new AssertionError(msg+" got="+e,e);}}

    private static final class SimpleOwner implements M3LongCollection {
        private long[] values;
        private SimpleOwner(long[] initial){values=initial.clone();}
        @Override public int size(){return values.length;}
        @Override public boolean contains(long target){for(long v:values)if(v==target)return true;return false;}
        @Override public void clear(){values=new long[0];}
        private void set(int at,long val){values[at]=val;}
        private void add(long value){values=Arrays.copyOf(values,values.length+1);values[values.length-1]=value;}
        @Override public long[] toArray(){return values.clone();}
        @Override public M3LongIterator iterator(){
            return new M3LongIterator(){int index;@Override public boolean hasNext(){return index<values.length;}
                @Override public long nextLong(){if(!hasNext())throw new NoSuchElementException();return values[index++];}};
        }
    }
}
