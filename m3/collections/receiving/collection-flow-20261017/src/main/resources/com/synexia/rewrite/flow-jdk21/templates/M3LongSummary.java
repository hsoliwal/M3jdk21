// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
/** Owning summary output. Sum wraps exactly like Java long; average inherits that sum contract. */
public record M3LongSummary(long count,long sum,long min,long max) {
    public M3LongSummary {
        if(count<0)throw new IllegalArgumentException("negative count");
    }
    public double average() {
        return count==0?0.0:(double)sum/count;
    }
}
