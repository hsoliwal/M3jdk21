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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * 16-bit chunked set; each chunk independently chooses ARRAY, RUNS or BITMAP.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatHybridIntSet.java}
 * blob {@code 39f209d3f2ee5493cf3b15bc8ca4a4021ea485f2} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatHybridIntSet {
    public enum ChunkKind { ARRAY, RUNS, BITMAP }
    private static final byte ARRAY=0,RUNS=1,BITMAP=2;
    // A 16-bit chunk has at most 65,536 values. RUNS is selected only with <=1,024 pairs.
    // Pack the count and pair count in the existing int lane: no extra retained index.
    private static final int COUNT_MASK=(1<<17)-1;
    private final int size;
    private final int[] chunkKeys;
    private final byte[] kinds;
    private final int[] offsets,counts;
    private final char[] arrayData;
    private final int[] runData;
    private final long[] bitmapData;

    private M3MatHybridIntSet(
            int size,int[] chunkKeys,byte[] kinds,int[] offsets,int[] counts,
            char[] arrayData,int[] runData,long[] bitmapData){
        this.size=size;this.chunkKeys=chunkKeys;this.kinds=kinds;this.offsets=offsets;
        this.counts=counts;this.arrayData=arrayData;this.runData=runData;this.bitmapData=bitmapData;
    }

    public static M3MatHybridIntSet of(int... source){
        Objects.requireNonNull(source,"source");
        int[] values=source.clone();
        Arrays.sort(values);
        int n=0;
        for(int x:values){
            if(x<0)throw new IllegalArgumentException("negative value");
            if(n==0||values[n-1]!=x)values[n++]=x;
        }
        return fromSortedUnique(Arrays.copyOf(values,n),n);
    }

    /** Caller must supply an ascending unique prefix; no array escapes into frozen state. */
    private static M3MatHybridIntSet fromSortedUnique(int[] values,int n){
        ArrayList<Integer> keys=new ArrayList<>();
        ArrayList<Byte> kindList=new ArrayList<>();
        ArrayList<int[]> lowLists=new ArrayList<>();
        int pos=0,arrayChars=0,runInts=0,bitmapWords=0;
        while(pos<n){
            int key=values[pos]>>>16,start=pos;
            while(pos<n&&(values[pos]>>>16)==key)pos++;
            int[] lows=new int[pos-start];
            for(int i=0;i<lows.length;i++)lows[i]=values[start+i]&0xffff;
            int runs=countRuns(lows);
            long ab=2L*lows.length,rb=8L*runs,bb=8192L;
            byte kind=ARRAY;
            long best=ab;
            if(rb<best){kind=RUNS;best=rb;}
            if(bb<best)kind=BITMAP;
            keys.add(key);kindList.add(kind);lowLists.add(lows);
            if(kind==ARRAY)arrayChars+=lows.length;
            else if(kind==RUNS)runInts+=2*runs;
            else bitmapWords+=1024;
        }
        int chunks=keys.size();
        int[] chunkKeys=new int[chunks],offsets=new int[chunks],counts=new int[chunks];
        byte[] kinds=new byte[chunks];
        char[] arrayData=new char[arrayChars];
        int[] runData=new int[runInts];
        long[] bitmapData=new long[bitmapWords];
        int ao=0,ro=0,bo=0;
        for(int c=0;c<chunks;c++){
            chunkKeys[c]=keys.get(c);kinds[c]=kindList.get(c);int[] lows=lowLists.get(c);counts[c]=lows.length;
            if(kinds[c]==ARRAY){
                offsets[c]=ao;for(int low:lows)arrayData[ao++]=(char)low;
            }else if(kinds[c]==RUNS){
                offsets[c]=ro;int[] rr=toRuns(lows);counts[c]|=(rr.length>>>1)<<17;System.arraycopy(rr,0,runData,ro,rr.length);ro+=rr.length;
            }else{
                offsets[c]=bo;for(int low:lows)bitmapData[bo+(low>>>6)]|=1L<<(low&63);bo+=1024;
            }
        }
        return new M3MatHybridIntSet(n,chunkKeys,kinds,offsets,counts,arrayData,runData,bitmapData);
    }

    public int size(){return size;}
    public int chunks(){return chunkKeys.length;}
    public long primitivePayloadBytes(){
        return 4L*(chunkKeys.length+offsets.length+counts.length)+kinds.length
                +2L*arrayData.length+4L*runData.length+8L*bitmapData.length;
    }
    public ChunkKind chunkKind(int chunkIndex){
        Objects.checkIndex(chunkIndex,chunkKeys.length);
        return ChunkKind.values()[kinds[chunkIndex]];
    }
    public int chunkKey(int chunkIndex){return chunkKeys[Objects.checkIndex(chunkIndex,chunkKeys.length)];}

    public boolean contains(int value){
        if(value<0)return false;
        int key=value>>>16,c=Arrays.binarySearch(chunkKeys,key);
        if(c<0)return false;
        int low=value&0xffff;
        return switch(kinds[c]){
            case ARRAY->Arrays.binarySearch(arrayData,offsets[c],offsets[c]+counts[c],(char)low)>=0;
            case BITMAP->(bitmapData[offsets[c]+(low>>>6)]&(1L<<(low&63)))!=0;
            case RUNS->containsRuns(c,low);
            default->false;
        };
    }

    public int[] toArray(){
        int[] out=new int[size];int k=0;
        for(int c=0;c<chunkKeys.length;c++){
            int high=chunkKeys[c]<<16;
            if(kinds[c]==ARRAY){
                for(int i=0;i<counts[c];i++)out[k++]=high|arrayData[offsets[c]+i];
            }else if(kinds[c]==BITMAP){
                int base=offsets[c];
                for(int w=0;w<1024;w++){
                    long x=bitmapData[base+w];
                    while(x!=0){int b=Long.numberOfTrailingZeros(x);out[k++]=high|(w<<6)|b;x&=x-1;}
                }
            }else{
                int base=offsets[c],remaining=counts[c]&COUNT_MASK;
                for(int r=0;remaining>0;r+=2){
                    int s=runData[base+r],len=runData[base+r+1];
                    for(int v=s;v<s+len;v++){out[k++]=high|v;remaining--;}
                }
            }
        }
        return out;
    }

    public M3MatHybridIntSet union(M3MatHybridIntSet other){return combine(other,0);}
    public M3MatHybridIntSet intersect(M3MatHybridIntSet other){return combine(other,1);}
    public M3MatHybridIntSet xor(M3MatHybridIntSet other){return combine(other,2);}
    public M3MatHybridIntSet minus(M3MatHybridIntSet other){return combine(other,3);}

    private M3MatHybridIntSet combine(M3MatHybridIntSet other,int op){
        Objects.requireNonNull(other,"other");
        int[] a=toArray(),b=other.toArray();
        // The AND result cannot exceed the shorter input; A minus B cannot exceed A.
        int upperBound=op==1?Math.min(a.length,b.length):op==3?a.length:a.length+b.length;
        int[] out=new int[upperBound];
        int i=0,j=0,k=0;
        while(i<a.length||j<b.length){
            int av=i<a.length?a[i]:Integer.MAX_VALUE,bv=j<b.length?b[j]:Integer.MAX_VALUE;
            if(i<a.length&&j<b.length&&av==bv){if(op==0||op==1)out[k++]=av;i++;j++;}
            else if(j>=b.length||(i<a.length&&av<bv)){if(op==0||op==2||op==3)out[k++]=av;i++;}
            else{if(op==0||op==2)out[k++]=bv;j++;}
        }
        // Both operands are canonical ascending unique; merge retains strict order.
        // Private builder owns the prefix and copies only encoded primitive chunks.
        return fromSortedUnique(out,k);
    }

    private boolean containsRuns(int c,int low){
        // The word stores the already-computed RUNS pair count in bits 17..27.
        int base=offsets[c],firstStart=runData[base],firstLength=runData[base+1];
        // Preserve the original one-run/front-hit fast path before logarithmic search.
        if(low<firstStart)return false;
        if(low<firstStart+firstLength)return true;
        int left=1,right=(counts[c]>>>17)-1;
        while(left<=right){
            int mid=(left+right)>>>1;
            int start=runData[base+(mid<<1)],length=runData[base+(mid<<1)+1];
            if(low<start)right=mid-1;
            else if(low>=start+length)left=mid+1;
            else return true;
        }
        return false;
    }
    private static int countRuns(int[] v){if(v.length==0)return 0;int r=1;for(int i=1;i<v.length;i++)if(v[i]!=v[i-1]+1)r++;return r;}
    private static int[] toRuns(int[] v){
        int[] out=new int[countRuns(v)*2];
        if(v.length==0)return out;
        int k=0,s=v[0],p=s;
        for(int i=1;i<=v.length;i++){
            if(i==v.length||v[i]!=p+1){out[k++]=s;out[k++]=p-s+1;if(i<v.length){s=v[i];p=s;}}
            else p=v[i];
        }
        return out;
    }
}
