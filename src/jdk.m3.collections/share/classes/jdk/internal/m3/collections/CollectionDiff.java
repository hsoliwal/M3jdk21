/* *****************************************************************************
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The difference of two collections as bags: for every distinct element, how often it occurs on each side.
 *
 * <p>The distinct elements are kept once, in the order they are first seen (the left collection, then the elements
 * only the right one has), in an element lane with two count lanes beside it. No entry object is stored; lists of the
 * difference are built only when asked for. Elements are compared with {@code equals} and {@code hashCode}; null is
 * an ordinary element. One pass over each collection, expected linear time.</p>
 *
 * <p>Provider comparison: Apache Commons Collections {@code CollectionUtils.subtract}, {@code intersection} and
 * {@code disjunction} (Apache-2.0) give the same bags as {@link #onlyLeft()} and {@link #onlyRight()},
 * {@link #common()}, and the two only-lists together. Not a {@code java.util} replacement.</p>
 *
 * @param <E> the element type
 */
public final class CollectionDiff<E> {
    private static final int MIN_SLOTS = 4;

    /** Storage lane {@code elementLane}: each distinct element once, in first-seen order. */
    private Object[] elementLane;
    /** Storage lane {@code hashLane}: the hash of each distinct element. */
    private int[] hashLane;
    /** Storage lane {@code leftCountLane}: occurrences in the left collection. */
    private int[] leftCountLane;
    /** Storage lane {@code rightCountLane}: occurrences in the right collection. */
    private int[] rightCountLane;
    /** Storage lane {@code indexLane}: open-addressed table of slot + 1, at most half full. */
    private int[] indexLane;
    private int distinct;

    private CollectionDiff(int slots) {
        elementLane = new Object[slots];
        hashLane = new int[slots];
        leftCountLane = new int[slots];
        rightCountLane = new int[slots];
        indexLane = new int[SlotIndex.length(slots)];
    }

    /**
     * Compares two collections as bags.
     *
     * @param left the collection before, or on the left
     * @param right the collection after, or on the right
     * @param <E> the element type
     * @return the difference; it does not change when the collections change later
     */
    public static <E> CollectionDiff<E> between(Collection<? extends E> left, Collection<? extends E> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        CollectionDiff<E> diff = new CollectionDiff<>(Math.max(MIN_SLOTS, left.size()));
        for (E element : left) {
            int slot = diff.slot(element);  // before the lane is read: slot() may replace the lanes
            diff.leftCountLane[slot]++;
        }
        for (E element : right) {
            int slot = diff.slot(element);
            diff.rightCountLane[slot]++;
        }
        return diff;
    }

    /** Returns the number of distinct elements of the two collections together. */
    public int distinct() {
        return distinct;
    }

    /** Returns the distinct element at {@code index}, in first-seen order. */
    @SuppressWarnings("unchecked")
    public E element(int index) {
        return (E) elementLane[Objects.checkIndex(index, distinct)];
    }

    /** Returns how often the element at {@code index} occurs in the left collection. */
    public int leftCount(int index) {
        return leftCountLane[Objects.checkIndex(index, distinct)];
    }

    /** Returns how often the element at {@code index} occurs in the right collection. */
    public int rightCount(int index) {
        return rightCountLane[Objects.checkIndex(index, distinct)];
    }

    /** Returns the right count minus the left count: positive when added, negative when removed, 0 when unchanged. */
    public int delta(int index) {
        return rightCount(index) - leftCount(index);
    }

    /** Returns whether the two collections are equal as bags. */
    public boolean isEmpty() {
        return Arrays.equals(leftCountLane, 0, distinct, rightCountLane, 0, distinct);
    }

    /** Returns the number of occurrences only the left collection has (removed, going from left to right). */
    public long removed() {
        long total = 0;
        for (int slot = 0; slot < distinct; slot++) {
            total += Math.max(0, leftCountLane[slot] - rightCountLane[slot]);
        }
        return total;
    }

    /** Returns the number of occurrences only the right collection has (added, going from left to right). */
    public long added() {
        long total = 0;
        for (int slot = 0; slot < distinct; slot++) {
            total += Math.max(0, rightCountLane[slot] - leftCountLane[slot]);
        }
        return total;
    }

    /** Returns the left collection minus the right one as a bag: each element as often as the left has it more. */
    public List<E> onlyLeft() {
        return expanded(leftCountLane, rightCountLane, false);
    }

    /** Returns the right collection minus the left one as a bag: each element as often as the right has it more. */
    public List<E> onlyRight() {
        return expanded(rightCountLane, leftCountLane, false);
    }

    /** Returns the bag both collections share: each element as often as the side that has it less. */
    public List<E> common() {
        return expanded(leftCountLane, rightCountLane, true);
    }

    /** Each element {@code min(a, b)} times when {@code shared}, else {@code max(0, a - b)} times. */
    private List<E> expanded(int[] a, int[] b, boolean shared) {
        List<E> list = new ArrayList<>();
        for (int slot = 0; slot < distinct; slot++) {
            int times = shared ? Math.min(a[slot], b[slot]) : a[slot] - b[slot];
            if (times > 0) {
                list.addAll(Collections.nCopies(times, element(slot)));
            }
        }
        return list;
    }

    /** The slot of {@code element}; a new distinct element takes the next slot. */
    private int slot(Object element) {
        int hash = SlotIndex.hash(element);
        int found = SlotIndex.find(indexLane, hashLane, elementLane, element, hash);
        if (found >= 0) {
            return found;
        }
        if (distinct == elementLane.length) {
            grow();
        }
        elementLane[distinct] = element;
        hashLane[distinct] = hash;
        SlotIndex.place(indexLane, hashLane, distinct);
        return distinct++;
    }

    /** Doubles the lanes; every lane is staged before any is published. */
    private void grow() {
        int slots = Math.multiplyExact(elementLane.length, 2);
        Object[] elements = Arrays.copyOf(elementLane, slots);
        int[] hashes = Arrays.copyOf(hashLane, slots);
        int[] left = Arrays.copyOf(leftCountLane, slots);
        int[] right = Arrays.copyOf(rightCountLane, slots);
        int[] index = SlotIndex.rebuilt(hashes, distinct, slots);
        elementLane = elements;
        hashLane = hashes;
        leftCountLane = left;
        rightCountLane = right;
        indexLane = index;
    }
}
