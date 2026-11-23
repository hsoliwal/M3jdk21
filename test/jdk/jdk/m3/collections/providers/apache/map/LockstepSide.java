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
package jdk.m3.collections.apache.map;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;

/**
 * One side of the lockstep: a map, how its key list is viewed, and the live list iterators in
 * numbered slots (an absent slot has no entry, never a null iterator).
 */
final class LockstepSide {

    /** The map behind the view. */
    final LinkedMap<Object, Object> map;

    /** Makes a list view of the map. */
    private final Function<LinkedMap<Object, Object>, List<Object>> views;

    /** The live iterators by slot. */
    private final Map<Integer, ListIterator<Object>> slots = new HashMap<>();

    LockstepSide(final LinkedMap<Object, Object> map,
            final Function<LinkedMap<Object, Object>, List<Object>> views) {
        this.map = map;
        this.views = views;
    }

    /**
     * Makes a fresh list view of the map.
     *
     * @return The view
     */
    List<Object> view() {
        return views.apply(map);
    }

    /**
     * Tests whether a slot holds an iterator.
     *
     * @param slot The slot
     * @return Whether it holds one
     */
    boolean has(final int slot) {
        return slots.containsKey(slot);
    }

    /**
     * Gets the iterator in a slot.
     *
     * @param slot The slot
     * @return The iterator
     */
    ListIterator<Object> iterator(final int slot) {
        return slots.get(slot);
    }

    /**
     * Puts an iterator in a slot.
     *
     * @param slot The slot
     * @param iterator The iterator
     * @return A plain marker for the outcome
     */
    Object open(final int slot, final ListIterator<Object> iterator) {
        slots.put(slot, iterator);
        return "open " + slot;
    }

    /**
     * Gets the call counters of a counting map.
     *
     * @return The counters, empty for any other map
     */
    List<Integer> counts() {
        return map instanceof CountingLinkedMap counting ? counting.counts() : List.of();
    }
}
