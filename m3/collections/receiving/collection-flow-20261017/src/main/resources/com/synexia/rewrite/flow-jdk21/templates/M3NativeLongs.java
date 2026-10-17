// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
/** Optional bulk primitive execution. Host explicitly loads the library; default remains Java. */
public final class M3NativeLongs {
    public enum Backend {
        JAVA,NATIVE
    }
    public static final int MAX_GROUPS=1_048_576;
    private M3NativeLongs() {
    }
    public static long[] sumByDenseKey(int[] keys,long[] values,int groups) {
        return sumByDenseKey(keys,values,groups,Backend.JAVA,null);
    }
    public static long[] sumByDenseKey(int[] keys,long[] values,int groups,Backend backend,M3ProgressMonitor m) {
        Objects.requireNonNull(keys);
        Objects.requireNonNull(values);
        Objects.requireNonNull(backend);
        if(keys.length!=values.length||groups<0||groups>MAX_GROUPS)throw new IllegalArgumentException("length/group bound");
        checkpoint(m,0);
        if(backend==Backend.NATIVE)return sumNativeMonitored(keys,values,groups,m);
        long[] result=new long[groups];
        for(int start=0;start<keys.length;) {
            checkpoint(m,0);
            int n=Math.min(1024,keys.length-start);
            for(int j=0;j<n;j++) {
                int i=start+j,k=keys[i];
                if(k<0||k>=groups)throw new IllegalArgumentException("key outside group domain");
                result[k]+=values[i];
            }
            checkpoint(m,n);
            start+=n;
        }
        checkpoint(m,0);
        return result;
    }
    public static long[] prefixSum(long[] values,long identity) {
        return prefixSum(values,identity,Backend.JAVA,null);
    }
    public static long[] prefixSum(long[] values,long identity,Backend backend,M3ProgressMonitor m) {
        Objects.requireNonNull(values);
        Objects.requireNonNull(backend);
        checkpoint(m,0);
        if(backend==Backend.NATIVE)return scanNative(values,identity,m);
        long[] result=new long[values.length];
        long total=identity;
        for(int start=0;start<values.length;) {
            checkpoint(m,0);
            int n=Math.min(1024,values.length-start);
            for(int j=0;j<n;j++) {
                int i=start+j;
                total+=values[i];
                result[i]=total;
            }
            checkpoint(m,n);
            start+=n;
        }
        checkpoint(m,0);
        return result;
    }
    private static void checkpoint(M3ProgressMonitor m,int worked) {
        M3Flow.check(m);
        if(m!=null&&worked>0)synchronized(m) {
            m.worked(worked);
        }
        M3Flow.check(m);
    }
    // Retained legacy private JNI symbol; the monitored entry is additive.
    private static native long[] sumNative(int[] keys,long[] values,int groups);
    private static native long[] sumNativeMonitored(int[] keys,long[] values,int groups,M3ProgressMonitor m);
    private static native long[] scanNative(long[] values,long identity,M3ProgressMonitor m);

    /** Explicit owning-array terminal; no implicit JNI selection or fallback. */
    public static long[] windowSums(long[] values, int width, int step, M3Windows.Tail tail) {
        return windowSums(values, width, step, tail, Backend.JAVA, null);
    }

    /**
     * Inputs must be stable for the entire call, including progress callbacks.
     * Java reports per-input progress; native reports batches of at most 1024.
     */
    public static long[] windowSums(long[] values, int width, int step, M3Windows.Tail tail,
            Backend backend, M3ProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(tail, "tail");
        Objects.requireNonNull(backend, "backend");
        if (width <= 0 || step <= 0) throw new IllegalArgumentException("positive width/step required");
        checkpoint(monitor, 0);
        if (backend == Backend.NATIVE) {
            return windowsNative(values, width, step, tail == M3Windows.Tail.PARTIAL, monitor);
        }
        return M3LongFlow.of(values).windowSum(width, step, tail).toArray(monitor);
    }

    private static native long[] windowsNative(long[] values, int width, int step,
            boolean partial, M3ProgressMonitor monitor);
}
