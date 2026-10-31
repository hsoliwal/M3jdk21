/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.stream.IntStream;

/** Differential JDK ArrayList/IntStream oracle; no third-party implementation dependency. */
public final class M3IntArrayListContractProof {
    private static long checks;
    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }
    private static void equal(Object expected, Object got, String where) {
        check(Objects.equals(expected, got), where + " expected=" + expected + " got=" + got);
    }
    private static List<Integer> boxed(M3IntArrayList a) {
        return Arrays.stream(a.toArray()).boxed().toList();
    }
    private static void inspect(M3IntArrayList a, List<Integer> oracle) {
        equal(oracle, boxed(a), "owner elements");
        equal(oracle.size(), a.size(), "owner size");
        equal(oracle.stream().mapToInt(Integer::intValue).sum(), a.intStream().sum(), "sum");
        equal(oracle.stream().mapToInt(Integer::intValue).sum(), a.parallelIntStream().sum(), "parallel sum");
        check(a.capacity() >= a.size(), "logical size <= capacity");
        int[] snapshot = a.toArray();
        if (snapshot.length != 0) { int was = snapshot[0]; snapshot[0] ^= 0x77; check(a.get(0) == was, "defensive toArray"); }
        M3IntIterator cursor = a.iterator();
        int i = 0;
        while(cursor.hasNext()) {
            check(i < oracle.size(), "length");
            equal(oracle.get(i++), cursor.nextInt(), "cursor value");
        }
        equal(oracle.size(), i, "cursor total");
    }
    private static int randomValue(Random r) {
        return r.nextInt(11)==0? Integer.MIN_VALUE : r.nextInt(11)==0?Integer.MAX_VALUE:r.nextInt(201)-100;
    }
    public static void main(String[] args) {
        Random r = new Random(0x20261018);
        M3IntArrayList a = new M3IntArrayList();
        List<Integer> j = new ArrayList<>();
        inspect(a,j);
        for (int step=0;step<9000;step++) {
            int v = randomValue(r), op=r.nextInt(13);
            switch(op) {
                case 0 -> { a.add(v);j.add(v); }
                case 1 -> { int p=r.nextInt(j.size()+1);a.insert(p,v);j.add(p,v); }
                case 2 -> { if (!j.isEmpty()) {int p=r.nextInt(j.size());equal(j.remove(p),a.removeAt(p),"removeAt");} }
                case 3 -> {if (!j.isEmpty()) {int p=r.nextInt(j.size());equal(j.set(p,v),a.set(p,v),"set");} }
                case 4 -> equal(j.remove((Integer)v),a.removeValue(v),"removeValue");
                case 5 -> equal(j.contains(v),a.contains(v),"contains");
                case 6 -> {int[] x={v,randomValue(r),v}; a.addAll(x); for (int w:x)j.add(w);}
                case 7 -> {int[] x={v,randomValue(r)};int p=r.nextInt(j.size()+1);a.addAll(p,x);for(int i=0;i<x.length;i++)j.add(p+i,x[i]);}
                case 8 -> {a.sort();j.sort(Integer::compare);}
                case 9 -> {if (r.nextInt(120)==0) {a.clear();j.clear();}}
                case 10 -> {a.ensureCapacity(j.size()+r.nextInt(8));}
                case 11 -> a.trimToSize();
                case 12 -> {equal(j.indexOf(v),a.indexOf(v),"indexOf");equal(j.lastIndexOf(v),a.lastIndexOf(v),"lastIndexOf");}
                default -> throw new AssertionError();
            }
            inspect(a,j);
        }
        a.sort(); j.sort(Integer::compare);
        int removed = a.deduplicateSorted();
        List<Integer> distinct = j.stream().distinct().toList();
        equal(j.size()-distinct.size(),removed,"dedup count");
        equal(distinct,boxed(a),"dedup sorted");
        for (int i=0;i<a.size();i++) check(a.binarySearch(a.get(i))>=0,"binarySearch");
        M3IntIterator old = a.iterator(); a.add(5);
        boolean refused=false;
        try{old.hasNext();}catch(ConcurrentModificationException correct){refused=true;}
        check(refused,"iterator fail fast");
        a.clear();a.addAll(new int[]{1,2,3,4,5,6,7,8,9});
        int count=0;
        try {a.filterInPlace(v->{if(v==6)throw new IllegalStateException("failed predicate");return v%2==1;});}
        catch(IllegalStateException expected){count++;}
        check(count==1,"predicate exception");
        check(Arrays.equals(a.toArray(),new int[]{1,3,5,6,7,8,9}),"unread suffix preserved");
        a.clear();a.addAll(new int[]{3,3,-1,8,3,0,-1});
        byte[] masks={(byte)1,0,(byte)3,0,(byte)1,0,(byte)7};
        equal(3,a.retainByStateMask(masks,1),"removed masked");
        check(Arrays.equals(a.toArray(),new int[]{3,-1,3,-1}),"masked stable");
        var original = M3IntIndexedSnapshot.copyOf(new int[]{3,-1,3,Integer.MIN_VALUE,8,Integer.MAX_VALUE});
        var sorted=original.sortedAscending(); var descending=original.sortedDescending();
        check(Arrays.equals(sorted.toArray(),new int[]{Integer.MIN_VALUE,-1,3,3,8,Integer.MAX_VALUE}),"ascending");
        check(Arrays.equals(descending.toArray(),new int[]{Integer.MAX_VALUE,8,3,3,-1,Integer.MIN_VALUE}),"descending");
        equal(2,sorted.lowerBound(3),"lowerBound");
        equal(2,descending.lowerBound(3),"descending bound");
        check(sorted.physicalIndexAt(2)==0&&sorted.physicalIndexAt(3)==2,"stable tie rows");
        var sliced=sorted.slice(1,5).distinctSorted();
        check(Arrays.equals(sliced.toArray(),new int[]{-1,3,8}),"slice distinct");
        check(Arrays.equals(original.toArray(),new int[]{3,-1,3,Integer.MIN_VALUE,8,Integer.MAX_VALUE}),"detached backing");
        var pred=original.filtered(v->v==3);
        check(Arrays.equals(pred.toArray(),new int[]{3,3}),"predicate filter preserves occurrence");
        Spliterator.OfInt split = sorted.intSpliterator();
        var prefix=split.trySplit();
        check(prefix!=null,"splittable immutable indexed snapshot");
        check(IntStream.concat(java.util.stream.StreamSupport.intStream(prefix,false),
             java.util.stream.StreamSupport.intStream(split,false)).toArray().length==sorted.size(),"split coverage");
        var byMask=M3IntIndexedSnapshot.fromAlignedMask(new int[]{11,22,33,44},
              new byte[]{1,0,3,1},1,M3IndexMaskCompactor.Backend.JAVA);
        check(Arrays.equals(byMask.toArray(),new int[]{11,33,44}),"mask selector");
        if (args.length == 1 && args[0].equals("--native")) {
            check(M3IndexMaskCompactor.nativeAvailable(), "actual native library required");
            var viaNative = M3IntIndexedSnapshot.fromAlignedMask(
                    new int[]{11, 22, 33, 44}, new byte[]{1, 0, 3, 1}, 1,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(viaNative.toArray(), new int[]{11, 33, 44}),
                    "checked JNI index-only selection parity");
        } else if (args.length == 1 && args[0].equals("--missing")) {
            boolean missing = false;
            try {
                M3IntIndexedSnapshot.fromAlignedMask(new int[]{1}, new byte[]{1}, 1,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            } catch (UnsatisfiedLinkError refusal) { missing = true; }
            check(missing, "explicit native request cannot silently use Java");
        }
        System.out.println("M3_INT_ARRAY_INDEX_PASS steps=9000 assertions="+checks
                + " backend=" + (args.length == 0 ? "JAVA" : args[0]));
    }
}
