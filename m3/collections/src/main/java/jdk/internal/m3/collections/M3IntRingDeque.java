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

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Thread-confined exact-width deque: one int[] and two counters; no element or index wrappers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/IntRingDeque.java}
 * blob {@code 42548e32a9f0879420bb8554500649305f3fe301} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntRingDeque {
    private int[] elements;
    private int head;
    private int size;
    public M3IntRingDeque() { this(8); }
    public M3IntRingDeque(int expectedSize) {
        if (expectedSize < 0 || expectedSize > (1 << 30)) throw new IllegalArgumentException("expectedSize");
        int capacity = 2; while (capacity < expectedSize) capacity <<= 1;
        elements = new int[capacity];
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return elements.length; }
    public long payloadBytes() { return 4L * elements.length; }
    public void addFirst(int value) {
        reserve(1); head = (head - 1) & (elements.length - 1); elements[head] = value; size++;
    }
    public void addLast(int value) {
        reserve(1); elements[(head + size) & (elements.length - 1)] = value; size++;
    }
    public int first() { if (size == 0) throw new NoSuchElementException(); return elements[head]; }
    public int last() { if (size == 0) throw new NoSuchElementException(); return elements[(head+size-1)&(elements.length-1)]; }
    public int removeFirst() {
        int value = first(); head = (head + 1) & (elements.length - 1); if (--size == 0) head = 0; return value;
    }
    public int removeLast() { int value = last(); if (--size == 0) head = 0; return value; }
    public int get(int index) { Objects.checkIndex(index,size); return elements[(head+index)&(elements.length-1)]; }
    public void clear() { size = 0; head = 0; }
    /** Validate and reserve before copying; the caller's array is never retained. */
    public void addAll(int[] source, int offset, int length) {
        Objects.requireNonNull(source,"source"); Objects.checkFromIndexSize(offset,length,source.length);
        reserve(length);
        int tail = (head + size) & (elements.length - 1), first = Math.min(length,elements.length-tail);
        System.arraycopy(source,offset,elements,tail,first);
        System.arraycopy(source,offset+first,elements,0,length-first); size += length;
    }
    /** Removes up to length items in FIFO order after validating the complete destination range. */
    public int drainTo(int[] target, int offset, int length) {
        Objects.requireNonNull(target,"target"); Objects.checkFromIndexSize(offset,length,target.length);
        int count = Math.min(length,size), first = Math.min(count,elements.length-head);
        System.arraycopy(elements,head,target,offset,first);
        System.arraycopy(elements,0,target,offset+first,count-first);
        head = (head + count) & (elements.length - 1); size -= count; if (size == 0) head = 0; return count;
    }
    public int[] toArray() {
        int[] result = new int[size]; int first = Math.min(size,elements.length-head);
        System.arraycopy(elements,head,result,0,first); System.arraycopy(elements,0,result,first,size-first); return result;
    }
    private void reserve(int extra) {
        if (extra > (1 << 30) - size) throw new IllegalStateException("maximum deque capacity reached");
        int required = size + extra;
        if (required <= elements.length) return;
        int capacity = elements.length;
        while (capacity < required) capacity <<= 1;
        int[] grown = new int[capacity]; int first = Math.min(size,elements.length-head);
        System.arraycopy(elements,head,grown,0,first); System.arraycopy(elements,0,grown,first,size-first);
        elements = grown; head = 0;
    }
}
