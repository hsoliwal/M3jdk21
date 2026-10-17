// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Temporary group membership over one parent-owned source; close invalidates every partition. */
public final class M3Partitions<T,K> implements AutoCloseable {
    private M3Flow.Source<T> source;
    private final long stamp;
    private List<K> keys;
    private int[] offsets,indices;
    M3Partitions(M3Flow.Source<T> source,long stamp,List<K> keys,int[] offsets,int[] indices) {
        this.source=source;
        this.stamp=stamp;
        this.keys=keys;
        this.offsets=offsets;
        this.indices=indices;
    }
    void check() {
        if(source==null)throw new IllegalStateException("partitions expired");
        source.guard(stamp);
    }
    public int size() {
        check();
        return keys.size();
    }
    public int rowCount() {
        check();
        return indices.length;
    }
    public Partition partition(int group) {
        Objects.checkIndex(group,size());
        return new Partition(group);
    }
    public final class Partition {
        private final int group;
        private Partition(int group) {
            this.group=group;
        }
        public K key() {
            check();
            return keys.get(group);
        }
        public int size() {
            check();
            return offsets[group+1]-offsets[group];
        }
        public int sourceIndex(int i) {
            Objects.checkIndex(i,size());
            return indices[offsets[group]+i];
        }
        public T get(int i) {
            int index=sourceIndex(i);
            T value=source.read.apply(index);
            check();
            return value;
        }
        public void forEach(Consumer<? super T> action) {
            forEach(action,null);
        }
        public void forEach(Consumer<? super T> action,M3ProgressMonitor m) {
            Objects.requireNonNull(action);
            for(int i=0;i<size();i++) {
                M3Flow.check(m);
                action.accept(get(i));
                check();
                M3Flow.worked(m);
            }
            M3Flow.check(m);
        }
        public int[] copyIndices() {
            return copyIndices(null);
        }
        public int[] copyIndices(M3ProgressMonitor m) {
            check();
            M3Flow.check(m);
            int[] result=Arrays.copyOfRange(indices,offsets[group],offsets[group+1]);
            check();
            M3Flow.check(m);
            return result;
        }
        public List<T> materialize() {
            return materialize(null);
        }
        public List<T> materialize(M3ProgressMonitor m) {
            var result=new ArrayList<T>(size());
            forEach(result::add,m);
            return Collections.unmodifiableList(result);
        }
    }
    @Override public void close() {
        source=null;
        keys=null;
        offsets=indices=null;
    }
}
