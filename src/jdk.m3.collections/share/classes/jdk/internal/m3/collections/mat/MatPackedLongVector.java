/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.mat;

import jdk.m3.arrays.SelectionBits;

import java.util.Objects;

/**
 * Fixed-width packed long values, 1..64 bits, supporting signed or unsigned interpretation.
 * Width 64 stores every raw long bit pattern.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPackedLongVector.java}
 * blob {@code 473dcd516e995f8ebf12cb3d8eb2830c517d3a22} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatPackedLongVector {
    private final int size;
    private final int width;
    private final boolean signed;
    private final long[] words;

    public MatPackedLongVector(int size, int width, boolean signed) {
        if(size<0) throw new IllegalArgumentException("negative size");
        if(width<1 || width>64) throw new IllegalArgumentException("width must be 1..64");
        long bits=Math.multiplyExact((long)size,width);
        if(bits>Integer.MAX_VALUE*64L) throw new IllegalArgumentException("packed vector too large");
        this.size=size;
        this.width=width;
        this.signed=signed;
        this.words=new long[(int)((bits+63)>>>6)];
    }

    public static MatPackedLongVector unsigned(int size,int width){ return new MatPackedLongVector(size,width,false); }
    public static MatPackedLongVector signed(int size,int width){ return new MatPackedLongVector(size,width,true); }

    public int size(){ return size; }
    public int width(){ return width; }
    public boolean signed(){ return signed; }
    public long payloadBytes(){ return 8L*words.length; }

    public long get(int index){
        Objects.checkIndex(index,size);
        long raw=readRaw(index);
        if(!signed || width==64) return raw;
        return (raw<<(64-width))>>(64-width);
    }

    public void set(int index,long value){
        Objects.checkIndex(index,size);
        checkValue(value);
        writeRaw(index,value & mask(width));
    }

    public SelectionBits equalTo(long value){
        checkValue(value);
        SelectionBits.Builder bits=SelectionBits.builder(size);
        for(int i=0;i<size;i++) if(get(i)==value) bits.set(i);
        return bits.build();
    }

    public SelectionBits greaterOrEqual(long value){
        checkValue(value);
        SelectionBits.Builder bits=SelectionBits.builder(size);
        for(int i=0;i<size;i++){
            long current=get(i);
            boolean match=signed ? current>=value : Long.compareUnsigned(current,value)>=0;
            if(match) bits.set(i);
        }
        return bits.build();
    }

    private long readRaw(int index){
        long bit=(long)index*width;
        int word=(int)(bit>>>6);
        int shift=(int)(bit&63);
        long value=words[word]>>>shift;
        if(shift!=0 && width>64-shift) value|=words[word+1]<<(64-shift);
        return value & mask(width);
    }

    private void writeRaw(int index,long value){
        long bit=(long)index*width;
        int word=(int)(bit>>>6);
        int shift=(int)(bit&63);
        long m=mask(width);
        words[word]=(words[word]&~(m<<shift))|(value<<shift);
        if(shift!=0 && width>64-shift){
            int high=width-(64-shift);
            long hm=mask(high);
            words[word+1]=(words[word+1]&~hm)|(value>>>(64-shift));
        }
    }

    private void checkValue(long value){
        if(width==64) return;
        if(signed){
            long bound=1L<<(width-1);
            if(value < -bound || value >= bound) throw new IllegalArgumentException("signed value exceeds width");
        }else if(value<0 || (value & ~mask(width))!=0){
            throw new IllegalArgumentException("unsigned value exceeds width");
        }
    }

    static long mask(int width){
        if(width<1 || width>64) throw new IllegalArgumentException("width");
        return width==64 ? -1L : (1L<<width)-1L;
    }
}
