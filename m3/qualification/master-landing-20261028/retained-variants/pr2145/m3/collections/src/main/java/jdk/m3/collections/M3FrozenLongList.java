/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/**
 * M3 five-library superset scaffold {@code M3FrozenLongList}: API group {@code FROZEN_LONG_LIST} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK List.of/Set.of/Map.of: JEP269 immutable; UOE mutate; null-hostile; dup-reject [license GPLv2+CPE; NEW_OWNER (LS1); priority P1; matrix owner PROPOSED:M3FrozenLongList/Set/Map]</li>
 *   <li>GUAVA ImmutableList: frozen list; subList views; asList everywhere [license Apache-2.0; NEW_OWNER (LS1 family); priority P1; matrix owner PROPOSED:M3FrozenLongList]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] valueLane}; offset scheme ordinal slot (dense 0..size); state-mask bits FROZEN; stored nodes NONE; API entry objects TRANSIENT_FACADE(Map.Entry) (transient facades over slots, never the stored representation); matrix note: frozen sorted/dense lanes; ordinal offsets; build-time copy / FROZEN=1 word; occupancy implicit ALL.
 *
 * <p>Status: M3-specific immutable primitive-array candidate. Inputs are copied on admission; 
 * returned arrays are detached snapshots. This is not a replacement for {@code java.util} APIs.
 * Original provider contracts remain independently qualified before target promotion.
 */
public final class M3FrozenLongList {
    private static final long[] EMPTY = new long[0];
    private static final long[] EMPTY_MASK = new long[0];
    /** State-mask bit {@code FROZEN} (per-slot state word, bit 0). */
    static final long STATE_FROZEN = 1L << 0;

    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private final long[] valueLane;
    /** Per-slot state words (FROZEN); one bit set per slot and state. */
    private final long[] stateMaskWords = EMPTY_MASK;
    /** Live slot count. */
    private final int size;
    private final int offset;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3FrozenLongList() { this(EMPTY, 0, 0); }

    private M3FrozenLongList(long[] frozen, int from, int length) {
        valueLane = frozen;
        offset = from;
        size = length;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongList|FROZEN_LONG_LIST|LONG|FROZEN|long[] valueLane";
    }

    /** JDK List.copyOf / Guava ImmutableList.copyOf (immutable primitive implementation candidate). */
    public static M3FrozenLongList copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        return new M3FrozenLongList(source.clone(), 0, source.length);
    }

    /** List.size (immutable primitive implementation candidate). */
    public int size() {
        return size;
    }

    /** List.isEmpty (immutable primitive implementation candidate). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** List.get (primitive) (immutable primitive implementation candidate). */
    public long get(int index) {
        return valueLane[offset + Objects.checkIndex(index, size)];
    }

    /** List.indexOf (immutable primitive implementation candidate). */
    public int indexOf(long value) {
        for (int i = 0; i < size; i++) if (valueLane[offset + i] == value) return i;
        return -1;
    }

    /** List.contains (immutable primitive implementation candidate). */
    public boolean contains(long value) {
        return indexOf(value) >= 0;
    }

    /** List.toArray (defensive copy) (immutable primitive implementation candidate). */
    public long[] toArray() {
        return Arrays.copyOfRange(valueLane, offset, offset + size);
    }

    /** List.subList (frozen view) (immutable primitive implementation candidate). */
    public M3FrozenLongList subList(int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, size);
        if (fromIndex == 0 && toIndex == size) return this;
        return new M3FrozenLongList(valueLane, offset + fromIndex, toIndex - fromIndex);
    }

    /** Iterable.forEach (immutable primitive implementation candidate). */
    public void forEach(java.util.function.LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueLane[offset + i]);
    }

    /** List.iterator (immutable primitive implementation candidate). */
    public java.util.PrimitiveIterator.OfLong iterator() {
        final long[] values = valueLane;
        final int from = offset, end = offset + size;
        return new PrimitiveIterator.OfLong() {
            private int cursor = from;
            @Override public boolean hasNext() { return cursor < end; }
            @Override public long nextLong() {
                if (cursor >= end) throw new NoSuchElementException();
                return values[cursor++];
            }
        };
    }

    /** UOE contract once received (JEP269) (immutable primitive implementation candidate). */
    public boolean add(long value) {
        throw new UnsupportedOperationException();
    }

    /** UOE contract once received (JEP269) (immutable primitive implementation candidate). */
    public long set(int index, long value) {
        throw new UnsupportedOperationException();
    }
}
