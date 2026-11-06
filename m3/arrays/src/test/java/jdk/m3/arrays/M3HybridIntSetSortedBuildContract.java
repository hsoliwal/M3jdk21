// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Deterministic independent sorted-set oracle. Never reuses the optimized builder. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-mat-hybrid-sorted-unique-20261030/src/test/java/com/synexia/matcollections/MatHybridSortedBuildOracle.java blob e6d7564b6131; recipe m3/arrays/recipe/hybrid-int-set-20261106/relocate.py v3
public final class M3HybridIntSetSortedBuildContract {
    private static long assertions;
    private static long fingerprint = 0xcbf29ce484222325L;

    private M3HybridIntSetSortedBuildContract() { }

    public static void main(String[] args) {
        lexicalDecoys();
        refusals();
        fixedSets();
        randomizedSets();
        System.out.printf("PASS MAT_HYBRID_SORTED_BUILD assertions=%d digest=%016x%n",
                assertions, fingerprint);
    }

    private static void lexicalDecoys() {
        String sourceLooking = "if (x<0) throw new IllegalArgumentException(); return of(Arrays.copyOf(out,k));";
        String regexSource = "(while|new|return)\\s*\\(";
        String block = """
            class Fake { int[] a = {1, 2, 3}; }
            //FLLDCIM: RECIPE fake anchor
            void notARealMethod() { return; }
            """;
        yes(sourceLooking.contains("Arrays.copyOf"), "source literal decoy");
        yes(Pattern.compile(regexSource).matcher("return(").find(), "regex code decoy");
        yes(block.contains("notARealMethod"), "text-block decoy");
    }

    private static void refusals() {
        thrown(NullPointerException.class, () -> M3HybridIntSet.of((int[]) null), "null source");
        thrown(IllegalArgumentException.class, () -> M3HybridIntSet.of(-1), "negative source");
        thrown(IllegalArgumentException.class, () -> M3HybridIntSet.of(10,-3,10), "mixed negative");
        M3HybridIntSet empty = M3HybridIntSet.of();
        equal(0,empty.size(), "empty size");
        equal(0,empty.chunks(), "empty chunks");
        yes(Arrays.equals(new int[0],empty.toArray()), "empty export");
        thrown(IndexOutOfBoundsException.class, () -> empty.chunkKey(0), "empty chunkKey");
        thrown(IndexOutOfBoundsException.class, () -> empty.chunkKind(0), "empty chunkKind");
        thrown(NullPointerException.class, () -> empty.union(null), "null union");
        thrown(NullPointerException.class, () -> empty.intersect(null), "null intersection");
        thrown(NullPointerException.class, () -> empty.xor(null), "null xor");
        thrown(NullPointerException.class, () -> empty.minus(null), "null difference");
    }

    private static void fixedSets() {
        int[] left = new int[10_010];
        left[0]=0;left[1]=1;left[2]=17;left[3]=65535;
        int count=4;
        for (int i=1000;i<5000;i++) left[count++]=(1<<16)|i;
        for (int i=0;i<6000;i++) left[count++]=(2<<16)|(i*10);
        left[count++]=Integer.MAX_VALUE;
        // Include duplicate entries across both sorted and unsorted input: copies are mandatory.
        int[] raw=Arrays.copyOf(left,count);
        int[] before=raw.clone();
        M3HybridIntSet a=M3HybridIntSet.of(raw);
        yes(Arrays.equals(before,raw),"of source ownership");
        assertCanonical(a,oracle(raw),"three-encoding fixed shape");
        equal(M3HybridIntSet.ChunkKind.ARRAY,a.chunkKind(0),"array lane");
        equal(M3HybridIntSet.ChunkKind.RUNS,a.chunkKind(1),"run lane");
        equal(M3HybridIntSet.ChunkKind.BITMAP,a.chunkKind(2),"bitmap lane");
        equal(M3HybridIntSet.ChunkKind.ARRAY,a.chunkKind(3),"max-value lane");
        int[] other = { 1, 3, 5, 65535, 65536, 67000, 99999, Integer.MAX_VALUE,
                3, 99999, 150000, 777777, 0 };
        probe(a,M3HybridIntSet.of(other),oracle(raw),oracle(other),"fixed");
        probe(a,a,oracle(raw),oracle(raw),"self");
        probe(a,M3HybridIntSet.of(),oracle(raw),new TreeSet<>(),"empty right");
        probe(M3HybridIntSet.of(),a,new TreeSet<>(),oracle(raw),"empty left");
        int[] crosses = {0, 65535, 65536, 131071, 131072, 196607, 196608,
                0x1fffffff, Integer.MAX_VALUE};
        probe(M3HybridIntSet.of(crosses),M3HybridIntSet.of(crosses),
                oracle(crosses),oracle(crosses),"chunk endpoints");
    }

    private static void randomizedSets() {
        Random random = new Random(0x5eedcafeaabb01L);
        for (int iteration=0;iteration<320;iteration++) {
            int lenA=random.nextInt(900),lenB=random.nextInt(900);
            int[] left=new int[lenA],right=new int[lenB];
            for(int i=0;i<lenA;i++) left[i]=randomValue(random,iteration,i);
            for(int i=0;i<lenB;i++) right[i]=randomValue(random,iteration+1,i);
            int[] keepA=left.clone(),keepB=right.clone();
            M3HybridIntSet a=M3HybridIntSet.of(left),b=M3HybridIntSet.of(right);
            yes(Arrays.equals(keepA,left),"random left unmodified");
            yes(Arrays.equals(keepB,right),"random right unmodified");
            probe(a,b,oracle(left),oracle(right),"random "+iteration);
            if((iteration&31)==0) {
                probe(a,M3HybridIntSet.of(),oracle(left),new TreeSet<>(),"random empty");
                probe(a,a,oracle(left),oracle(left),"random idempotent");
            }
        }
    }

    private static int randomValue(Random random,int salt,int position) {
        return switch (random.nextInt(9)) {
            case 0 -> 0;
            case 1 -> Integer.MAX_VALUE;
            case 2 -> (position&65535);
            case 3 -> (1<<16) | (900+random.nextInt(4100));
            case 4 -> (2<<16) | (random.nextInt(32768)*2);
            case 5 -> (3<<16) | random.nextInt(65536);
            case 6 -> random.nextInt(100);
            case 7 -> ((salt&127)<<16) | random.nextInt(65536);
            default -> random.nextInt(Integer.MAX_VALUE);
        };
    }

    private static TreeSet<Integer> oracle(int[] values) {
        TreeSet<Integer> set=new TreeSet<>();
        for(int value:values) set.add(value);
        return set;
    }

    private static void probe(M3HybridIntSet a, M3HybridIntSet b,
            TreeSet<Integer> la,TreeSet<Integer> lb,String id) {
        assertCanonical(a,la,id+" left");
        assertCanonical(b,lb,id+" right");
        TreeSet<Integer> expected = new TreeSet<>(la);
        expected.addAll(lb);
        assertCanonical(a.union(b),expected,id+" union");
        expected = new TreeSet<>(la);
        expected.retainAll(lb);
        assertCanonical(a.intersect(b),expected,id+" intersection");
        expected = new TreeSet<>(la);
        expected.removeAll(lb);
        assertCanonical(a.minus(b),expected,id+" difference");
        expected = new TreeSet<>(la);
        for (int value:lb) if(!expected.add(value)) expected.remove(value);
        assertCanonical(a.xor(b),expected,id+" xor");
        // Different set operations must not modify the source physical representations.
        assertCanonical(a,la,id+" source A retained");
        assertCanonical(b,lb,id+" source B retained");
    }

    private static void assertCanonical(M3HybridIntSet set,TreeSet<Integer> expected,String id) {
        int[] values=set.toArray();
        int[] expectedArray=expected.stream().mapToInt(Integer::intValue).toArray();
        yes(Arrays.equals(expectedArray,values),id+" sorted distinct content");
        equal(expectedArray.length,set.size(),id+" size");
        int previous=-1;
        for(int chunk=0;chunk<set.chunks();chunk++) {
            int key=set.chunkKey(chunk);
            yes(key>previous,id+" chunk strict order");
            previous=key;
            yes(Objects.nonNull(set.chunkKind(chunk)),id+" encoding");
            fingerprint=(fingerprint ^ key)*0x100000001b3L;
            fingerprint=(fingerprint ^ set.chunkKind(chunk).ordinal())*0x100000001b3L;
        }
        fingerprint=(fingerprint ^ set.primitivePayloadBytes())*0x100000001b3L;
        for(int value:expectedArray) {
            yes(set.contains(value),id+" presence");
            fingerprint=(fingerprint ^ value)*0x100000001b3L;
        }
        // Explicit negative-range semantics, outside this collection's universe.
        yes(!set.contains(-1),id+" no negative");
        if(values.length>0){
            int first=values[0];values[0]^=0x12345678;
            yes(set.contains(first),id+" export independent");
        }
    }

    private static void equal(int expected,int actual,String msg) {
        yes(expected==actual,msg+" expected="+expected+" actual="+actual);
    }
    private static void equal(M3HybridIntSet.ChunkKind expected,
            M3HybridIntSet.ChunkKind actual,String msg) {
        yes(expected==actual,msg+" expected="+expected+" actual="+actual);
    }
    private static void thrown(Class<? extends Throwable> type,Runnable work,String msg) {
        try {work.run();} catch(Throwable e){yes(type.isInstance(e),msg+": "+e);return;}
        throw new AssertionError(msg+" expected "+type.getName());
    }
    private static void yes(boolean condition,String message){
        assertions++;
        if(!condition) throw new AssertionError(message+" assertion "+assertions);
    }
}
