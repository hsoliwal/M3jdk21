/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.RandomAccess;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic Java21 SDK contract oracle; --native requires actual JNI. */
public final class M3CollectionSdkSelfTest {
    private static long checks;
    private M3CollectionSdkSelfTest() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (nativeRun && !M3IndexPathMasks.nativeAvailable())
            throw new AssertionError("required native path-mask library missing");
        partitions();
        countedBag();
        facades();
        indexedMasks(nativeRun ? M3IndexPathMasks.Backend.NATIVE_REQUIRED
                : M3IndexPathMasks.Backend.JAVA);
        System.out.println("PASS M3CollectionSdkSelfTest checks=" + checks + " native=" + nativeRun);
    }

    private static void partitions() {
        ArrayList<Integer> source = new ArrayList<>(List.of(1,2,3,4,5));
        List<List<Integer>> chunks = M3CollectionSdk.partition(source, 2);
        eq(List.of(List.of(1,2),List.of(3,4),List.of(5)), chunks);
        ok(chunks instanceof RandomAccess);
        fails(UnsupportedOperationException.class, chunks::clear);
        chunks.get(0).set(1, 42);
        eq(42, source.get(1).intValue());
        source.add(6);
        eq(List.of(5,6), chunks.get(2));
        ok(!(M3CollectionSdk.partition(new LinkedList<>(source), 2) instanceof RandomAccess));
        fails(IllegalArgumentException.class, () -> M3CollectionSdk.partition(source, 0));
        fails(NullPointerException.class, () -> M3CollectionSdk.partition(null, 2));
        fails(IndexOutOfBoundsException.class, () -> chunks.get(chunks.size()));
        List<Integer> huge = new java.util.AbstractList<>() {
            @Override public int size() { return Integer.MAX_VALUE; }
            @Override public Integer get(int index) { return index; }
        };
        List<List<Integer>> last = M3IndexPartitions.view(huge, 3);
        eq(715827883, last.size());
        eq(1, last.get(last.size()-1).size());
    }

    private static void countedBag() {
        M3LongBag bag = M3CollectionSdk.longBag();
        Map<Long,Long> reference = new HashMap<>();
        Random rng = new Random(0x4d3353444b424147L);
        long[] keys = {0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L, 3L, 7L};
        for (int i=0;i<2048;i++) {
            long key=keys[rng.nextInt(keys.length)],before=reference.getOrDefault(key,0L);
            long n=rng.nextInt(11);
            switch (rng.nextInt(3)) {
                case 0 -> { eq(before,bag.add(key,n)); if(n>0)reference.put(key,before+n); }
                case 1 -> {long k=Math.min(before,n);eq(k,bag.remove(key,n));
                    if(k==before)reference.remove(key);else reference.put(key,before-k);}
                default -> {eq(before,bag.setCount(key,n));
                    if(n==0)reference.remove(key);else reference.put(key,n);}
            }
            eq(reference.getOrDefault(key,0L).longValue(),bag.count(key));
            eq(reference.size(),bag.distinctCount());
            long total=0; for(long value:reference.values())total+=value;
            eq(total,bag.totalCount());
            final long[] seen={0};
            bag.forEachCount((value,count)->{
                eq(reference.get(value).longValue(),count);seen[0]++;
            });
            eq(reference.size(),(int)seen[0]);
        }
        fails(IllegalArgumentException.class,()->bag.add(0,-1));
        fails(IllegalArgumentException.class,()->bag.remove(0,-1));
        fails(IllegalArgumentException.class,()->bag.setCount(0,-1));
        M3LongBag max=new M3LongBag();
        max.add(0,Long.MAX_VALUE);
        fails(ArithmeticException.class,()->max.add(0,1));
        fails(ArithmeticException.class,()->max.add(1,1));
        eq(Long.MAX_VALUE,max.totalCount());
        bag.clear(); ok(bag.isEmpty());
    }

    private static void facades() {
        ok(M3CollectionSdk.jdkArrayList() instanceof ArrayList<?>);
        M3LongArrayList list=M3CollectionSdk.longArrayList();
        list.add(Long.MIN_VALUE);eq(Long.MIN_VALUE,list.get(0));
        M3LongHashSet set=M3CollectionSdk.longHashSet();
        ok(set.add(0L));ok(set.contains(0L));
        M3LongLongHashMap map=M3CollectionSdk.longLongHashMap();
        ok(map.put(0L,Long.MAX_VALUE));eq(Long.MAX_VALUE,map.getOrDefault(0L,-1L));
        M3TreeMap<Integer,String> sortedMap=M3CollectionSdk.treeMap();
        sortedMap.put(3,"three");eq("three",sortedMap.get(3));
        M3TreeSet<Integer> sortedSet=M3CollectionSdk.treeSet();
        ok(sortedSet.add(3));ok(sortedSet.contains(3));
        List<Integer> base=new ArrayList<>(List.of(1,2,3,4));
        List<Integer> visible=M3CollectionSdk.readOnlyFilteredList(base,x->x%2==0);
        eq(List.of(2,4),visible);
        fails(UnsupportedOperationException.class,()->visible.add(6));
        List<Integer> mutable=M3CollectionSdk.mutableFilteredList(base,x->x%2==0);
        mutable.add(6);eq(List.of(2,4,6),mutable);
        TreeSet<Integer> owner=new TreeSet<>(Comparator.reverseOrder());
        owner.addAll(List.of(1,2,3,4,5,6));
        NavigableSet<Integer> filtered=M3CollectionSdk.readOnlyFilteredNavigableSet(
                owner,x->x%2==0);
        eq(List.of(6,4,2),new ArrayList<>(filtered));
        fails(UnsupportedOperationException.class,()->filtered.add(8));
        NavigableSet<Integer> editable=M3CollectionSdk.mutableFilteredNavigableSet(
                owner,x->x%2==0);
        editable.add(8);eq(8,editable.first().intValue());
        TreeMap<Integer,String> tree=new TreeMap<>();
        tree.put(1,"a");tree.put(2,"bb");tree.put(3,"ccc");
        NavigableMap<Integer,String> mapView=M3CollectionSdk.readOnlyFilteredNavigableMap(
                tree,(k,v)->v.length()>1);
        eq(List.of(2,3),new ArrayList<>(mapView.keySet()));
        fails(UnsupportedOperationException.class,()->mapView.put(4,"dddd"));
        NavigableMap<Integer,String> write=M3CollectionSdk.mutableFilteredNavigableMap(
                tree,(k,v)->v.length()>1);
        eq("bb",write.remove(2));ok(!tree.containsKey(2));
        long[] data={7,3,7};
        M3LongIndexProjection snap=M3CollectionSdk.indexedLongSnapshot(data);
        data[0]=55;eq(7L,snap.get(0));
    }

    private static void indexedMasks(M3IndexPathMasks.Backend backend) {
        int[] rows={42,19,42,-7,0},offsets={1,0,1,2,3};
        int[][] planes={{1,0x80000001,0,1},{2,2,2,0}};
        int[] required={0x80000001,2},excluded={4,0};
        eq(new int[]{42,42},M3CollectionSdk.selectMaskedPath(rows,offsets,planes,
                required,excluded,0,5,1,backend));
        eq(new int[]{42,42},M3CollectionSdk.selectMaskedPath(rows,offsets,planes,
                required,excluded,4,5,-1,backend));
        fails(IndexOutOfBoundsException.class,()->M3CollectionSdk.selectMaskedPath(
                rows,offsets,planes,required,excluded,4,2,1,backend));
    }

    private static void eq(List<?> a,List<?> b){ok(a.equals(b));}
    private static void eq(Object a,Object b){ok(java.util.Objects.equals(a,b));}
    private static void eq(long a,long b){ok(a==b);}
    private static void eq(int a,int b){ok(a==b);}
    private static void eq(int[] a,int[] b){ok(Arrays.equals(a,b));}
    private static void ok(boolean good){if(!good)throw new AssertionError("M3 SDK check "+checks);checks++;}
    private static void fails(Class<? extends Throwable> type,Runnable body){
        try{body.run();}catch(Throwable error){
            if(type.isInstance(error)){checks++;return;}
            throw new AssertionError("Unexpected: "+error,error);
        }
        throw new AssertionError("Expected "+type.getName());
    }
}
