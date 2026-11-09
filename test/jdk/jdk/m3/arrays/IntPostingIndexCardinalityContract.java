// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import static jdk.m3.arrays.IntPostingIndex.Backend.JAVA;
import static jdk.m3.arrays.IntPostingIndex.Backend.NATIVE_REQUIRED;

/** Tests real product sources; no storage, JNI or loader substitute in normal modes. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingCardinalityContract.java blob 18b33e324ad4; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndexCardinalityContract {
    private static long checks;
    private static long scenarios;
    private IntPostingIndexCardinalityContract() { }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Task { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Task task) throws Exception {
        try { task.run(); } catch (Throwable error) {
            check(type.isInstance(error), "wrong error " + error); return;
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
    private static Method raw(String name) throws Exception {
        Method m = IntPostingIndex.class.getDeclaredMethod(name,
                int[].class, int[].class, int[].class, int.class);
        m.setAccessible(true); return m;
    }
    private static int invoke(Method method, int[] data, int[] offsets, int[] keys, int rows) throws Exception {
        try { return (Integer) method.invoke(null, data, offsets, keys, rows); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception x) throw x;
            if (e.getCause() instanceof Error x) throw x;
            throw new AssertionError(e.getCause());
        }
    }
    private static void verify(int universe, int[] keys, int[] rows, int[] query) {
        IntPostingIndex index = IntPostingIndex.ofPairs(keys, rows, universe);
        BitSet union = new BitSet(), intersection = new BitSet(); intersection.set(0, universe);
        for (int key : query) {
            BitSet hits = new BitSet();
            for (int i = 0; i < keys.length; i++) if (keys[i] == key) hits.set(rows[i]);
            union.or(hits); intersection.and(hits);
        }
        int[] saved = query.clone();
        Arrays.fill(keys, 123); Arrays.fill(rows, 0); // constructor must own its input
        int any = union.cardinality(), all = intersection.cardinality();
        check(index.countAny(query) == any, "JAVA countAny");
        check(index.countAll(query) == all, "JAVA countAll");
        check(index.any(query).size() == any, "existing OR agrees");
        check(index.all(query).size() == all, "existing AND agrees");
        if (IntPostingIndex.nativeAvailable()) {
            check(index.countAny(NATIVE_REQUIRED, query) == any, "JNI countAny");
            check(index.countAll(NATIVE_REQUIRED, query) == all, "JNI countAll");
        }
        check(Arrays.equals(query, saved), "query mutation"); scenarios++;
    }
    private static void exhaustive() {
        int[] names = {Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        for (int masks = 0; masks < 4096; masks++) {
            for (int q = 0; q < 8; q++) {
                int[] keys = new int[12], rows = new int[12], query = new int[Integer.bitCount(q) * 2];
                int n = 0, at = 0;
                for (int k = 0; k < 3; k++) {
                    for (int row = 0; row < 4; row++) if ((masks & (1 << (k * 4 + row))) != 0) {
                        keys[n] = names[k]; rows[n++] = row;
                    }
                    if ((q & (1 << k)) != 0) { query[at++] = names[k]; query[at++] = names[k]; }
                }
                verify(4, Arrays.copyOf(keys, n), Arrays.copyOf(rows, n), query);
            }
        }
    }
    private static void random() {
        Random random = new Random(0x4d33434f554e5453L);
        int[] names = {Integer.MIN_VALUE, -999, -1, 0, 1, 7, 19, Integer.MAX_VALUE};
        for (int trial = 0; trial < 3000; trial++) {
            int universe = random.nextInt(1025), n = universe == 0 ? 0 : random.nextInt(2000);
            int[] keys = new int[n], rows = new int[n], query = new int[random.nextInt(18)];
            for (int i = 0; i < n; i++) { keys[i] = names[random.nextInt(names.length)]; rows[i] = random.nextInt(universe); }
            for (int i = 0; i < query.length; i++) query[i] = random.nextInt(9) == 0 ? 42 : names[random.nextInt(names.length)];
            verify(universe, keys, rows, query);
        }
    }
    private static void huge() throws Exception {
        int top = Integer.MAX_VALUE - 1;
        var index = IntPostingIndex.ofPairs(new int[]{-1,-1,-1,5,5,9},
                new int[]{0,1024,top,1024,top,top}, Integer.MAX_VALUE);
        var b = IntPostingIndex.nativeAvailable() ? NATIVE_REQUIRED : JAVA;
        check(index.countAny(b) == 0, "empty OR");
        check(index.countAll(b) == Integer.MAX_VALUE, "empty AND exact full int universe");
        check(index.countAny(b,-1,5,9,-1) == 3, "large sparse union");
        check(index.countAll(b,-1,5,9) == 1, "large sparse intersection");
        check(index.countAll(b,-1,5) == 2, "large two-way intersection");
        check(index.countAll(b,-1,33) == 0, "absent AND");
        check(index.countAny(b,33) == 0, "absent OR");
        check(index.countAll(b,-1) == 3, "single posting");
        var empty = IntPostingIndex.ofPairs(new int[0],new int[0],Integer.MAX_VALUE);
        check(empty.countAll(b) == Integer.MAX_VALUE && empty.countAny(b) == 0, "empty owner");
        if (IntPostingIndex.nativeAvailable()) {
            check(invoke(raw("countIntersectionNative"),new int[0],new int[]{0},new int[0],Integer.MAX_VALUE)
                    == Integer.MAX_VALUE,"raw empty AND does not expand universe");
            check(invoke(raw("countUnionNative"),new int[0],new int[]{0,0,0,0},new int[]{0,1,2},Integer.MAX_VALUE)
                    == 0,"all-empty heap");
        }
    }
    private static void guards() throws Exception {
        var index = IntPostingIndex.ofKeys(new int[]{1,2,1});
        expect(NullPointerException.class, () -> index.countAny((int[]) null));
        expect(NullPointerException.class, () -> index.countAll((int[]) null));
        expect(NullPointerException.class, () -> index.countAny(null,new int[0]));
        expect(NullPointerException.class, () -> index.countAll(null,new int[0]));
        if (!IntPostingIndex.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () -> index.countAny(NATIVE_REQUIRED,1));
            expect(UnsatisfiedLinkError.class, () -> index.countAll(NATIVE_REQUIRED,99));
            return;
        }
        for (String name : List.of("countUnionNative","countIntersectionNative")) {
            Method m = raw(name);
            expect(IllegalArgumentException.class, () -> invoke(m,null,new int[]{0},new int[0],1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[0],null,new int[0],1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[0],new int[]{0},null,1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[0],new int[0],new int[0],1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[0],new int[]{0},new int[0],-1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{0},new int[]{0,2},new int[]{0},1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{0},new int[]{0,1},new int[]{0,0},1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{0},new int[]{0,1},new int[]{1},1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{0},new int[]{0,1},new int[]{-1},1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{1,0},new int[]{0,2},new int[]{0},2));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{0,0},new int[]{0,2},new int[]{0},1));
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{-1},new int[]{0,1},new int[]{0},1));
            // Late selected-list invalidity must not be hidden by an earlier empty list.
            expect(IllegalArgumentException.class, () -> invoke(m,new int[]{9},new int[]{0,0,1},new int[]{0,1},1));
            int[] data={0,2,1,2}, off={0,2,4}, pos={0,1};
            int[] copy=data.clone(), oc=off.clone(), pc=pos.clone();
            check(invoke(m,data,off,pos,3)==(name.equals("countUnionNative")?3:1),"raw count");
            check(Arrays.equals(data,copy)&&Arrays.equals(off,oc)&&Arrays.equals(pos,pc),"native input immutable");
        }
    }
    private static void concurrent() throws Exception {
        var index = IntPostingIndex.ofPairs(new int[]{0,0,0,1,1,2},new int[]{0,1,2,1,2,2},3);
        var b = IntPostingIndex.nativeAvailable()?NATIVE_REQUIRED:JAVA;
        try(var pool=Executors.newFixedThreadPool(4)) {
            List<Callable<Boolean>> tasks=new ArrayList<>();
            for(int i=0;i<16;i++) tasks.add(()->{
                for(int t=0;t<100;t++) if(index.countAny(b,0,1,2)!=3||index.countAll(b,0,1,2)!=1)return false;
                return true;
            });
            for(var answer:pool.invokeAll(tasks)) check(answer.get(),"concurrent counts");
        }
    }
    private static void legacy() throws Exception {
        var index=IntPostingIndex.ofKeys(new int[]{1,2,1});
        expect(UnsatisfiedLinkError.class,()->index.countAny(NATIVE_REQUIRED,1));
        expect(UnsatisfiedLinkError.class,()->index.countAll(NATIVE_REQUIRED,1));
        check(index.any(NATIVE_REQUIRED,1).size()==2,"old union survives new missing symbol");
        check(index.all(NATIVE_REQUIRED,1).size()==2,"old intersection survives new missing symbol");
        check(IntPostingIndex.nativeAvailable(),"legacy availability unchanged");
        check(index.countAny(1)==2&&index.countAll(1)==2,"Java counts with legacy library");
    }
    private static void failedBackend(boolean allocation) throws Exception {
        var index=IntPostingIndex.ofPairs(new int[]{0,0,1,1},new int[]{0,1,0,1},2);
        Class<? extends Throwable> type=allocation?OutOfMemoryError.class:IllegalStateException.class;
        expect(type,()->index.countAny(NATIVE_REQUIRED,0,1));
        if (!allocation || Integer.getInteger("fail.at",1)==1) {
            expect(type,()->index.countAll(NATIVE_REQUIRED,0,1));
        }
        check(index.countAny(0,1)==2&&index.countAll(0,1)==2,"failure preserves Java owner");
    }
    public static void main(String[] args) throws Exception {
        String lib=System.getProperty("matposting.native.path","");
        if(!lib.isEmpty()) IntPostingIndex.loadNative(Path.of(lib));
        String mode=args.length==0?"normal":args[0];
        if(mode.equals("legacy")) legacy();
        else if(mode.equals("corrupt")) failedBackend(false);
        else if(mode.equals("allocation")) failedBackend(true);
        else if(mode.equals("huge")) huge();
        else { exhaustive();random();guards();huge();concurrent(); }
        System.out.println("POSTING_CARDINALITY_PASS mode="+mode+" native="+IntPostingIndex.nativeAvailable()
                +" checks="+checks+" scenarios="+scenarios);
    }
}
