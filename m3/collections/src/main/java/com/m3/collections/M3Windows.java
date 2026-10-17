// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Streaming windows over selected source coordinates; O(min(width, source size)) indices.
 * Each callback gets one borrowed descriptor, not a materialized member list. */
public final class M3Windows<T> {
    public enum Tail {
        DROP,PARTIAL
    }
    private interface Sink<T> {
        boolean accept(Window<T> window);
    }
    private final M3Flow<T> flow;
    private final int width,step;
    private final Tail tail;
    M3Windows(M3Flow<T> flow,int width,int step,Tail tail) {
        if(width<=0||step<=0)throw new IllegalArgumentException("positive width/step required");
        this.flow=flow;
        this.width=width;
        this.step=step;
        this.tail=Objects.requireNonNull(tail);
    }
    private static final class Lease<T> {
        M3Flow.Source<T> source;
        int[] ring;
        final long version;
        long epoch,start;
        int length;
        Lease(M3Flow.Source<T> source,long version,int[] ring) {
            this.source=source;
            this.version=version;
            this.ring=ring;
        }
        void check(long expected) {
            if(source==null||epoch!=expected)throw new IllegalStateException("window expired");
            source.guard(version);
        }
    }
    public static final class Window<T> {
        private final Lease<T> lease;
        private final long epoch;
        private Window(Lease<T> lease) {
            this.lease=lease;
            epoch=lease.epoch;
        }
        public int size() {
            lease.check(epoch);
            return lease.length;
        }
        public int sourceIndex(int i) {
            Objects.checkIndex(i,size());
            return lease.ring[(int)((lease.start+i)%lease.ring.length)];
        }
        public T get(int i) {
            int index=sourceIndex(i);
            T value=lease.source.read.apply(index);
            lease.check(epoch);
            return value;
        }
        public int[] copyIndices() {
            return copyIndices(null);
        }
        public int[] copyIndices(M3ProgressMonitor m) {
            int[] result=new int[size()];
            for(int i=0;i<result.length;i++) {
                M3Flow.check(m);
                result[i]=sourceIndex(i);
            }
            M3Flow.check(m);
            return result;
        }
        public List<T> materialize() {
            return materialize(null);
        }
        public List<T> materialize(M3ProgressMonitor m) {
            var result=new ArrayList<T>(size());
            for(int i=0;i<size();i++) {
                M3Flow.check(m);
                result.add(get(i));
            }
            M3Flow.check(m);
            return Collections.unmodifiableList(result);
        }
    }
    public void forEach(Consumer<? super Window<T>> action) {
        forEach(action,null);
    }
    public void forEach(Consumer<? super Window<T>> action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        run(w-> {
            action.accept(w);
            return true;
        }
        ,m);
    }
    public M3LongFlow mapToLong(ToLongFunction<? super Window<T>> mapper) {
        Objects.requireNonNull(mapper);
        return new M3LongFlow((s,m)->run(w-> {
            long v=mapper.applyAsLong(w);
            M3Flow.check(m);
            return s.accept(v);
        }
        ,m),flow.maxRows);
    }
    private void run(Sink<T> sink,M3ProgressMonitor m) {
        M3Flow.Snapshot snap=flow.snapshot(m);
        int capacity=Math.min(width,snap.size());
        if(capacity>flow.maxGroups)throw new IllegalArgumentException("window state budget");
        Lease<T> lease=new Lease<>(flow.source,snap.version(),new int[Math.max(1,capacity)]);
        long[] seen= {
            0
        }
        ,next= {
            0
        }
        ;
        boolean[] running= {
            true
        }
        ;
        try {
            flow.walk(snap,false,(i,v)-> {
                lease.ring[(int)(seen[0]%lease.ring.length)]=i;
                seen[0]++;
                if(next[0]+width<=seen[0]) {
                    running[0]=emit(lease,next[0],width,sink,m);
                    next[0]+=step;
                }
                return running[0];
            }
            ,m);
            while(running[0]&&tail==Tail.PARTIAL&&next[0]<seen[0]) {
                running[0]=emit(lease,next[0],(int)(seen[0]-next[0]),sink,m);
                next[0]+=step;
            }
        }
        finally {
            lease.epoch++;
            lease.source=null;
            lease.ring=null;
        }
    }
    private static <T> boolean emit(Lease<T> lease,long start,int length,Sink<T> sink,M3ProgressMonitor m) {
        lease.epoch++;
        lease.start=start;
        lease.length=length;
        M3Flow.check(m);
        lease.source.guard(lease.version);
        try {
            boolean more=sink.accept(new Window<>(lease));
            lease.source.guard(lease.version);
            M3Flow.check(m);
            return more;
        }
        finally {
            lease.epoch++;
        }
    }
}
