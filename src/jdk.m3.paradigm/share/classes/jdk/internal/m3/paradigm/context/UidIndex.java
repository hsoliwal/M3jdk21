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

/**
 * Two-long UID to non-negative context-index map with no UUID/key-entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidIndex.java}
 * blob {@code d888dd14b2492a88ab4f7f900b9b19f5ab8f9ac2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidIndex {
    private static final int EMPTY = -1;
    private static final int MIN_CAPACITY = 8;
    private long[] high;
    private long[] low;
    private int[] values;
    private int size;

    public UidIndex() {
        this(MIN_CAPACITY);
    }

    public UidIndex(int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("negative expected size");
        int capacity = MIN_CAPACITY;
        while (capacity < Math.max(MIN_CAPACITY, expectedSize << 1)) capacity <<= 1;
        high = new long[capacity];
        low = new long[capacity];
        values = new int[capacity];
        Arrays.fill(values, EMPTY);
    }

    public int size() { return size; }

    public int get(long highWord, long lowWord) {
        int slot = locate(highWord, lowWord);
        return slot < 0 ? -1 : values[slot];
    }

    public boolean contains(long highWord, long lowWord) {
        return locate(highWord, lowWord) >= 0;
    }

    public void put(long highWord, long lowWord, int contextIndex) {
        IndexList.requireIndexValue(contextIndex);
        if ((size + 1) * 10 >= values.length * 6) rehash(values.length << 1);
        int mask = values.length - 1;
        int slot = hash(highWord, lowWord) & mask;
        while (values[slot] != EMPTY) {
            if (high[slot] == highWord && low[slot] == lowWord) {
                if (values[slot] != contextIndex) {
                    throw new IllegalArgumentException("duplicate UID mapped to another context");
                }
                return;
            }
            slot = (slot + 1) & mask;
        }
        high[slot] = highWord;
        low[slot] = lowWord;
        values[slot] = contextIndex;
        size++;
    }

    private int locate(long highWord, long lowWord) {
        int mask = values.length - 1;
        int slot = hash(highWord, lowWord) & mask;
        while (true) {
            int value = values[slot];
            if (value == EMPTY) return -1;
            if (high[slot] == highWord && low[slot] == lowWord) return slot;
            slot = (slot + 1) & mask;
        }
    }

    private void rehash(int capacity) {
        long[] oldHigh = high;
        long[] oldLow = low;
        int[] oldValues = values;
        high = new long[capacity];
        low = new long[capacity];
        values = new int[capacity];
        Arrays.fill(values, EMPTY);
        int oldSize = size;
        size = 0;
        for (int i = 0; i < oldValues.length; i++) {
            if (oldValues[i] != EMPTY) put(oldHigh[i], oldLow[i], oldValues[i]);
        }
        if (size != oldSize) throw new AssertionError("rehash size mismatch");
    }

    private static int hash(long highWord, long lowWord) {
        long value = highWord ^ Long.rotateLeft(lowWord, 29);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return (int) (value ^ (value >>> 32));
    }
}
