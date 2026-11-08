// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;
import java.util.Arrays;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/main/java/com/synexia/matcollections/MatIntList.java blob 821f776b5eae; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingList implements Iterable<Integer> {
    private final int[] values; private final int offset; private final int size;
    public IntPostingList(int[] values){ this(Objects.requireNonNull(values,"values").clone(),0,values.length,true); }
    private IntPostingList(int[] values,int offset,int size,boolean trusted){
        this.values=values;this.offset=offset;this.size=size;Objects.checkFromIndexSize(offset,size,values.length);
    }
    static IntPostingList trustedView(int[] values,int offset,int size){ return new IntPostingList(values,offset,size,true); }
    public int size(){return size;} public boolean isEmpty(){return size==0;}
    public int getInt(int index){return values[offset+Objects.checkIndex(index,size)];}
    public Integer get(int index){return getInt(index);}
    public long primitivePayloadBytes(){return values.length*4L;}
    public IntPostingList slice(int from,int to){Objects.checkFromToIndex(from,to,size);return trustedView(values,offset+from,to-from);}
    public int indexOf(int value){for(int i=0;i<size;i++)if(values[offset+i]==value)return i;return -1;}
    public boolean contains(int value){return indexOf(value)>=0;}
    public int[] toArray(){return Arrays.copyOfRange(values,offset,offset+size);}
    public IntStream intStream(){return Arrays.stream(values,offset,offset+size);}
    @Override public PrimitiveIterator.OfInt iterator(){return intStream().iterator();}
    @Override public boolean equals(Object other){
        if(this==other)return true;
        if(!(other instanceof IntPostingList list)||size!=list.size)return false;
        for(int i=0;i<size;i++)if(getInt(i)!=list.getInt(i))return false;
        return true;
    }
    @Override public int hashCode(){int h=1;for(int i=0;i<size;i++)h=31*h+getInt(i);return h;}
}
