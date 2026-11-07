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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Mixed-range vector: every fixed block independently chooses CONSTANT, PACKED, or PLAIN.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatBlockIntVector.java}
 * blob {@code 8995c943158d62b5b6941242f819e074e942a14c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatBlockIntVector {
    public static final int DEFAULT_BLOCK_SIZE=256;
    private static final byte CONSTANT=0, PACKED=1, PLAIN=2;
    private final int size, blockSize;
    private final byte[] kinds;
    private final int[] bases,widths,offsets;
    private final long[] packedData;
    private final int[] plainData;

    private M3MatBlockIntVector(
            int size,int blockSize,byte[] kinds,int[] bases,int[] widths,int[] offsets,
            long[] packedData,int[] plainData){
        this.size=size;this.blockSize=blockSize;this.kinds=kinds;this.bases=bases;
        this.widths=widths;this.offsets=offsets;this.packedData=packedData;this.plainData=plainData;
    }

    public static M3MatBlockIntVector of(int[] values){return of(values,DEFAULT_BLOCK_SIZE);}

    public static M3MatBlockIntVector of(int[] values,int blockSize){
        Objects.requireNonNull(values,"values");
        if(blockSize<1)throw new IllegalArgumentException("blockSize");
        int blocks=values.length==0?0:1+(values.length-1)/blockSize;
        byte[] kinds=new byte[blocks];
        int[] bases=new int[blocks],widths=new int[blocks],offsets=new int[blocks];
        long packedWords=0;
        int plainCount=0;
        for(int b=0;b<blocks;b++){
            int from=b*blockSize,to=from+Math.min(blockSize,values.length-from);
            int min=values[from],max=values[from];
            for(int i=from+1;i<to;i++){int v=values[i];if(v<min)min=v;if(v>max)max=v;}
            long delta=(long)max-min;
            bases[b]=min;
            if(delta==0){kinds[b]=CONSTANT;continue;}
            int width=64-Long.numberOfLeadingZeros(delta);
            long words=((long)(to-from)*width+63)>>>6;
            if(8L*words+8<4L*(to-from)){
                kinds[b]=PACKED;widths[b]=width;offsets[b]=Math.toIntExact(packedWords);packedWords+=words;
            }else{
                kinds[b]=PLAIN;offsets[b]=plainCount;plainCount+=to-from;
            }
        }
        long[] packed=new long[Math.toIntExact(packedWords)];
        int[] plain=new int[plainCount];
        for(int b=0;b<blocks;b++){
            int from=b*blockSize,to=from+Math.min(blockSize,values.length-from);
            if(kinds[b]==PLAIN){
                System.arraycopy(values,from,plain,offsets[b],to-from);
            }else if(kinds[b]==PACKED){
                int width=widths[b];
                long[] temp=new long[(int)(((long)(to-from)*width+63)>>>6)];
                for(int i=from;i<to;i++)write(temp,(long)(i-from)*width,width,(long)values[i]-bases[b]);
                System.arraycopy(temp,0,packed,offsets[b],temp.length);
            }
        }
        return new M3MatBlockIntVector(values.length,blockSize,kinds,bases,widths,offsets,packed,plain);
    }

    public int size(){return size;}
    public int blockSize(){return blockSize;}
    public int blocks(){return kinds.length;}
    public int get(int index){
        Objects.checkIndex(index,size);
        int b=index/blockSize,within=index%blockSize;
        return switch(kinds[b]){
            case CONSTANT->bases[b];
            case PLAIN->plainData[offsets[b]+within];
            case PACKED->{int w=widths[b];yield (int)(bases[b]+read(packedData,((long)offsets[b]<<6)+(long)within*w,w));}
            default->throw new IllegalStateException();
        };
    }
    public int[] toArray(){int[] a=new int[size];for(int i=0;i<size;i++)a[i]=get(i);return a;}
    public long primitivePayloadBytes(){return kinds.length+12L*kinds.length+8L*packedData.length+4L*plainData.length;}
    public int constantBlocks(){return count(CONSTANT);}
    public int packedBlocks(){return count(PACKED);}
    public int plainBlocks(){return count(PLAIN);}
    private int count(byte k){int n=0;for(byte x:kinds)if(x==k)n++;return n;}

    private static long mask(int w){return w==64?-1L:(1L<<w)-1;}
    private static long read(long[] a,long bit,int w){
        int i=(int)(bit>>>6),s=(int)(bit&63);
        long v=a[i]>>>s;
        if(s!=0&&w>64-s)v|=a[i+1]<<(64-s);
        return v&mask(w);
    }
    private static void write(long[] a,long bit,int w,long v){
        int i=(int)(bit>>>6),s=(int)(bit&63);
        long m=mask(w);
        a[i]=(a[i]&~(m<<s))|(v<<s);
        if(s!=0&&w>64-s){int h=w-(64-s);long hm=mask(h);a[i+1]=(a[i+1]&~hm)|(v>>>(64-s));}
    }
}
