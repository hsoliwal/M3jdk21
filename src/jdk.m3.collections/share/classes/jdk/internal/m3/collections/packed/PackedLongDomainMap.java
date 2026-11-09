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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import jdk.internal.m3.collections.LongDomainMap;

/**
 * Direct bounded long-key domain, lazy value lane and presence bits; single-threaded.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongDomainMap.java}
 * blob {@code 0e5752f2c9890a52fbddeb3a1dc9e24f4dc9dd31} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongDomainMap implements LongDomainMap {
    private static final long[] EMPTY = new long[0];
    private final long minimum;
    private final int slots;
    private long[] values = EMPTY;
    private long[] occupied = EMPTY;
    private int size;

    public PackedLongDomainMap(long minimum, int slots) {
        if (slots < 0 || slots > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("slots");
        if (slots > 0) Math.addExact(minimum, slots - 1L);
        this.minimum = minimum; this.slots = slots;
    }
    public long minimum() { return minimum; }
    public int domainSize() { return slots; }
    public int size() { return size; }
    public long arrayPayloadBytes() { return 8L * (values.length + (long) occupied.length); }
    public boolean containsKey(long key) { return present(index(key)); }
    public long getOrDefault(long key, long fallback) {
        int at = index(key); return present(at) ? values[at] : fallback;
    }
    /** Returns true on insertion and false on replacement. Out-of-domain keys throw. */
    public boolean put(long key, long value) {
        int at = index(key);
        if (values.length == 0) {
            long[] nextValues = new long[slots];
            long[] nextOccupied = new long[(int) ((slots + 63L) >>> 6)];
            values = nextValues; occupied = nextOccupied;
        }
        boolean added = !present(at);
        values[at] = value;
        if (added) { occupied[at >>> 6] |= 1L << (at & 63); size++; }
        return added;
    }
    public boolean remove(long key) {
        int at = index(key); if (!present(at)) return false;
        occupied[at >>> 6] &= ~(1L << (at & 63)); size--; return true;
    }
    public void clear() { Arrays.fill(occupied, 0); size = 0; }
    public void trimEmpty() { if (size == 0) { values = EMPTY; occupied = EMPTY; } }
    private boolean present(int at) {
        return occupied.length != 0 && (occupied[at >>> 6] & (1L << (at & 63))) != 0;
    }
    private int index(long key) {
        long delta = key - minimum;
        if (key < minimum || delta < 0 || delta >= slots) throw new IndexOutOfBoundsException("key outside domain: " + key);
        return (int) delta;
    }
}
