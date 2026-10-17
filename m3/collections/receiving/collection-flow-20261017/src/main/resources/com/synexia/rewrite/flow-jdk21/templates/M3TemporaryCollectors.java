// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Explicit scoped terminals, not a standard Collector that pretends it has an abort hook. */
public final class M3TemporaryCollectors {
    private M3TemporaryCollectors() {
    }
    public static <T,R> R withView(M3Flow<T> flow,Function<? super M3IndexView<T>,? extends R> callback) {
        return withView(flow,callback,null);
    }
    public static <T,R> R withView(M3Flow<T> flow,Function<? super M3IndexView<T>,? extends R> callback,M3ProgressMonitor m) {
        Objects.requireNonNull(flow);
        Objects.requireNonNull(callback);
        try(M3IndexView<T> view=flow.virtualize(m)) {
            M3Flow.check(m);
            R result=callback.apply(view);
            view.size();
            M3Flow.check(m);
            return result;
        }
    }
}
