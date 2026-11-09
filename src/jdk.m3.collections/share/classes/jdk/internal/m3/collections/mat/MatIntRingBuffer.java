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
import java.util.NoSuchElementException;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntRingBuffer.java}
 * blob {@code 8e2f4be2cd4ebec0aa87b85bc9a59c062e1aa6a4} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIntRingBuffer{
  private final int[] values;private int head,size;
  public MatIntRingBuffer(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");values=new int[capacity];}
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
