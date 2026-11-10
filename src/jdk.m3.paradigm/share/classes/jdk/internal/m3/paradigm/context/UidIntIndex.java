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
 * Open-addressed 128-bit context UID to nonnegative int index, with no entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidIntIndex.java}
 * blob {@code 923bda4c4293278ed1cf5f5d848e12a9d0d04c00} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidIntIndex {
    private static final int EMPTY = -1;

    private final long[] words;
    private final int[] values;
    private final int size;

    private UidIntIndex(long[] words, int[] values, int size) {
        this.words = words;
        this.values = values;
        this.size = size;
    }

    public static Builder builder(int expectedSize) {
        return new Builder(expectedSize);
    }

    public int size() { return size; }

    public int getOrDefault(long high, long low, int fallback) {
        int mask = values.length - 1;
        int slot = mix(high, low) & mask;
        while (true) {
            int value = values[slot];
            if (value == EMPTY) return fallback;
            int word = slot << 1;
            if (words[word] == high && words[word + 1] == low) return value;
            slot = (slot + 1) & mask;
        }
    }

    public boolean contains(long high, long low) {
        return getOrDefault(high, low, EMPTY) != EMPTY;
    }

    public long logicalTableBytes() {
        return words.length * (long) Long.BYTES
                + values.length * (long) Integer.BYTES;
    }

    public static final class Builder {
        private long[] words;
        private int[] values;
        private int size;

        private Builder(int expectedSize) {
            if (expectedSize < 0) {
                throw new IllegalArgumentException("expectedSize must be nonnegative");
            }
            int capacity = 8;
            while (capacity * 3L / 5L < expectedSize) capacity <<= 1;
            words = new long[capacity << 1];
            values = new int[capacity];
            Arrays.fill(values, EMPTY);
        }

        public Builder put(long high, long low, int value) {
            if (value < 0) {
                throw new IllegalArgumentException("indexed value must be nonnegative");
            }
            if ((size + 1) * 5L > values.length * 3L) resize(values.length << 1);
            insert(high, low, value);
            return this;
        }

        public UidIntIndex build() {
            return new UidIntIndex(words.clone(), values.clone(), size);
        }

        private void insert(long high, long low, int value) {
            int mask = values.length - 1;
            int slot = mix(high, low) & mask;
            while (true) {
                if (values[slot] == EMPTY) {
                    int word = slot << 1;
                    words[word] = high;
                    words[word + 1] = low;
                    values[slot] = value;
                    size++;
                    return;
                }
                int word = slot << 1;
                if (words[word] == high && words[word + 1] == low) {
                    values[slot] = value;
                    return;
                }
                slot = (slot + 1) & mask;
            }
        }

        private void resize(int capacity) {
            long[] oldWords = words;
            int[] oldValues = values;
            words = new long[capacity << 1];
            values = new int[capacity];
            Arrays.fill(values, EMPTY);
            int oldSize = size;
            size = 0;
            for (int slot = 0; slot < oldValues.length; slot++) {
                if (oldValues[slot] != EMPTY) {
                    int word = slot << 1;
                    insert(oldWords[word], oldWords[word + 1], oldValues[slot]);
                }
            }
            if (size != oldSize) {
                throw new AssertionError("UID index resize lost entries");
            }
        }
    }

    private static int mix(long high, long low) {
        long z = high ^ Long.rotateLeft(low, 29) ^ 0x9e3779b97f4a7c15L;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z ^= z >>> 31;
        return (int) (z ^ (z >>> 32));
    }
}
