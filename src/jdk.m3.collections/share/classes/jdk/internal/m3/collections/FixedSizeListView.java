/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * M3 five-library superset {@code FixedSizeListView}: API group
 * {@code FIXED_SIZE_LIST} over caller-owned arrays.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK Arrays.asList: fixed-size writable array view [license GPLv2+CPE;
 *   SUPERSET_VIEW VIEW_OF:LongCompositeViews; priority P2; matrix owner
 *   LongCompositeViews]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind OBJECT; {@code Object[] valueLane} retains the caller's
 * original array with direct identity offsets. Fixed-size state is implicit at
 * the owner boundary. Stored structural nodes: none; API entry objects: none.
 *
 * <p>Status: source-sealed receiving candidate. The caller's original array remains
 * the authoritative payload store; JDK array-list semantics are the independent
 * behavioral oracle. This owner does not replace {@code java.util.List} or
 * the JDK's own {@code Arrays.asList}.
 */
public final class FixedSizeListView<E> {
    /** State-mask bit {@code FIXED_SIZE} (per-slot state word, bit 0). */
    static final long STATE_FIXED_SIZE = 1L << 0;

    /** Authoritative array: the exact caller-provided reference lane for {@code of}. */
    private Object[] valueLane = new Object[0];
    /** Legacy empty metadata lane; fixed length is an implicit owner-wide state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty fixed-size view. */
    public FixedSizeListView() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FixedSizeListView|FIXED_SIZE_LIST|OBJECT|FIXED_SIZE|Object[] valueLane";
    }

    /** Retain the original caller array; null is not a valid backing owner. */
    public static <E> FixedSizeListView<E> of(E[] backing) {
        Objects.requireNonNull(backing);
        FixedSizeListView<E> view = new FixedSizeListView<>();
        view.valueLane = backing;
        view.size = backing.length;
        return view;
    }

    /** Indexed read using the JDK array view's bounds and exception behavior. */
    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) valueLane[index];
    }

    /** Write through to the original array, retaining array covariance checks. */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        E previous = (E) valueLane[index];
        valueLane[index] = element;
        return previous;
    }

    /** First matching slot; invoke search-element equals just as a JDK List does. */
    public int indexOf(Object element) {
        for (int i = 0; i < size; i++) {
            if (element == null ? valueLane[i] == null : element.equals(valueLane[i])) {
                return i;
            }
        }
        return -1;
    }

    /** Membership uses the same first-match equality contract as indexOf. */
    public boolean contains(Object element) {
        return indexOf(element) >= 0;
    }

    /** Detached Object[] copy, independent even for covariant backing arrays. */
    public Object[] toArray() {
        return Arrays.copyOf(valueLane, size, Object[].class);
    }

    /** JDK-owned fixed-size, writable-element facade over the same array. */
    @SuppressWarnings("unchecked")
    public java.util.List<E> asList() {
        return Arrays.asList((E[]) valueLane);
    }

    /** Length is fixed by the caller-owned backing array. */
    public int size() {
        return size;
    }

    /** Fixed-size append always refuses, including for an empty backing. */
    public boolean add(E element) {
        throw new UnsupportedOperationException();
    }
}
