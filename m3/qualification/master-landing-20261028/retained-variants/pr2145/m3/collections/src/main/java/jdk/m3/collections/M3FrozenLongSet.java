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
 * M3 five-library superset scaffold {@code M3FrozenLongSet}: API group {@code FROZEN_LONG_SET} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK List.of/Set.of/Map.of: JEP269 immutable; UOE mutate; null-hostile; dup-reject [license GPLv2+CPE; NEW_OWNER (LS1) (adjacent prior art: merged #1057 M3LongSortedSnapshot, #979 M3LongCompiledMembership = index snapshots, not the Set contract); priority P1; matrix owner PROPOSED:M3FrozenLongList/Set/Map]</li>
 *   <li>GUAVA ImmutableSortedSet: frozen sorted set; navigable; comparator [license Apache-2.0; NEW_OWNER (LS1) (adjacent prior art: merged #1057 M3LongSortedSnapshot, #979 M3LongCompiledMembership = index snapshots, not the Set contract); priority P1; matrix owner PROPOSED:M3FrozenLongSet (natural order)]</li>
 *   <li>ECLIPSE ImmutableList/Set/Map/Bag/SortedSet/SortedMap/SortedBag/Multimap: frozen families; toImmutable() everywhere; UOE mutate [license EPL-2.0|EDL-1.0; NEW_OWNER (LS1-LS3 claim EC frozen contracts via own oracles) (adjacent prior art: merged #1057 M3LongSortedSnapshot, #979 M3LongCompiledMembership = index snapshots, not the Set contract); priority P1; matrix owner PROPOSED:M3Frozen* family (same owners as LS1-LS3)]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] valueLane}; offset scheme sorted ordinal slot; binary-search offsets; state-mask bits FROZEN; stored nodes NONE; API entry objects TRANSIENT_FACADE(Map.Entry) (transient facades over slots, never the stored representation); matrix note: frozen sorted/dense lanes; ordinal offsets; build-time copy / FROZEN=1 word; occupancy implicit ALL.
 *
 * <p>Status: M3-specific immutable primitive-array candidate. Inputs are copied on admission; 
 * returned arrays are detached snapshots. This is not a replacement for {@code java.util} APIs.
 * Original provider contracts remain independently qualified before target promotion.
 */
public final class M3FrozenLongSet {
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

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3FrozenLongSet() { this(EMPTY); }

    private M3FrozenLongSet(long[] sortedUnique) {
        valueLane = sortedUnique;
        size = sortedUnique.length;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongSet|FROZEN_LONG_SET|LONG|FROZEN|long[] valueLane";
    }

    /** JDK Set.copyOf / Guava ImmutableSortedSet.copyOf (immutable primitive implementation candidate). */
    public static M3FrozenLongSet copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        long[] values = source.clone();
        Arrays.sort(values);
        int kept = 0;
        for (int read = 0; read < values.length; read++) {
            long value = values[read];
            if (kept == 0 || value != values[kept - 1]) {
                if (kept != read) values[kept] = value;
                kept++;
            }
        }
        return new M3FrozenLongSet(kept == values.length ? values : Arrays.copyOf(values, kept));
    }

    /** Set.size (immutable primitive implementation candidate). */
    public int size() {
        return size;
    }

    /** Set.isEmpty (immutable primitive implementation candidate). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Set.contains (immutable primitive implementation candidate). */
    public boolean contains(long value) {
        return Arrays.binarySearch(valueLane, 0, size, value) >= 0;
    }

    /** ImmutableSortedSet.asList().get (immutable primitive implementation candidate). */
    public long get(int ordinal) {
        return valueLane[Objects.checkIndex(ordinal, size)];
    }

    /** SortedSet.first (immutable primitive implementation candidate). */
    public long first() {
        if (size == 0) throw new NoSuchElementException();
        return valueLane[0];
    }

    /** SortedSet.last (immutable primitive implementation candidate). */
    public long last() {
        if (size == 0) throw new NoSuchElementException();
        return valueLane[size - 1];
    }

    /** ImmutableSortedSet.indexOf (immutable primitive implementation candidate). */
    public int rank(long value) {
        int index = Arrays.binarySearch(valueLane, 0, size, value);
        return index >= 0 ? index : -1;
    }

    /** Set.toArray (defensive copy) (immutable primitive implementation candidate). */
    public long[] toArray() {
        return valueLane.clone();
    }

    /** Iterable.forEach (immutable primitive implementation candidate). */
    public void forEach(java.util.function.LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueLane[i]);
    }

    /** Set.iterator (ascending) (immutable primitive implementation candidate). */
    public java.util.PrimitiveIterator.OfLong iterator() {
        final long[] values = valueLane;
        return new PrimitiveIterator.OfLong() {
            private int cursor;
            @Override public boolean hasNext() { return cursor < values.length; }
            @Override public long nextLong() {
                if (cursor >= values.length) throw new NoSuchElementException();
                return values[cursor++];
            }
        };
    }

    /** UOE contract once received (JEP269) (immutable primitive implementation candidate). */
    public boolean add(long value) {
        throw new UnsupportedOperationException();
    }

    /** UOE contract once received (JEP269) (immutable primitive implementation candidate). */
    public boolean remove(long value) {
        throw new UnsupportedOperationException();
    }
}
