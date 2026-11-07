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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Bounded Space-Saving heavy-hitter sketch over long keys.
 *
 * <p>Retained state is three parallel long[] lanes: key, estimated count and
 * replacement error. Update is O(k), intentionally trading hash metadata for a
 * tiny fixed hot set.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongSpaceSavingTopK.java}
 * blob {@code d60f937b6ef6702cda710c1f8feb93e1c8ef9aee} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongSpaceSavingTopK {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long key, long estimate, long error);
    }

    private final long[] keys;
    private final long[] counts;
    private final long[] errors;
    private int size;

    public M3LongSpaceSavingTopK(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity <= 0");
        keys = new long[capacity];
        counts = new long[capacity];
        errors = new long[capacity];
    }

    public int capacity() { return keys.length; }
    public int size() { return size; }
    public long payloadBytes() { return (long) keys.length * Long.BYTES * 3L; }

    public long add(long key) { return add(key, 1L); }

    public long add(long key, long increment) {
        if (increment <= 0L) throw new IllegalArgumentException("increment <= 0");
        if (size < keys.length) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    counts[i] = saturatingAdd(counts[i], increment);
                    return counts[i];
                }
            }
            keys[size] = key;
            counts[size] = increment;
            errors[size] = 0L;
            size++;
            return increment;
        }

        // Full sketch: find a repeated key and the earliest minimum in one pass.
        int minimum = 0;
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                counts[i] = saturatingAdd(counts[i], increment);
                return counts[i];
            }
            if (counts[i] < counts[minimum]) minimum = i;
        }
        long previousMinimum = counts[minimum];
        keys[minimum] = key;
        errors[minimum] = previousMinimum;
        counts[minimum] = saturatingAdd(previousMinimum, increment);
        return counts[minimum];
    }

    public long estimate(long key) {
        for (int i = 0; i < size; i++) if (keys[i] == key) return counts[i];
        return 0L;
    }

    public long error(long key) {
        for (int i = 0; i < size; i++) if (keys[i] == key) return errors[i];
        return 0L;
    }

    public void forEach(EntryConsumer consumer) {
        for (int i = 0; i < size; i++) consumer.accept(keys[i], counts[i], errors[i]);
    }

    public void clear() {
        Arrays.fill(keys, 0, size, 0L);
        Arrays.fill(counts, 0, size, 0L);
        Arrays.fill(errors, 0, size, 0L);
        size = 0;
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
