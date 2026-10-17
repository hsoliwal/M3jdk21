// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
/** Key grouping is separate from scheduling and concurrency safety. */
public final class M3KeyedFlow<T,K> {
    interface Key<T,K> {
        K get(int index,T value);
    }
    interface LongValue<T> {
        long get(int index,T value);
    }
    private final M3Flow<T> flow;
    private final boolean needsValue;
    private final Key<T,K> key;
    M3KeyedFlow(M3Flow<T> flow,boolean needsValue,Key<T,K> key) {
        this.flow=flow;
        this.needsValue=needsValue;
        this.key=key;
    }
    public M3LongGroups<K> sumLong(ToLongFunction<? super T> v) {
        Objects.requireNonNull(v);
        return aggregate(true,(i,row)->v.applyAsLong(row),false);
    }
    public M3LongGroups<K> sumLongAt(IntToLongFunction v) {
        Objects.requireNonNull(v);
        return aggregate(false,(i,row)->v.applyAsLong(i),false);
    }
    public M3LongGroups<K> counting() {
        return aggregate(false,(i,row)->0L,true);
    }
    private M3LongGroups<K> aggregate(boolean valueNeeded,LongValue<T> v,boolean count) {
        return new M3LongGroups<>((sink,m)-> {
            try(VirtualLongAccumulator<K> a=new VirtualLongAccumulator<>(flow.maxGroups)) {
                var snap=flow.walk(needsValue||valueNeeded,(i,row)-> {
                    K k=key.get(i,row);
                    M3Flow.check(m);
                    int p=a.position(k);
                    a.add(p,v.get(i,row));
                    return true;
                }
                ,m);
                for(int i=0;i<a.size();i++) {
                    M3Flow.check(m);
                    flow.source.guard(snap.version());
                    sink.accept(a.key(i),count?a.count(i):a.sum(i));
                    flow.source.guard(snap.version());
                }
                M3Flow.check(m);
            }
        }
        );
    }
    public M3SummaryGroups<K> summarizingLong(ToLongFunction<? super T> v) {
        Objects.requireNonNull(v);
        return summaries(true,(i,row)->v.applyAsLong(row));
    }
    public M3SummaryGroups<K> summarizingLongAt(IntToLongFunction v) {
        Objects.requireNonNull(v);
        return summaries(false,(i,row)->v.applyAsLong(i));
    }
    private M3SummaryGroups<K> summaries(boolean valueNeeded,LongValue<T> v) {
        return new M3SummaryGroups<>((sink,m)-> {
            try(VirtualLongAccumulator<K> a=new VirtualLongAccumulator<>(flow.maxGroups)) {
                var snap=flow.walk(needsValue||valueNeeded,(i,row)-> {
                    K k=key.get(i,row);
                    M3Flow.check(m);
                    a.add(a.position(k),v.get(i,row));
                    return true;
                }
                ,m);
                for(int i=0;i<a.size();i++) {
                    M3Flow.check(m);
                    flow.source.guard(snap.version());
                    sink.accept(a.key(i),a.summary(i));
                    flow.source.guard(snap.version());
                }
                M3Flow.check(m);
            }
        }
        );
    }
    public <R> R withPartitions(Function<? super M3Partitions<T,K>,? extends R> callback) {
        return withPartitions(callback,null);
    }
    public <R> R withPartitions(Function<? super M3Partitions<T,K>,? extends R> callback,M3ProgressMonitor m) {
        Objects.requireNonNull(callback);
        try(var p=partitions(m)) {
            M3Flow.check(m);
            R r=callback.apply(p);
            p.check();
            M3Flow.check(m);
            return r;
        }
    }
    private M3Partitions<T,K> partitions(M3ProgressMonitor m) {
        try(VirtualLongAccumulator<K> a=new VirtualLongAccumulator<>(flow.maxGroups)) {
            PrimitiveIndices rows=new PrimitiveIndices(flow.maxRows),codes=new PrimitiveIndices(flow.maxRows);
            var snap=flow.walk(needsValue,(i,row)-> {
                int p=a.position(key.get(i,row));
                a.add(p,0L);
                rows.add(i);
                codes.add(p);
                return true;
            }
            ,m);
            int[] offsets=new int[a.size()+1];
            ArrayList<K> keys=new ArrayList<>(a.size());
            for(int p=0;p<a.size();p++) {
                M3Flow.check(m);
                keys.add(a.key(p));
                offsets[p+1]=Math.addExact(offsets[p],Math.toIntExact(a.count(p)));
            }
            int[] cursor=offsets.clone(),indices=new int[rows.size];
            for(int i=0;i<rows.size;i++) {
                M3Flow.check(m);
                indices[cursor[codes.data[i]]++]=rows.data[i];
            }
            flow.source.guard(snap.version());
            return new M3Partitions<>(flow.source,snap.version(),keys,offsets,indices);
        }
    }
/** Serial source order per key in this invocation only. Workers are joined before return.
     * Different keys still require non-conflicting, thread-safe source reads/user effects.
     * No rollback, automatic retry, cross-invocation key locking or interruption of arbitrary code. */
    public void forEachParallel(int parallelism,Consumer<? super T> action) {
        forEachParallel(parallelism,action,null);
    }
    public void forEachParallel(int parallelism,Consumer<? super T> action,M3ProgressMonitor m) {
        if(parallelism<=0)throw new IllegalArgumentException("positive parallelism required");
        Objects.requireNonNull(action);
        withPartitions(p-> {
            int workers=Math.min(parallelism,p.size());
            if(workers==0)return null;
            ExecutorService pool=Executors.newFixedThreadPool(workers);
            AtomicInteger next=new AtomicInteger();
            AtomicReference<Throwable> failure=new AtomicReference<>();
            boolean interrupted=false;
            try {
                for(int w=0;w<workers;w++)pool.execute(()-> {
                    try {
                        int g;
                        while(failure.get()==null&&(g=next.getAndIncrement())<p.size()) {
                            var part=p.partition(g);
                            for(int i=0;i<part.size()&&failure.get()==null;i++) {
                                M3Flow.check(m);
                                T value=part.get(i);
                                action.accept(value);
                                p.check();
                                M3Flow.worked(m);
                            }
                        }
                    }
                    catch(Throwable error) {
                        failure.compareAndSet(null,error);
                    }
                }
                );
            }
            catch(Throwable error) {
                failure.compareAndSet(null,error);
            }
            finally {
                pool.shutdown();
                for(;;) {
                    try {
                        if(pool.awaitTermination(1,TimeUnit.DAYS))break;
                    }
                    catch(InterruptedException error) {
                        interrupted=true;
                        failure.compareAndSet(null,new CancellationException("interrupted terminal"));
                        pool.shutdownNow();
                    }
                }
                if(interrupted)Thread.currentThread().interrupt();
            }
            Throwable error=failure.get();
            if(error instanceof RuntimeException r)throw r;
            if(error instanceof Error e)throw e;
            if(error!=null)throw new IllegalStateException(error);
            return null;
        }
        ,m);
    }
}
