// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.*;
/** One terminal-owned key table and parallel primitive summary lanes, never one row object. */
final class VirtualLongAccumulator<K> implements AutoCloseable {
    private final HashMap<K,Integer> positions=new HashMap<>();
    private final ArrayList<K> keys=new ArrayList<>();
    private final int maximum;
    private long[] count=new long[0],sum=new long[0],min=new long[0],max=new long[0];
    private boolean open=true;
    VirtualLongAccumulator(int maximum) {
        this.maximum=maximum;
    }
    int position(K key) {
        active();
        Integer found=positions.get(key);
        if(found!=null)return found;
        int p=keys.size();
        if(p==maximum)throw new IllegalArgumentException("group budget");
        if(p==sum.length) {
            int cap=(int)Math.min(maximum,Math.max(8L,2L*p));
            count=Arrays.copyOf(count,cap);
            sum=Arrays.copyOf(sum,cap);
            min=Arrays.copyOf(min,cap);
            max=Arrays.copyOf(max,cap);
        }
        keys.add(key);
        positions.put(key,p);
        return p;
    }
    void add(int p,long value) {
        active();
        if(count[p]++==0) {
            min[p]=max[p]=value;
        }
        else {
            min[p]=Math.min(min[p],value);
            max[p]=Math.max(max[p],value);
        }
        sum[p]+=value;
    }
    int size() {
        active();
        return keys.size();
    }
    K key(int p) {
        active();
        return keys.get(p);
    }
    long count(int p) {
        active();
        return count[p];
    }
    long sum(int p) {
        active();
        return sum[p];
    }
    M3LongSummary summary(int p) {
        active();
        return new M3LongSummary(count[p],sum[p],min[p],max[p]);
    }
    private void active() {
        if(!open)throw new IllegalStateException("closed aggregate");
    }
    @Override public void close() {
        open=false;
        keys.clear();
        positions.clear();
        count=sum=min=max=new long[0];
    }
}
