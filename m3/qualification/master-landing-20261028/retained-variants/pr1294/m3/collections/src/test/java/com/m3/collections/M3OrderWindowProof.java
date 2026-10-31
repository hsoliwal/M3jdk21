/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Executable oracle over real collection owners, not an alternative implementation. */
public final class M3OrderWindowProof {
    private static long checks;
    private static MessageDigest observations;
    private static boolean nativeMode;
    private static volatile Object escape;

    private M3OrderWindowProof() { }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    private static void observe(long value) {
        for (int shift = 56; shift >= 0; shift -= 8) observations.update((byte) (value >>> shift));
    }

    private static void same(int[] expected, int[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
        for (int value : actual) observe(value);
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "wrong failure: " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }

    private static long reference(int[] rows, byte[] states, int first, int required,
            int excluded, int[] output, int offset, int limit) {
        int next = first, written = 0;
        while (next < rows.length && written < limit) {
            int flags = Byte.toUnsignedInt(states[next]);
            if ((flags | required) == flags && (flags & excluded) == 0) {
                output[offset + written++] = rows[next];
            }
            next++;
        }
        return ((long) next << 32) | Integer.toUnsignedLong(written);
    }

    private static void window(int[] rows, byte[] states, int first, int required,
            int excluded, int offset, int limit) {
        int[] source = rows.clone(); byte[] originalStates = states.clone();
        int[] expected = new int[offset + limit + 4]; Arrays.fill(expected, 0x7ffffeed);
        int[] java = expected.clone(), jni = expected.clone();
        long cursor = reference(rows, states, first, required, excluded, expected, offset, limit);
        long actual = M3IndexMaskWindow.scan(rows, states, first, required, excluded, java,
                offset, limit, M3IndexMaskCompactor.Backend.JAVA);
        check(cursor == actual, "Java window cursor");
        same(expected, java, "Java stable window/output guard");
        observe(actual);
        if (nativeMode) {
            actual = M3IndexMaskWindow.scan(rows, states, first, required, excluded, jni,
                    offset, limit, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            check(cursor == actual, "JNI window cursor");
            same(expected, jni, "JNI stable window/output guard");
        }
        check(Arrays.equals(source, rows), "row input mutation");
        check(Arrays.equals(originalStates, states), "state input mutation");
    }

    private static void windows() {
        int[] rows = new int[257]; byte[] states = new byte[257];
        for (int i = 0; i < rows.length; i++) { rows[i] = i % 9 - 4; states[i] = (byte) i; }
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                window(rows, states, (required + excluded) % 129, required, excluded, 3, 65);
            }
        }
        Random random = new Random(0x4d334f57494eL);
        int[] lengths = {0,1,2,63,64,65,127,128,129,255,256,257,4095,4096,4097,8201};
        int[] limits = {0,1,2,63,64,65,127,128,129,4096};
        for (int size : lengths) {
            rows = new int[size]; states = new byte[size];
            for (int i = 0; i < size; i++) { rows[i] = random.nextInt(); states[i] = (byte) random.nextInt(); }
            for (int limit : limits) {
                int first = size == 0 ? 0 : random.nextInt(size + 1);
                window(rows, states, first, 0, 0, 2, limit);
                window(rows, states, first, 128, 128, 1, limit);
                window(rows, states, first, 128, 4, 7, limit);
                window(rows, states, size, 255, 0, 0, limit);
                window(rows, states, first, random.nextInt(256), random.nextInt(256), 0, limit);
            }
            // Resume at each returned cursor; no selected occurrence may disappear or repeat.
            for (int limit : new int[]{1,64,129}) {
                List<Integer> selected = new ArrayList<>();
                for (int i = 0; i < size; i++) if ((states[i] & 0x80) != 0 && (states[i] & 2) == 0) selected.add(rows[i]);
                List<Integer> emitted = new ArrayList<>(); int first = 0;
                while (first < size) {
                    int[] result = new int[limit];
                    long c = M3IndexMaskWindow.scan(rows, states, first, 128, 2, result, 0, limit,
                            nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA);
                    int next = M3IndexMaskWindow.nextIndex(c);
                    check(next > first && next <= size, "resumable progress");
                    for (int i = 0; i < M3IndexMaskWindow.written(c); i++) emitted.add(result[i]);
                    first = next;
                }
                check(selected.equals(emitted), "resumable occurrence order");
            }
        }
    }

    private static void windowFailures() throws Exception {
        int[] rows = {3,2,1}; byte[] states = {1,2,3}; int[] out = {17,17,17};
        var java = M3IndexMaskCompactor.Backend.JAVA;
        throwsType(NullPointerException.class, () -> M3IndexMaskWindow.scan(null,states,0,0,0,out,0,1,java));
        throwsType(NullPointerException.class, () -> M3IndexMaskWindow.scan(rows,null,0,0,0,out,0,1,java));
        throwsType(NullPointerException.class, () -> M3IndexMaskWindow.scan(rows,states,0,0,0,out,0,1,null));
        throwsType(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows,states,0,0,0,rows,0,1,java));
        throwsType(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows,new byte[0],0,0,0,out,0,1,java));
        for (int mask : new int[]{-1,256,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
            throwsType(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows,states,0,mask,0,out,0,0,java));
            throwsType(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows,states,0,0,mask,out,0,0,java));
        }
        for (int first : new int[]{-1,4,Integer.MAX_VALUE}) throwsType(IndexOutOfBoundsException.class,
                () -> M3IndexMaskWindow.scan(rows,states,first,1,1,out,0,1,java));
        for (int limit : new int[]{-1,4097,Integer.MAX_VALUE}) throwsType(IllegalArgumentException.class,
                () -> M3IndexMaskWindow.scan(rows,states,0,1,1,out,0,limit,java));
        for (int offset : new int[]{-1,3,Integer.MAX_VALUE}) throwsType(IndexOutOfBoundsException.class,
                () -> M3IndexMaskWindow.scan(rows,states,0,1,1,out,offset,1,java));
        check(Arrays.equals(out,new int[]{17,17,17}), "preflight must not write");
        if (nativeMode) {
            Method method = M3IndexMaskWindow.class.getDeclaredMethod("scanNative", int[].class, byte[].class,
                    int.class,int.class,int.class,int[].class,int.class,int.class);
            method.setAccessible(true);
            for (Object[] arguments : new Object[][] {
                {null,states,0,0,0,out,0,0}, {rows,null,0,0,0,out,0,0},
                {rows,states,-1,0,0,out,0,0}, {rows,states,4,0,0,out,0,0},
                {rows,states,0,256,0,out,0,0}, {rows,states,0,0,-1,out,0,0},
                {rows,states,0,0,0,rows,0,0}, {rows,states,0,1,1,out,Integer.MAX_VALUE,1},
                {rows,new byte[1],0,1,1,out,0,1}, {rows,states,0,0,0,out,0,4097}
            }) {
                try { method.invoke(null,arguments); throw new AssertionError("raw JNI validation"); }
                catch (InvocationTargetException failure) {
                    check(failure.getCause() instanceof IllegalArgumentException,"raw JNI exception kind");
                }
            }
            check(Arrays.equals(out,new int[]{17,17,17}),"raw JNI preflight wrote output");
        }
    }

    private static int[] stable(int[] order, long[] values, boolean descending) {
        Integer[] boxed = Arrays.stream(order).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, (a,b) -> descending ? Long.compare(values[b],values[a]) : Long.compare(values[a],values[b]));
        return Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
    }

    private static void projection(M3LongIndexProjection value, int[] order, long[] payload) {
        check(value.size()==order.length,"projection size");
        same(order,value.physicalIndices(),"stable physical occurrence identity");
        long[] materialized=value.toArray();
        for (int i=0;i<order.length;i++) {
            check(value.get(i)==payload[order[i]] && materialized[i]==payload[order[i]],"projected value");
        }
        if(order.length>0) {
            int[] escaped=value.physicalIndices(); escaped[0]=-1;
            materialized[0]=~materialized[0];
            check(value.physicalIndex(0)==order[0] && value.get(0)==payload[order[0]],"defensive projections");
        }
    }

    private static void sorts() {
        Random random=new Random(0x4d3353544142L);
        for(int trial=0;trial<800;trial++) {
            int n=trial<20?trial:random.nextInt(300);
            long[] input=new long[n];
            for(int i=0;i<n;i++) input[i]=switch(random.nextInt(12)) {
                case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE; default -> random.nextInt(9)-4;
            };
            long[] payload=input.clone(); var value=M3LongIndexProjection.snapshot(input);
            Arrays.fill(input,Long.MAX_VALUE);
            int[] ids=java.util.stream.IntStream.range(0,n).toArray();
            for(int pass=0;pass<8;pass++) {
                boolean descending=(pass&1)==0;
                ids=stable(ids,payload,descending);
                value=descending?value.sortedDescending():value.sortedAscending();
                check(value.order()==(descending?M3LongIndexProjection.Order.DESCENDING:M3LongIndexProjection.Order.ASCENDING),"order metadata");
                projection(value,ids,payload);
                var again=descending?value.sortedDescending():value.sortedAscending();
                check(again!=value,"sorting retains distinct descriptor contract");
                projection(again,ids,payload);
                if(pass%3==0) {
                    int first=random.nextInt(ids.length+1), count=random.nextInt(ids.length-first+1);
                    value=value.slice(first,count); ids=Arrays.copyOfRange(ids,first,first+count);
                } else {
                    byte[] states=new byte[ids.length]; random.nextBytes(states);
                    List<Integer> chosen=new ArrayList<>();
                    for(int i=0;i<ids.length;i++) if((states[i]&0x81)==0x81) chosen.add(ids[i]);
                    var selected=value.filtered(states,0x81,M3IndexMaskCompactor.Backend.JAVA);
                    if(nativeMode) same(selected.physicalIndices(),value.filtered(states,0x81,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).physicalIndices(),"filtered range JNI parity");
                    value=selected; ids=chosen.stream().mapToInt(Integer::intValue).toArray();
                }
            }
        }
        var tied=M3LongIndexProjection.snapshot(new long[]{2,1,2,1,2,0,1});
        var ascending=tied.sortedAscending().slice(1,5);
        same(new int[]{0,2,1,3,6},ascending.sortedDescending().physicalIndices(),"split tie groups");
        var empty=tied.slice(7,0).sortedDescending().sortedAscending();
        check(empty.isEmpty(),"empty offset view");
    }

    public static void nullableRepresentative() {
        for(Comparator<String> comparator : List.of(Comparator.nullsFirst(Comparator.<String>naturalOrder()),
                Comparator.nullsLast(Comparator.<String>naturalOrder()))) {
            NavigableSet<String> backing=new TreeSet<>(comparator);
            backing.add(null); backing.add("a");
            for(NavigableSet<String> view : List.of(backing,backing.descendingSet())) {
                int[] seen={0};
                var filtered=M3FilteredNavigableSet.view(view,v->{if(v==null)seen[0]++;return true;});
                check(filtered.contains(null),"nullable stored representative");
                check(seen[0]==1,"null predicate call count");
                check(!M3FilteredNavigableSet.view(view,v->v!=null).contains(null),"null predicate rejection");
            }
        }
        Comparator<String> equivalent=Comparator.comparingInt(v->v==null||v.isEmpty()?0:1);
        NavigableSet<String> backing=new TreeSet<>(equivalent); backing.add(null); backing.add("x");
        check(M3FilteredNavigableSet.view(backing,v->v==null).contains(""),"nonnull probe, null stored representative");
        System.out.println("NULL_REPRESENTATIVE checks="+checks+" PASS");
    }

    private static void wrappers() {
        record Member(int key,boolean visible) { }
        var comparator=Comparator.comparingInt(Member::key);
        NavigableSet<Member> backing=new TreeSet<>(comparator);
        Member a=new Member(1,true),b=new Member(2,false),c=new Member(3,true);
        backing.addAll(List.of(a,b,c)); int[] calls={0};
        Predicate<Member> predicate=v->{calls[0]++;return v.visible();};
        NavigableSet<Member> view=M3FilteredNavigableSet.view(backing,predicate);
        check(view.contains(new Member(1,false)),"predicate must see stored representative");
        check(!view.contains(new Member(2,true)),"probe is not predicate owner");
        check(view.first()==a&&view.last()==c,"identity through sorted wrapper");
        Iterator<Member> iterator=view.iterator(); calls[0]=0;
        check(iterator.hasNext()&&iterator.hasNext()&&calls[0]==1,"iterator preparation at most once");
        check(iterator.next()==a&&iterator.next()==c,"filtered encounter order");
        check(!iterator.hasNext()&&!iterator.hasNext(),"stable exhaustion");
        throwsType(UnsupportedOperationException.class,()->view.removeIf(v->{throw new AssertionError("mutation invoked predicate");}));
        throwsType(UnsupportedOperationException.class,()->view.addAll(List.of()));
        throwsType(UnsupportedOperationException.class,view::clear);
        check(backing.size()==3,"read-only wrapper mutated backing");
        backing.remove(c); check(view.size()==1,"live backing mutation");
        boolean[] accept={false}; var mutable=M3FilteredNavigableSet.view(backing,v->accept[0]);
        check(mutable.isEmpty(),"dynamic predicate false"); accept[0]=true;
        check(mutable.size()==2,"dynamic predicate recomputed");
        check(view.comparator()==comparator,"comparator identity");
    }

    private static void allocation() {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation observation unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long[] values=new long[32768]; for(int i=0;i<values.length;i++)values[i]=i/8;
        var value=M3LongIndexProjection.snapshot(values).sortedAscending().slice(5,32750);
        for(int i=0;i<100;i++) escape=value.sortedDescending();
        for(int sample=0;sample<3;sample++) {
            long before=bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
            for(int i=0;i<100;i++)escape=value.sortedDescending();
            long after=bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
            System.out.println("SORT_ALLOCATION sample="+sample+" units=32750 operations=100 bytes="+(after-before));
        }
    }

    public static void main(String[] args) throws Exception {
        String mode=args.length==0?"common":args[0]; checks=0;
        if(mode.equals("nullable")) {nullableRepresentative();return;}
        if(mode.equals("allocation")) {allocation();return;}
        nativeMode=args.length>1&&args[1].equals("jni");
        if(nativeMode)check(M3IndexMaskCompactor.nativeAvailable(),"native library required for proof");
        else {
            check(!M3IndexMaskCompactor.nativeAvailable(),"isolated missing-library JVM required");
            for(int limit:new int[]{0,1}) throwsType(UnsatisfiedLinkError.class,()->M3IndexMaskWindow.scan(
                    new int[1],new byte[1],0,1,1,new int[1],0,limit,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        observations=MessageDigest.getInstance("SHA-256");
        windows();windowFailures();sorts();wrappers();
        System.out.println("ORDER_WINDOW checks="+checks+" native="+nativeMode+" observations="+
                HexFormat.of().formatHex(observations.digest())+" PASS");
    }
}
