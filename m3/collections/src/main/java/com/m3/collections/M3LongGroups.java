// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Intermediate distinct-key aggregate continuation. */
public final class M3LongGroups<K> {
    @FunctionalInterface public interface KeyLongPredicate<K> {
        boolean test(K key,long value);
    }
    interface Terminal<K> {
        void run(ObjLongConsumer<? super K> sink,M3ProgressMonitor m);
    }
    private final Terminal<K> terminal;
    M3LongGroups(Terminal<K> terminal) {
        this.terminal=terminal;
    }
    public M3LongGroups<K> filterGroups(KeyLongPredicate<? super K> p) {
        Objects.requireNonNull(p);
        return new M3LongGroups<>((s,m)->terminal.run((k,v)-> {
            boolean keep=p.test(k,v);
            M3Flow.check(m);
            if(keep)s.accept(k,v);
        }
        ,m));
    }
    public M3LongGroups<K> mapLong(LongUnaryOperator op) {
        Objects.requireNonNull(op);
        return new M3LongGroups<>((s,m)->terminal.run((k,v)-> {
            long x=op.applyAsLong(v);
            M3Flow.check(m);
            s.accept(k,x);
        }
        ,m));
    }
    public void forEach(ObjLongConsumer<? super K> action) {
        forEach(action,null);
    }
    public void forEach(ObjLongConsumer<? super K> action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        M3Flow.check(m);
        terminal.run(action,m);
        M3Flow.check(m);
    }
    public Map<K,Long> toMap() {
        return toMap(null);
    }
    public Map<K,Long> toMap(M3ProgressMonitor m) {
        Map<K,Long> result=new LinkedHashMap<>();
        forEach(result::put,m);
        return Collections.unmodifiableMap(result);
    }
    public long countGroups() {
        return countGroups(null);
    }
    public long countGroups(M3ProgressMonitor m) {
        long[] n= {
            0
        }
        ;
        forEach((k,v)->n[0]++,m);
        return n[0];
    }
}
