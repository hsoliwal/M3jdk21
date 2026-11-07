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
package jdk.m3.collections;
import java.util.Arrays;
import java.util.Objects;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatAdaptiveIntVector.java}
 * blob {@code 4864d93fc8400f23139f624891a82f65862a1ff6} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatAdaptiveIntVector{
    public enum Encoding{CONSTANT,PACKED,PLAIN}
    private final int size;private final Encoding encoding;private final int base;private final int width;
    private final int[] plain;private final long[] packed;private final M3MatCollectionPlan plan;
    private M3MatAdaptiveIntVector(int size,Encoding encoding,int base,int width,int[] plain,long[] packed,M3MatCollectionPlan plan){
        this.size=size;this.encoding=encoding;this.base=base;this.width=width;this.plain=plain;this.packed=packed;this.plan=plan;
    }
    public static M3MatAdaptiveIntVector of(int[] source){
        Objects.requireNonNull(source,"source");int size=source.length;
        if(size==0)return new M3MatAdaptiveIntVector(0,Encoding.CONSTANT,0,0,null,new long[0],new M3MatCollectionPlan(M3MatStorageKind.VECTOR_CONSTANT,0,0,0,"empty"));
        int min=source[0],max=source[0];for(int value:source){if(value<min)min=value;if(value>max)max=value;}
        long range=(long)max-min;if(range==0)return new M3MatAdaptiveIntVector(size,Encoding.CONSTANT,min,0,null,new long[0],new M3MatCollectionPlan(M3MatStorageKind.VECTOR_CONSTANT,0,size,1,"constant base="+min));
        int width=64-Long.numberOfLeadingZeros(range);long words=(((long)width*size)+63L)>>>6,packedBytes=words*8L,plainBytes=size*4L;
        if(width<32&&packedBytes<plainBytes){
            long[] packed=new long[Math.toIntExact(words)];
            for(int i=0;i<size;i++)write(packed,(long)i*width,width,(long)source[i]-min);
            return new M3MatAdaptiveIntVector(size,Encoding.PACKED,min,width,null,packed,new M3MatCollectionPlan(M3MatStorageKind.VECTOR_PACKED,packedBytes,size,range+1,"frame-of-reference width="+width+" base="+min));
        }
        int[] plain=source.clone();return new M3MatAdaptiveIntVector(size,Encoding.PLAIN,0,32,plain,null,new M3MatCollectionPlan(M3MatStorageKind.VECTOR_PLAIN,plainBytes,size,range+1,"int[]"));
    }
    public int size(){return size;}public Encoding encoding(){return encoding;}public int bitWidth(){return width;}public int base(){return base;}
    public M3MatCollectionPlan plan(){return plan;}public long primitivePayloadBytes(){return plan.primitivePayloadBytes();}
    public int get(int index){Objects.checkIndex(index,size);return switch(encoding){case CONSTANT->base;case PLAIN->plain[index];case PACKED->Math.toIntExact((long)base+read(packed,(long)index*width,width));};}
    public int[] toArray(){if(encoding==Encoding.PLAIN)return plain.clone();int[] r=new int[size];for(int i=0;i<size;i++)r[i]=get(i);return r;}
    public M3MatBits equalTo(int value){M3MatBits.Builder b=M3MatBits.builder(size);for(int i=0;i<size;i++)if(get(i)==value)b.set(i);return b.build();}
    public M3MatBits greaterOrEqual(int value){M3MatBits.Builder b=M3MatBits.builder(size);for(int i=0;i<size;i++)if(get(i)>=value)b.set(i);return b.build();}
    public M3MatBits lessThan(int value){M3MatBits.Builder b=M3MatBits.builder(size);for(int i=0;i<size;i++)if(get(i)<value)b.set(i);return b.build();}
    @Override public boolean equals(Object other){if(this==other)return true;if(!(other instanceof M3MatAdaptiveIntVector v)||size!=v.size)return false;for(int i=0;i<size;i++)if(get(i)!=v.get(i))return false;return true;}
    @Override public int hashCode(){return Arrays.hashCode(toArray());}
    private static long mask(int width){if(width<=0||width>63)throw new IllegalArgumentException("width");return(1L<<width)-1L;}
    private static void write(long[] words,long bitOffset,int width,long value){long mask=mask(width);if((value&~mask)!=0)throw new IllegalArgumentException("packed delta overflow");int word=(int)(bitOffset>>>6),shift=(int)(bitOffset&63);words[word]=(words[word]&~(mask<<shift))|(value<<shift);if(shift!=0&&width>64-shift){int high=width-(64-shift);long hm=(1L<<high)-1L;words[word+1]=(words[word+1]&~hm)|(value>>>(64-shift));}}
    private static long read(long[] words,long bitOffset,int width){long mask=mask(width);int word=(int)(bitOffset>>>6),shift=(int)(bitOffset&63);long value=words[word]>>>shift;if(shift!=0&&width>64-shift)value|=words[word+1]<<(64-shift);return value&mask;}
}
