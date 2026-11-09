// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Independent, deterministic oracle; source-looking text is deliberately non-code. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-mat-hybrid-run-binary-20261030/src/test/java/com/synexia/matcollections/MatHybridRunSearchOracle.java blob fd404cb78877; recipe m3/arrays/recipe/hybrid-int-set-20261106/relocate.py v3
public final class HybridIntSetRunSearchContract {
    private static long checks;
    private static long fingerprint = 0xcbf29ce484222325L;
    private HybridIntSetRunSearchContract() { }

    public static void main(String[] args) {
        decoyText();
        invalidCases();
        for (int runs : new int[] {1, 2, 3, 7, 16, 64, 128, 256, 512, 1023, 1024}) {
            int[] values = runs(runs, 8, 3, 0);
            HybridIntSet set = HybridIntSet.of(values);
            check(set.chunkKind(0) == HybridIntSet.ChunkKind.RUNS, "RUNS shape " + runs);
            verifyQueries(set, toTree(values), runs <= 64 ? 1 : 11, "runs=" + runs);
        }
        mixedChunks();
        randomGroups();
        System.out.printf("M3_RUN_SEARCH_PASS checks=%d fingerprint=%016x%n", checks, fingerprint);
    }

    private static void decoyText() {
        String code = "private boolean containsRuns(int c,int low){ return a[b] == 7; }";
        String regex = "(?:(?:for|while)\\s*\\([^)]*\\))";
        String block = """
                class Fake { void method() { return; } }
                //FLLDCIM: RECIPE fake source anchor
                System.out.println("not compiled");
                """;
        check(code.contains("containsRuns"), "source-looking string");
        check(Pattern.compile(regex).matcher("while (true)").find(), "regex-looking source");
        check(block.contains("FLLDCIM"), "text-block source decoy");
    }

    private static void invalidCases() {
        throwsExpected(NullPointerException.class, () -> HybridIntSet.of((int[]) null), "null array");
        throwsExpected(IllegalArgumentException.class, () -> HybridIntSet.of(-1), "negative value");
        throwsExpected(IllegalArgumentException.class, () -> HybridIntSet.of(7, 3, -9), "later negative");
        HybridIntSet empty = HybridIntSet.of();
        check(empty.chunks() == 0 && empty.size() == 0, "empty chunks");
        check(empty.toArray().length == 0 && !empty.contains(0), "empty behavior");
        throwsExpected(NullPointerException.class, () -> empty.union(null), "null union");
        throwsExpected(IndexOutOfBoundsException.class, () -> empty.chunkKind(0), "empty chunk kind");
    }

    private static int[] runs(int n, int length, int gap, int chunk) {
        int[] raw = new int[Math.multiplyExact(n, length)];
        int p = 0;
        for (int i=0; i<n; i++) {
            for (int j=0; j<length; j++) raw[p++] = (chunk << 16) + i*(length + gap) + j;
        }
        return raw;
    }

    private static TreeSet<Integer> toTree(int[] a) {
        TreeSet<Integer> ret = new TreeSet<>();
        for (int x : a) ret.add(x);
        return ret;
    }

    private static void verifyQueries(HybridIntSet set, TreeSet<Integer> expected, int stride, String id) {
        check(set.size() == expected.size(), id+" cardinality");
        int[] visible = set.toArray();
        check(Arrays.equals(expected.stream().mapToInt(Integer::intValue).toArray(),visible), id+" array");
        long payload = set.primitivePayloadBytes();
        check(payload >= 0, id+" payload");
        fingerprint = mix(fingerprint ^ payload);
        for(int chunk=0;chunk<set.chunks();chunk++) {
            int key=set.chunkKey(chunk);
            fingerprint = mix(fingerprint ^ (key<<2) ^ set.chunkKind(chunk).ordinal());
        }
        for(int q=0;q<65536;q+=stride) {
            boolean want=expected.contains(q);
            boolean got=set.contains(q);
            check(want==got,id+" query="+q);
            fingerprint = mix(fingerprint ^ q ^ (got?0x8814L:0x9945L));
        }
        for(int x : visible) {
            check(set.contains(x),id+" included="+x);
            fingerprint=mix(fingerprint^x);
        }
        if(visible.length>0) {
            int first=visible[0];
            visible[0]=first^0x4b42;
            check(set.contains(first),id+" snapshot independent");
        }
        check(!set.contains(-1),id+" negative miss");
        check(!set.contains(Integer.MIN_VALUE),id+" min miss");
        check(!set.contains(Integer.MAX_VALUE) || expected.contains(Integer.MAX_VALUE), id+" max");
    }

    private static void mixedChunks() {
        int[] left=runs(1024,8,3,0), middle=runs(256,11,9,1), late=runs(1023,8,6,3);
        int[] all=new int[left.length+middle.length+late.length+5];
        int at=0;
        for(int[] part:new int[][]{left,middle,late}) {
            System.arraycopy(part,0,all,at,part.length);at+=part.length;
        }
        all[at++]=(2<<16)|0;
        all[at++]=(2<<16)|3;
        all[at++]=(2<<16)|999;
        all[at++]=Integer.MAX_VALUE;
        all[at++]=Integer.MAX_VALUE;
        int[] before=all.clone();
        HybridIntSet multi=HybridIntSet.of(all);
        check(Arrays.equals(before,all),"caller input retained");
        check(multi.chunks()==5,"five sorted chunks");
        check(multi.chunkKind(0)==HybridIntSet.ChunkKind.RUNS,"first run offset");
        check(multi.chunkKind(1)==HybridIntSet.ChunkKind.RUNS,"second run offset");
        check(multi.chunkKind(2)==HybridIntSet.ChunkKind.ARRAY,"array gap");
        check(multi.chunkKind(3)==HybridIntSet.ChunkKind.RUNS,"third run offset");
        check(multi.chunkKind(4)==HybridIntSet.ChunkKind.ARRAY,"end array");
        // Query every chunk with local coordinates and exact first/last/high-domain values.
        TreeSet<Integer> expected=toTree(all);
        for(int c=0;c<4;c++) {
            for(int low=0;low<65536;low+=53) {
                int q=(c<<16)|low;
                boolean actual=multi.contains(q), want=expected.contains(q);
                check(actual==want,"mixed chunk="+c+" low="+low);
                fingerprint=mix(fingerprint^q^(actual?1L:0L));
            }
        }
        check(multi.contains(Integer.MAX_VALUE),"int max");
        check(multi.contains((1<<16)|0),"second run first");
        check(!multi.contains((1<<16)|11),"second run gap");
        check(multi.contains((3<<16)|0),"third run first");
        check(!multi.contains((3<<16)|8),"third run gap");
        // Rebuilding via set-algebra operations must reconstruct packed run metadata.
        HybridIntSet rhs=HybridIntSet.of(runs(64,8,5,0));
        TreeSet<Integer> b=toTree(rhs.toArray());
        verifyAlgebra(multi,rhs,expected,b,"mixed");
    }

    private static void randomGroups() {
        Random rnd=new Random(0x51ab199fbeefL);
        for(int iter=0;iter<90;iter++) {
            int count=1+rnd.nextInt(800);
            int[] values=new int[count*7];
            int at=0;
            for(int i=0;i<count;i++) {
                for(int t=0;t<7;t++) values[at++]=(iter%4<<16)|(i*9+t);
            }
            int[] copy=values.clone();
            HybridIntSet a=HybridIntSet.of(values);
            check(Arrays.equals(copy,values),"random input retained");
            check(a.chunkKind(0)==HybridIntSet.ChunkKind.RUNS,"random run encoding");
            TreeSet<Integer> expected=toTree(values);
            // Predictable random membership including boundaries, absent gaps and values.
            for(int j=0;j<1600;j++) {
                int query=((iter%4)<<16)|rnd.nextInt(10000);
                boolean want=expected.contains(query),got=a.contains(query);
                check(got==want,"random iter="+iter+" query="+query);
                fingerprint=mix(fingerprint ^ query ^ (got?17:51));
            }
            if(iter%20==0) verifyAlgebra(a,HybridIntSet.of(values),expected,expected,"self random");
        }
    }

    private static void verifyAlgebra(HybridIntSet a, HybridIntSet b,
             TreeSet<Integer> left, TreeSet<Integer> right, String id) {
        TreeSet<Integer> union=new TreeSet<>(left);union.addAll(right);
        TreeSet<Integer> inter=new TreeSet<>(left);inter.retainAll(right);
        TreeSet<Integer> xor=new TreeSet<>(union);xor.removeAll(inter);
        TreeSet<Integer> minus=new TreeSet<>(left);minus.removeAll(right);
        for(Object[] pair:new Object[][]{{a.union(b),union},{a.intersect(b),inter},{a.xor(b),xor},{a.minus(b),minus}}){
            HybridIntSet product=(HybridIntSet)pair[0];
            @SuppressWarnings("unchecked") TreeSet<Integer> expected=(TreeSet<Integer>)pair[1];
            check(Arrays.equals(product.toArray(),expected.stream().mapToInt(Integer::intValue).toArray()),id+" algebra");
            for(int element:expected) check(product.contains(element),id+" product membership");
            fingerprint=mix(fingerprint^product.size()^product.primitivePayloadBytes());
        }
    }

    private static long mix(long x) {return (x ^ (x>>>29)) * 0x100000001b3L;}
    private static void check(boolean ok,String msg) {checks++;if(!ok)throw new AssertionError(msg+" check="+checks);}
    private static void throwsExpected(Class<? extends Throwable> type,Runnable work,String label) {
        try{work.run();} catch(Throwable err){check(type.isInstance(err),label+" wrong exception="+err);return;}
        throw new AssertionError(label+" failed to throw");
    }
}
