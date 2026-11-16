/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Immutable object counts with first-encounter distinct ordinals.
 *
 * <p>The existing object-to-int owner indexes elements into exact element and
 * count lanes. No entry or node object is retained for each distinct element.
 * Set facades and iterators are projected on demand. Copies own their structural
 * storage and retain the first encountered representative of each equality
 * group. Element objects remain shallow references and require stable equality
 * and hash codes while used as keys.
 *
 * <p>Null source elements are rejected; null and missing queries return count
 * zero. Counts fit an int, and total size saturates at Integer.MAX_VALUE, as in
 * Guava ImmutableMultiset. Every mutation request throws, including no-ops.
 * This is the existing internal M3 API, not a Guava or Eclipse interface.
 */
public final class FrozenBag<E> {
    static final long STATE_FROZEN = 1L << 0;

    private final Object[] elementLane;
    private final int[] countLane;
    private final long[] stateMaskWords;
    private final ObjectIntHashMap<Object> elementOrdinals;
    private final int size;

    /** Creates a valid immutable empty owner. */
    public FrozenBag() {
        this(new Object[0], new int[0], new ObjectIntHashMap<>(0), 0);
    }

    private FrozenBag(Object[] elements, int[] counts,
            ObjectIntHashMap<Object> ordinals, int total) {
        elementLane = elements;
        countLane = counts;
        elementOrdinals = ordinals;
        size = total;
        stateMaskWords = new long[(int) (((long) elements.length + 63) >>> 6)];
        Arrays.fill(stateMaskWords, -1L);
        if ((elements.length & 63) != 0) {
            stateMaskWords[stateMaskWords.length - 1] = (1L << (elements.length & 63)) - 1;
        }
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenBag|FROZEN_BAG|OBJECT|FROZEN|Object[] elementLane;int[] countLane;int[] hashIndexLane";
    }

    /**
     * Copies encounter-ordered counts without trusting the reported collection
     * size. Construction stays private until traversal, callbacks and every
     * allocation succeed. A single element count cannot exceed Integer.MAX_VALUE.
     */
    public static <E> FrozenBag<E> copyOf(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<E> elements = new ArrayList<>();
        ObjectIntHashMap<Object> ordinals = new ObjectIntHashMap<>(0);
        int[] counts = new int[8];
        int total = 0;
        for (E element : source) {
            Objects.requireNonNull(element, "element");
            int ordinal = ordinals.getOrDefault(element, -1);
            if (ordinal >= 0) {
                if (counts[ordinal] == Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("too many occurrences of one element");
                }
                counts[ordinal]++;
            } else {
                ordinal = elements.size();
                if (ordinal == counts.length) {
                    int capacity = Math.toIntExact(Math.min(Integer.MAX_VALUE - 8L,
                            Math.max((long) ordinal + 1,
                                    (long) counts.length + (counts.length >>> 1) + 1)));
                    if (capacity <= ordinal) {
                        throw new OutOfMemoryError("FrozenBag count capacity exhausted");
                    }
                    counts = Arrays.copyOf(counts, capacity);
                }
                ordinals.put(element, ordinal);
                elements.add(element);
                counts[ordinal] = 1;
            }
            if (total != Integer.MAX_VALUE) {
                total++;
            }
        }
        return new FrozenBag<>(elements.toArray(), Arrays.copyOf(counts, elements.size()),
                ordinals, total);
    }

    /** Total occurrences, saturated at Integer.MAX_VALUE. */
    public int size() {
        return size;
    }

    public int distinctSize() {
        return elementLane.length;
    }

    public int count(Object element) {
        int ordinal = elementOrdinals.getOrDefault(element, -1);
        return ordinal < 0 ? 0 : countLane[ordinal];
    }

    public boolean contains(Object element) {
        return count(element) != 0;
    }

    /** First-encounter distinct ordinal. */
    @SuppressWarnings("unchecked")
    public E elementAt(int ordinal) {
        return (E) elementLane[Objects.checkIndex(ordinal, elementLane.length)];
    }

    public int countAt(int ordinal) {
        return countLane[Objects.checkIndex(ordinal, countLane.length)];
    }

    /** Immutable encounter-ordered facade over the distinct element lane. */
    public Set<E> elementSet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() {
                return elementLane.length;
            }
            @Override public boolean contains(Object element) {
                return FrozenBag.this.contains(element);
            }
            @Override public Iterator<E> iterator() {
                return new Iterator<>() {
                    private int cursor;
                    @Override public boolean hasNext() {
                        return cursor < elementLane.length;
                    }
                    @Override public E next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return elementAt(cursor++);
                    }
                };
            }
            @Override public Spliterator<E> spliterator() {
                return Spliterators.spliterator(iterator(), elementLane.length,
                        Spliterator.ORDERED | Spliterator.DISTINCT
                                | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            }
        });
    }

    public int add(E element, int occurrences) {
        throw new UnsupportedOperationException("immutable bag");
    }

    public int remove(Object element, int occurrences) {
        throw new UnsupportedOperationException("immutable bag");
    }

    public int setCount(E element, int count) {
        throw new UnsupportedOperationException("immutable bag");
    }
}
