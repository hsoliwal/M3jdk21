// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Summary objects are allocated per completed group, not per input row. */
public final class M3SummaryGroups<K> {
    interface Terminal<K> {
        void run(BiConsumer<? super K,? super M3LongSummary> sink,M3ProgressMonitor m);
    }
    private final Terminal<K> terminal;
    M3SummaryGroups(Terminal<K> terminal) {
        this.terminal=terminal;
    }
    public M3SummaryGroups<K> filterGroups(BiPredicate<? super K,? super M3LongSummary> p) {
        Objects.requireNonNull(p);
        return new M3SummaryGroups<>((s,m)->terminal.run((k,v)-> {
            boolean keep=p.test(k,v);
            M3Flow.check(m);
            if(keep)s.accept(k,v);
        }
        ,m));
    }
    public void forEach(BiConsumer<? super K,? super M3LongSummary> action) {
        forEach(action,null);
    }
    public void forEach(BiConsumer<? super K,? super M3LongSummary> action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        terminal.run(action,m);
    }
    public Map<K,M3LongSummary> toMap() {
        return toMap(null);
    }
    public Map<K,M3LongSummary> toMap(M3ProgressMonitor m) {
        Map<K,M3LongSummary> result=new LinkedHashMap<>();
        forEach(result::put,m);
        return Collections.unmodifiableMap(result);
    }
}
