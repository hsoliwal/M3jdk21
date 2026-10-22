/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;
import java.util.Arrays;
import java.util.Comparator;
/** Parallel-array sorted object map. O(log n) lookup, O(n) mutation, zero tree nodes. */
public final class FlatSortedMap<K,V> {
    private Object[] keys,values; private int size; private final Comparator<? super K> comparator;
    public FlatSortedMap(Comparator<? super K> c){this(16,c);} public FlatSortedMap(int cap,Comparator<? super K> c){if(cap<0)throw new IllegalArgumentException();keys=new Object[cap];values=new Object[cap];comparator=c;}
    public int size(){return size;} public boolean isEmpty(){return size==0;}
    public int indexOfKey(K k){int lo=0,hi=size-1;while(lo<=hi){int m=(lo+hi)>>>1;int c=compare(keyAt(m),k);if(c<0)lo=m+1;else if(c>0)hi=m-1;else return m;}return~lo;}
    public boolean put(K k,V v){int i=indexOfKey(k);if(i>=0){values[i]=v;return false;}int p=~i;ensure(size+1);int moved=size-p;if(moved>0){System.arraycopy(keys,p,keys,p+1,moved);System.arraycopy(values,p,values,p+1,moved);}keys[p]=k;values[p]=v;size++;return true;}
    @SuppressWarnings("unchecked") public V getOrDefault(K k,V d){int i=indexOfKey(k);return i>=0?(V)values[i]:d;}
    public boolean remove(K k){int i=indexOfKey(k);if(i<0)return false;int moved=size-i-1;if(moved>0){System.arraycopy(keys,i+1,keys,i,moved);System.arraycopy(values,i+1,values,i,moved);}keys[--size]=null;values[size]=null;return true;}
    @SuppressWarnings("unchecked") public K keyAt(int i){check(i);return(K)keys[i];} @SuppressWarnings("unchecked") public V valueAt(int i){check(i);return(V)values[i];}
    public void clear(){Arrays.fill(keys,0,size,null);Arrays.fill(values,0,size,null);size=0;} public long referenceSlots(){return(long)keys.length+values.length;}
    @SuppressWarnings({"unchecked","rawtypes"}) private int compare(K a,K b){return comparator!=null?comparator.compare(a,b):((Comparable)a).compareTo(b);}
    private void ensure(int min){if(min<=keys.length)return;int c=PrimitiveHashing.nextPowerOfTwo(min);keys=Arrays.copyOf(keys,c);values=Arrays.copyOf(values,c);} private void check(int i){if(i<0||i>=size)throw new IndexOutOfBoundsException();}
}
