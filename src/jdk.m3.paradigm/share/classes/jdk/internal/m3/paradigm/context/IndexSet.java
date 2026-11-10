/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Open-addressed set for non-negative context indices; -1/-2 are internal sentinels.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextIndexSet.java}
 * blob {@code 04242a5c9448c89bb8f1d03a049a345830c6e08c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IndexSet {
    private static final int EMPTY = -1;
    private static final int DELETED = -2;
    private static final int MIN_CAPACITY = 8;
    private int[] table;
    private int size;
    private int used;

    public IndexSet() {
        table = new int[MIN_CAPACITY];
        Arrays.fill(table, EMPTY);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return table.length; }

    public boolean contains(int value) {
        IndexList.requireIndexValue(value);
        return find(value) >= 0;
    }

    public boolean add(int value) {
        IndexList.requireIndexValue(value);
        if ((used + 1) * 10 >= table.length * 6) rehash(table.length << 1);
        int mask = table.length - 1;
        int slot = mix(value) & mask;
        int deleted = -1;
        while (true) {
            int current = table[slot];
            if (current == value) return false;
            if (current == EMPTY) {
                int target = deleted >= 0 ? deleted : slot;
                if (deleted < 0) used++;
                table[target] = value;
                size++;
                return true;
            }
            if (current == DELETED && deleted < 0) deleted = slot;
            slot = (slot + 1) & mask;
        }
    }

    public boolean remove(int value) {
        IndexList.requireIndexValue(value);
        int slot = find(value);
        if (slot < 0) return false;
        table[slot] = DELETED;
        size--;
        if (size == 0) clear();
        else if (table.length > MIN_CAPACITY && size * 8 < table.length) rehash(table.length >>> 1);
        return true;
    }

    public void clear() {
        Arrays.fill(table, EMPTY);
        size = 0;
        used = 0;
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int value : table) if (value >= 0) action.accept(value);
    }

    public int[] toArray() {
        int[] result = new int[size];
        int at = 0;
        for (int value : table) if (value >= 0) result[at++] = value;
        Arrays.sort(result);
        return result;
    }

    private int find(int value) {
        int mask = table.length - 1;
        int slot = mix(value) & mask;
        while (true) {
            int current = table[slot];
            if (current == EMPTY) return -1;
            if (current == value) return slot;
            slot = (slot + 1) & mask;
        }
    }

    private void rehash(int requested) {
        int capacity = MIN_CAPACITY;
        while (capacity < requested) capacity <<= 1;
        int[] previous = table;
        table = new int[capacity];
        Arrays.fill(table, EMPTY);
        int previousSize = size;
        size = 0;
        used = 0;
        for (int value : previous) if (value >= 0) add(value);
        if (size != previousSize) throw new AssertionError("rehash size mismatch");
    }

    private static int mix(int value) {
        int mixed = value ^ (value >>> 16);
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }
}
