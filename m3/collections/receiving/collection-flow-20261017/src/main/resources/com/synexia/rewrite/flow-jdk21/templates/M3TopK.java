// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** First k indices in client comparator order. Ties retain source order. O(k) index storage.
 * Comparator must be consistent for the duration of the terminal; negative means comes first. */
public final class M3TopK<T> {
    private final M3Flow<T> flow;
    private final int k;
    private final IntBinaryOperator order;
    M3TopK(M3Flow<T> flow,int k,IntBinaryOperator order) {
        if(k<0)throw new IllegalArgumentException("negative k");
        this.flow=flow;
        this.k=k;
        this.order=Objects.requireNonNull(order);
    }
    public M3IndexView<T> toView() {
        return toView(null);
    }
    public M3IndexView<T> toView(M3ProgressMonitor m) {
        var snap=flow.snapshot(m);
        int capacity=Math.min(k,snap.size());
        if(capacity>flow.maxGroups)throw new IllegalArgumentException("top-k state budget");
        int[] heap=new int[capacity];
        int[] size= {
            0
        }
        ;
        IntBinaryOperator rank=(a,b)-> {
            M3Flow.check(m);
            flow.source.guard(snap.version());
            int c=order.applyAsInt(a,b);
            flow.source.guard(snap.version());
            M3Flow.check(m);
            return c==0?Integer.compare(a,b):c;
        }
        ;
        if(capacity>0)flow.walk(snap,false,(i,v)-> {
            if(size[0]<capacity) {
                int at=size[0]++;
                heap[at]=i;
                while(at>0) {
                    int parent=(at-1)/2;
                    if(rank.applyAsInt(heap[at],heap[parent])<=0)break;
                    swap(heap,at,parent);
                    at=parent;
                }
            }
            else if(rank.applyAsInt(i,heap[0])<0) {
                heap[0]=i;
                down(heap,size[0],0,rank);
            }
            return true;
        }
        ,m);
        int[] result=new int[size[0]];
        for(int at=result.length-1;at>=0;at--) {
            M3Flow.check(m);
            result[at]=heap[0];
            heap[0]=heap[--size[0]];
            down(heap,size[0],0,rank);
        }
        flow.source.guard(snap.version());
        return new M3IndexView<>(flow.source,snap.version(),result);
    }
    public void forEach(Consumer<? super T> action) {
        forEach(action,null);
    }
    public void forEach(Consumer<? super T> action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        try(var view=toView(m)) {
            for(int i=0;i<view.size();i++) {
                M3Flow.check(m);
                action.accept(view.get(i));
                view.size();
            }
            M3Flow.check(m);
        }
    }
    public List<T> toList() {
        return toList(null);
    }
    public List<T> toList(M3ProgressMonitor m) {
        try(var view=toView(m)) {
            return view.materialize(m);
        }
    }
    public M3LongFlow mapIndexToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new M3LongFlow((sink,m)-> {
            try(var v=toView(m)) {
                for(int i=0;i<v.size();i++) {
                    M3Flow.check(m);
                    long x=mapper.applyAsLong(v.sourceIndex(i));
                    v.size();
                    if(!sink.accept(x))break;
                }
            }
        }
        ,flow.maxRows);
    }
    private static void down(int[] heap,int n,int at,IntBinaryOperator rank) {
        while(at<n/2) {
            int left=at*2+1,right=left+1,worst=left;
            if(right<n&&rank.applyAsInt(heap[right],heap[left])>0)worst=right;
            if(rank.applyAsInt(heap[worst],heap[at])<=0)break;
            swap(heap,at,worst);
            at=worst;
        }
    }
    private static void swap(int[] h,int a,int b) {
        int t=h[a];
        h[a]=h[b];
        h[b]=t;
    }
}
