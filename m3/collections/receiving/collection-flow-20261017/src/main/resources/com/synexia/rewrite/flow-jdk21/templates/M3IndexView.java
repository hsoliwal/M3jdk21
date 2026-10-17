// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
/** Read-only, borrowed owner view. Membership is fixed; payloads are not deeply snapshotted.
 * Close invalidates every slice and releases the adapter/indices. Closing is owner-thread-only. */
public final class M3IndexView<T> extends AbstractList<T> implements RandomAccess,AutoCloseable {
    private static final class Access<T> {
        M3Flow.Source<T> source;
        int[] indices;
        final long version;
        Access(M3Flow.Source<T> source,long version,int[] indices) {
            this.source=source;
            this.version=version;
            this.indices=indices;
        }
        void check() {
            if(source==null)throw new IllegalStateException("view closed");
            source.guard(version);
        }
    }
    private final Access<T> access;
    private final int start,length;
    M3IndexView(M3Flow.Source<T> source,long version,int[] indices) {
        this(new Access<>(source,version,indices),0,indices.length);
    }
    private M3IndexView(Access<T> access,int start,int length) {
        this.access=access;
        this.start=start;
        this.length=length;
    }
    @Override public int size() {
        access.check();
        return length;
    }
    public int sourceIndex(int i) {
        Objects.checkIndex(i,size());
        return access.indices[start+i];
    }
    @Override public T get(int i) {
        T value=access.source==null?closed():access.source.read.apply(sourceIndex(i));
        access.check();
        return value;
    }
    private T closed() {
        throw new IllegalStateException("view closed");
    }
    @Override public M3IndexView<T> subList(int from,int to) {
        Objects.checkFromToIndex(from,to,size());
        return new M3IndexView<>(access,start+from,to-from);
    }
    public int[] copyIndices() {
        return copyIndices(null);
    }
    public int[] copyIndices(M3ProgressMonitor m) {
        access.check();
        M3Flow.check(m);
        int[] copy=Arrays.copyOfRange(access.indices,start,start+length);
        access.check();
        M3Flow.check(m);
        return copy;
    }
    public List<T> materialize() {
        return materialize(null);
    }
    public List<T> materialize(M3ProgressMonitor m) {
        var list=new ArrayList<T>(size());
        for(int i=0;i<size();i++) {
            M3Flow.check(m);
            list.add(get(i));
            M3Flow.worked(m);
        }
        M3Flow.check(m);
        return Collections.unmodifiableList(list);
    }
    @Override public void close() {
        access.source=null;
        access.indices=null;
    }
}
