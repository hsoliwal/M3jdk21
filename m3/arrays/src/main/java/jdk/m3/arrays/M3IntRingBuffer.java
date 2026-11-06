// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;
import java.util.NoSuchElementException;
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-mat-collections/src/main/java/com/synexia/matcollections/MatIntRingBuffer.java blob 8e2f4be2cd4e; recipe m3/arrays/recipe/int-ring-buffer-20261106/relocate.py v3
public final class M3IntRingBuffer{
  private final int[] values;private int head,size;
  public M3IntRingBuffer(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");values=new int[capacity];}
  public int capacity(){return values.length;}public int size(){return size;}public boolean isEmpty(){return size==0;}public boolean isFull(){return size==values.length;}
  // Index translation is one bounded wrap, including arbitrary non-power-of-two capacities.
  private int physical(int offset){int toEnd=values.length-head;return offset>=toEnd?offset-toEnd:head+offset;}
  private int next(int position){return position+1==values.length?0:position+1;}
  public boolean offerLast(int v){if(isFull())return false;values[physical(size)]=v;size++;return true;}
  public boolean offerFirst(int v){if(isFull())return false;head=head==0?values.length-1:head-1;values[head]=v;size++;return true;}
  public int peekFirst(){if(isEmpty())throw new NoSuchElementException();return values[head];}
  public int peekLast(){if(isEmpty())throw new NoSuchElementException();return values[physical(size-1)];}
  public int removeFirst(){int r=peekFirst();head=next(head);size--;return r;}
  public int removeLast(){int r=peekLast();size--;return r;}
  // Copy the two physical runs directly to the independent logical snapshot.
  public int[] snapshot(){int[]r=new int[size];int first=Math.min(size,values.length-head);System.arraycopy(values,head,r,0,first);if(first<size)System.arraycopy(values,0,r,first,size-first);return r;}
  public long primitivePayloadBytes(){return values.length*4L;}
}
