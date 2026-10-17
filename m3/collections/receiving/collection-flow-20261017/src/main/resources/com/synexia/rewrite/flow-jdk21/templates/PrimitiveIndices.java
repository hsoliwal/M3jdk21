// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.Arrays;
/** Primitive membership buffer; no element wrapper per source row. */
final class PrimitiveIndices {
    int[] data=new int[0];
    int size;
    private final int maximum;
    PrimitiveIndices(int maximum) {
        this.maximum=maximum;
    }
    void add(int value) {
        if(size==maximum) throw new IllegalArgumentException("membership budget");
        if(size==data.length) data=Arrays.copyOf(data,(int)Math.min(maximum,Math.max(8L,2L*size)));
        data[size++]=value;
    }
    void append(PrimitiveIndices right) {
        if(right==this) throw new IllegalArgumentException("self combine");
        if((long)size+right.size>maximum) throw new IllegalArgumentException("membership budget");
        int n=size+right.size;
        if(n>data.length) data=Arrays.copyOf(data,n);
        System.arraycopy(right.data,0,data,size,right.size);
        size=n;
    }
    int[] array() {
        return Arrays.copyOf(data,size);
    }
}
