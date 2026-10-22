/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;
import java.util.Arrays;
/** Sorted primitive long set in one contiguous array, without tree nodes. */
public final class LongSortedSet {
    private long[] values; private int size;
    public LongSortedSet(){this(16);} public LongSortedSet(int c){if(c<0)throw new IllegalArgumentException();values=new long[c];}
    public int size(){return size;}
    public int indexOf(long v){int lo=0,hi=size-1;while(lo<=hi){int m=(lo+hi)>>>1;long mv=values[m];if(mv<v)lo=m+1;else if(mv>v)hi=m-1;else return m;}return~lo;}
    public boolean contains(long v){return indexOf(v)>=0;}
    public boolean add(long v){int i=indexOf(v);if(i>=0)return false;int p=~i;ensure(size+1);int moved=size-p;if(moved>0)System.arraycopy(values,p,values,p+1,moved);values[p]=v;size++;return true;}
    public boolean remove(long v){int i=indexOf(v);if(i<0)return false;int moved=size-i-1;if(moved>0)System.arraycopy(values,i+1,values,i,moved);size--;return true;}
    public long valueAt(int i){if(i<0||i>=size)throw new IndexOutOfBoundsException();return values[i];}
    public void clear(){size=0;} public long estimatedPayloadBytes(){return(long)values.length*Long.BYTES;}
    private void ensure(int min){if(min<=values.length)return;values=Arrays.copyOf(values,PrimitiveHashing.nextPowerOfTwo(min));}
}
