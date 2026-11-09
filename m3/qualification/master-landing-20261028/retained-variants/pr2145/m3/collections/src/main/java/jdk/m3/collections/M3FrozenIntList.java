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
 * M3 five-library superset scaffold {@code M3FrozenIntList}: API group {@code FROZEN_INT_LIST} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK List.of/Set.of/Map.of: JEP269 immutable; UOE mutate; null-hostile; dup-reject [license GPLv2+CPE; NEW_OWNER (LS1); priority P1; matrix owner PROPOSED:M3FrozenLongList/Set/Map]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind INT; lanes {@code int[] valueLane}; offset scheme ordinal slot (dense 0..size); state-mask bits FROZEN; stored nodes NONE; API entry objects TRANSIENT_FACADE(Map.Entry) (transient facades over slots, never the stored representation); matrix note: frozen sorted/dense lanes; ordinal offsets; build-time copy / FROZEN=1 word; occupancy implicit ALL.
 *
 * <p>Status: M3-specific immutable primitive-array candidate. Inputs are copied on admission; 
 * returned arrays are detached snapshots. This is not a replacement for {@code java.util} APIs.
 * Original provider contracts remain independently qualified before target promotion.
 */
public final class M3FrozenIntList {
    private static final int[] EMPTY = new int[0];
    private static final long[] EMPTY_MASK = new long[0];
    /** State-mask bit {@code FROZEN} (per-slot state word, bit 0). */
    static final long STATE_FROZEN = 1L << 0;

    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private final int[] valueLane;
    /** Per-slot state words (FROZEN); one bit set per slot and state. */
    private final long[] stateMaskWords = EMPTY_MASK;
    /** Live slot count. */
    private final int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3FrozenIntList() { this(EMPTY); }

    private M3FrozenIntList(int[] frozen) {
        valueLane = frozen;
        size = frozen.length;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenIntList|FROZEN_INT_LIST|INT|FROZEN|int[] valueLane";
    }

    /** JDK List.copyOf (int lane) (immutable primitive implementation candidate). */
    public static M3FrozenIntList copyOf(int[] source) {
        Objects.requireNonNull(source, "source");
        return new M3FrozenIntList(source.clone());
    }

    /** List.size (immutable primitive implementation candidate). */
    public int size() {
        return size;
    }

    /** List.get (primitive) (immutable primitive implementation candidate). */
    public int get(int index) {
        return valueLane[Objects.checkIndex(index, size)];
    }

    /** List.indexOf (immutable primitive implementation candidate). */
    public int indexOf(int value) {
        for (int i = 0; i < size; i++) if (valueLane[i] == value) return i;
        return -1;
    }

    /** List.toArray (defensive copy) (immutable primitive implementation candidate). */
    public int[] toArray() {
        return valueLane.clone();
    }

    /** Iterable.forEach (immutable primitive implementation candidate). */
    public void forEach(java.util.function.IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueLane[i]);
    }

    /** List.iterator (immutable primitive implementation candidate). */
    public java.util.PrimitiveIterator.OfInt iterator() {
        final int[] values = valueLane;
        return new PrimitiveIterator.OfInt() {
            private int cursor;
            @Override public boolean hasNext() { return cursor < values.length; }
            @Override public int nextInt() {
                if (cursor >= values.length) throw new NoSuchElementException();
                return values[cursor++];
            }
        };
    }

    /** UOE contract once received (JEP269) (immutable primitive implementation candidate). */
    public boolean add(int value) {
        throw new UnsupportedOperationException();
    }
}
