/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Fixed-size, live array-backed sequence. There is exactly one value lane:
 * the caller's original array. List facades are projected only at the API
 * boundary; neither entries nor copied per-element state are retained.
 * The owner does not promise thread safety beyond the caller's array.
 */
public final class FixedSizeListView<E> {
    /** Fixed-size state is per-view metadata, not one object per element. */
    static final long STATE_FIXED_SIZE = 1L << 0;

    private static final long[] FIXED_MASK = {STATE_FIXED_SIZE};
    private Object[] valueLane = new Object[0];
    private final long[] stateMaskWords = FIXED_MASK;
    private int size;

    /** An independent, empty fixed-size view (also preserves the old constructor). */
    public FixedSizeListView() { }

    /** Stable introspection signature retained for clients of the original owner. */
    public static String scaffoldDescriptor() {
        return "M3FixedSizeListView|FIXED_SIZE_LIST|OBJECT|FIXED_SIZE|Object[] valueLane";
    }

    /** Does not copy the input: updates are visible through both references. */
    public static <E> FixedSizeListView<E> of(E[] backing) {
        Objects.requireNonNull(backing, "backing");
        FixedSizeListView<E> view = new FixedSizeListView<>();
        view.valueLane = backing;
        view.size = backing.length;
        return view;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) valueLane[index];
    }

    /** Preserves the runtime component type's ArrayStoreException behavior. */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        E prior = (E) valueLane[index];
        valueLane[index] = element;
        return prior;
    }

    /** Exact List equality direction: candidate.equals(stored), not reverse. */
    public int indexOf(Object candidate) {
        if (candidate == null) {
            for (int i = 0; i < size; i++) {
                if (valueLane[i] == null) return i;
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (candidate.equals(valueLane[i])) return i;
            }
        }
        return -1;
    }

    public boolean contains(Object candidate) {
        return indexOf(candidate) >= 0;
    }

    /** Detached Object[] snapshot, never the retained backing array. */
    public Object[] toArray() {
        return Arrays.copyOf(valueLane, size, Object[].class);
    }

    /** The standard JDK fixed-size facade, backed by the same single array. */
    @SuppressWarnings("unchecked")
    public List<E> asList() {
        return Arrays.asList((E[]) valueLane);
    }

    public int size() {
        return size;
    }

    /** A fixed-size list cannot append, even when it is empty. */
    public boolean add(E element) {
        throw new UnsupportedOperationException();
    }
}
