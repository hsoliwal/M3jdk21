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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;
import jdk.internal.m3.collections.LongReadOnlyMap;

/**
 * Immutable sorted primitive lanes; duplicate input keys reject, and no input lane is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedFrozenLongMap.java}
 * blob {@code 50729509d91638586f3c09a5dbce4def8a54cd8b} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedFrozenLongMap implements LongReadOnlyMap {
    private final long[] keys, values;
    private PackedFrozenLongMap(long[] keys, long[] values) { this.keys = keys; this.values = values; }
    public static PackedFrozenLongMap copyOf(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        if (keys.length != values.length) throw new IllegalArgumentException("lane lengths");
        long[] k = keys.clone(), v = values.clone();
        for (int i = (k.length >>> 1) - 1; i >= 0; i--) sift(k, v, i, k.length);
        for (int end = k.length - 1; end > 0; end--) { swap(k, v, 0, end); sift(k, v, 0, end); }
        for (int i = 1; i < k.length; i++) if (k[i - 1] == k[i]) throw new IllegalArgumentException("duplicate key: " + k[i]);
        return new PackedFrozenLongMap(k, v);
    }
    public static PackedFrozenLongMap copyOf(PackedLongLongMap source) {
        Objects.requireNonNull(source, "source"); long[] keys = source.keysToArray(), values = new long[keys.length];
        for (int i = 0; i < keys.length; i++) values[i] = source.getOrThrow(keys[i]);
        return copyOf(keys, values);
    }
    public int size() { return keys.length; }
    public boolean containsKey(long key) { return Arrays.binarySearch(keys, key) >= 0; }
    public long getOrDefault(long key, long fallback) {
        int at = Arrays.binarySearch(keys, key); return at < 0 ? fallback : values[at];
    }
    public long[] keysToArray() { return keys.clone(); }
    public long[] valuesToArray() { return values.clone(); }
    public long arrayPayloadBytes() { return 16L * keys.length; }
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); for (int i = 0; i < keys.length; i++) consumer.accept(keys[i], values[i]);
    }
    private static void sift(long[] k, long[] v, int at, int size) {
        while (at < (size >>> 1)) {
            int child = at * 2 + 1;
            if (child + 1 < size && k[child + 1] > k[child]) child++;
            if (k[at] >= k[child]) break;
            swap(k, v, at, child); at = child;
        }
    }
    private static void swap(long[] k, long[] v, int a, int b) {
        long key = k[a], value = v[a]; k[a] = k[b]; v[a] = v[b]; k[b] = key; v[b] = value;
    }
}
