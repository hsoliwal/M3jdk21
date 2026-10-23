/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import static com.m3.collections.M3IndexMaskCompactor.Backend.*;

/** Real index/mask/address owners, including explicit required-JNI runs. No substitute implementations. */
public final class M3ProjectionRefinementProbe {
    private static long checks;
    private static volatile M3LongIndexProjection retained;
    private M3ProjectionRefinementProbe() { }
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void rejects(Runnable action, Class<? extends Throwable> kind) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing " + kind.getSimpleName());
    }
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "java" : args[0];
        if (mode.equals("measure")) { measure(); return; }
        if (mode.equals("absent")) {
            check(!M3IndexMaskCompactor.nativeAvailable(), "mask JNI must be absent");
            check(!M3LongAddressKernel.nativeAvailable(), "address JNI must be absent");
            rejects(() -> M3IndexMaskCompactor.selectPositions(new byte[0], 0, NATIVE_REQUIRED), UnsatisfiedLinkError.class);
            rejects(() -> M3LongAddressKernel.gather(new long[0], new int[0], 0, 0, NATIVE_REQUIRED), UnsatisfiedLinkError.class);
            System.out.println("PROJECTION_ABSENT checks=" + checks + " failures=0"); return;
        }
        boolean nativeMode = mode.equals("native");
        if (nativeMode) {
            check(M3IndexMaskCompactor.nativeAvailable(), "required mask JNI not loaded");
            check(M3LongAddressKernel.nativeAvailable(), "required gather JNI not loaded");
        }
        var backend = nativeMode ? NATIVE_REQUIRED : JAVA;
        Random random = new Random(0x4d335649455753L);
        Field valuesField = M3LongIndexProjection.class.getDeclaredField("values");
        valuesField.setAccessible(true);
        for (int n : new int[] {0,1,2,3,31,32,33,255,256,257,1023}) {
            for (int trial = 0; trial < 48; trial++) {
                long[] payload = new long[n];
                for (int i = 0; i < n; i++) payload[i] = i % 17 == 0 ? Long.MIN_VALUE : i % 19 == 0 ? Long.MAX_VALUE : random.nextInt(23)-11;
                long[] external = payload.clone();
                var view = M3LongIndexProjection.snapshot(external);
                Arrays.fill(external, 99);
                int[] ids = new int[n];
                for (int i = 0; i < n; i++) ids[i] = i;
                Object owner = valuesField.get(view);
                for (int step = 0; step < 12; step++) {
                    if (step % 4 == 2) {
                        int start = random.nextInt(ids.length+1);
                        int length = random.nextInt(ids.length-start+1);
                        ids = Arrays.copyOfRange(ids, start, start+length);
                        view = view.slice(start,length);
                    } else if (step % 4 == 3) {
                        byte[] states = new byte[ids.length]; random.nextBytes(states);
                        int mask = new int[] {0,1,128,255}[trial % 4];
                        int[] filtered = new int[ids.length]; int count=0;
                        for (int i=0;i<ids.length;i++) if (((states[i]&255)&mask)==mask) filtered[count++]=ids[i];
                        ids=Arrays.copyOf(filtered,count);
                        view=view.filtered(states,mask,backend);
                        Arrays.fill(states,(byte)0); // no retained caller-state ownership
                    } else {
                        boolean descending = step % 2 == 1;
                        Integer[] boxed = Arrays.stream(ids).boxed().toArray(Integer[]::new);
                        Comparator<Integer> order = Comparator.comparingLong(i -> payload[i]);
                        Arrays.sort(boxed, descending ? order.reversed() : order);
                        ids = Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
                        view = descending ? view.sortedDescending() : view.sortedAscending();
                    }
                    check(Arrays.equals(ids, view.physicalIndices()), "stable physical occurrence order");
                    check(valuesField.get(view) == owner, "single payload owner preserved");
                    long[] expected = new long[ids.length];
                    for (int i=0;i<ids.length;i++) {
                        expected[i]=payload[ids[i]];
                        check(view.get(i)==expected[i] && view.physicalIndex(i)==ids[i], "index and scalar lookup");
                    }
                    check(Arrays.equals(expected, view.toArray()), "materialization");
                    check(Arrays.equals(expected,M3LongAddressKernel.gather(payload,ids,0,ids.length,backend)), "Java/JNI gather");
                    int[] exported = view.physicalIndices();
                    Arrays.fill(exported,-1);
                    check(Arrays.equals(ids,view.physicalIndices()), "index export isolated");
                }
            }
        }
        var ties=M3LongIndexProjection.snapshot(new long[]{5,1,5,3,1,5}).sortedAscending();
        check(Arrays.equals(new int[]{0,2,5,3,1,4},ties.sortedDescending().physicalIndices()),"whole-run reversal preserves ties");
        check(Arrays.equals(ties.physicalIndices(),ties.sortedDescending().sortedAscending().physicalIndices()),"exact round trip");
        rejects(() -> ties.slice(-1,1), IndexOutOfBoundsException.class);
        rejects(() -> ties.filtered(new byte[6],256,backend), IllegalArgumentException.class);
        rejects(() -> ties.filtered(new byte[5],0,backend), IllegalArgumentException.class);
        if (nativeMode) nativeWindows();
        System.out.println("PROJECTION_REFINEMENT mode="+mode+" checks="+checks+" failures=0 real_owner=YES");
    }
    private static void nativeWindows() {
        Random rng=new Random(19);
        int[] rows=new int[530]; long[] longs=new long[530]; byte[] states=new byte[540];
        rng.nextBytes(states);
        for(int i=0;i<rows.length;i++){rows[i]=rng.nextInt(); longs[i]=i%3==0?Long.MIN_VALUE:rng.nextLong();}
        for(int n:new int[]{0,1,255,256,257,512}) for(int mask=0;mask<256;mask++) {
            check(Arrays.equals(M3IndexMaskCompactor.selectWindow(rows,7,states,11,n,mask,JAVA),
                    M3IndexMaskCompactor.selectWindow(rows,7,states,11,n,mask,NATIVE_REQUIRED)),"independent int offsets");
            check(Arrays.equals(M3IndexMaskCompactor.selectLongWindow(longs,7,states,11,n,mask,JAVA),
                    M3IndexMaskCompactor.selectLongWindow(longs,7,states,11,n,mask,NATIVE_REQUIRED)),"independent long offsets");
            byte[] slice=Arrays.copyOfRange(states,11,11+n);
            check(Arrays.equals(M3IndexMaskCompactor.selectPositions(slice,mask,JAVA),
                    M3IndexMaskCompactor.selectPositions(slice,mask,NATIVE_REQUIRED)),"native coordinate selection");
        }
        long[] payload={Long.MIN_VALUE,4,Long.MAX_VALUE};
        for(int bad:new int[]{0,1,255,256,257,511}) {
            int[] ids=new int[512]; for(int i=0;i<ids.length;i++) ids[i]=i%3;
            ids[bad]=3;
            long[] javaOutput=new long[520],nativeOutput=new long[520];
            Arrays.fill(javaOutput,19);Arrays.fill(nativeOutput,19);
            rejects(() -> M3LongAddressKernel.gatherInto(payload,ids,0,512,javaOutput,4,JAVA),IndexOutOfBoundsException.class);
            rejects(() -> M3LongAddressKernel.gatherInto(payload,ids,0,512,nativeOutput,4,NATIVE_REQUIRED),IndexOutOfBoundsException.class);
            check(Arrays.equals(javaOutput,nativeOutput),"exact exceptional prefix across JNI chunk");
        }
    }
    private static void measure() {
        int n=262_144, repeats=40;
        long[] payload=new long[n];for(int i=0;i<n;i++)payload[i]=i/7;
        var ascending=M3LongIndexProjection.snapshot(payload).sortedAscending();
        for(int i=0;i<30;i++)retained=ascending.sortedDescending();
        var threads=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        check(threads.isThreadAllocatedMemorySupported() && threads.isCurrentThreadCpuTimeSupported(),"measurement counters available");
        threads.setThreadAllocatedMemoryEnabled(true);threads.setThreadCpuTimeEnabled(true);
        ManagementFactory.getMemoryMXBean().gc();
        long id=Thread.currentThread().threadId();
        long bytes=threads.getThreadAllocatedBytes(id), cpu=threads.getCurrentThreadCpuTime(),wall=System.nanoTime();
        for(int i=0;i<repeats;i++)retained=ascending.sortedDescending();
        long elapsed=System.nanoTime()-wall,usedCpu=threads.getCurrentThreadCpuTime()-cpu,allocated=threads.getThreadAllocatedBytes(id)-bytes;
        check(retained.size()==n && retained.get(0)==payload[n-1],"benchmark result consumed");
        ManagementFactory.getMemoryMXBean().gc();
        long heapUsed=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        java.lang.ref.Reference.reachabilityFence(ascending);
        System.out.println("{\"heapUsedAfterGcRequest\":"+heapUsed+",\"size\":"+n+",\"operations\":"+repeats+",\"allocatedBytes\":"+allocated+",\"cpuNanos\":"+usedCpu+",\"wallNanos\":"+elapsed+",\"scope\":\"known-direction conversion only\"}");
    }
}
