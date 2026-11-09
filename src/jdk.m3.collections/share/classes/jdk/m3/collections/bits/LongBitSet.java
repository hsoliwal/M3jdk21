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
package jdk.m3.collections.bits;

import java.util.function.IntConsumer;
import jdk.internal.m3.collections.bits.PackedLongBitSet;

/**
 * Dense boolean set: one bit per non-negative ordinal, backed by 64-bit words.
 *
 * <p>API_UNION U2 (2026-11-09): one owner for the concept. The Synexia contract (com.synexia.common.collections
 * LongBitSet) and the queries of the former M3 class meet here; {@link #of(int)} returns the packed implementation.</p>
 */
public interface LongBitSet {
    /** Returns an empty set sized for 64 bits. */
    static LongBitSet of() { return new PackedLongBitSet(); }

    /** Returns an empty set sized for {@code bits} bits; it grows on demand. */
    static LongBitSet of(int bits) { return new PackedLongBitSet(bits); }

    /** Returns whether bit {@code bit} is set. */
    boolean get(int bit);

    /** Sets bit {@code bit}. */
    void set(int bit);

    /** Sets or clears bit {@code bit}. */
    default void set(int bit, boolean value) {
        if (value) set(bit); else clear(bit);
    }

    /** Clears bit {@code bit}. */
    void clear(int bit);

    /** Flips bit {@code bit}. */
    void flip(int bit);

    /** Clears every bit. */
    void clear();

    /** Clears every bit (Synexia name of {@link #clear()}). */
    default void clearAll() { clear(); }

    /** Returns the number of set bits. */
    int cardinality();

    /** Returns the first set bit at or after {@code from}, or -1. */
    int nextSetBit(int from);

    /** Returns the first clear bit at or after {@code from}. */
    int nextClearBit(int from);

    /** Returns the last set bit at or before {@code from}, or -1. */
    int previousSetBit(int from);

    /** Returns the last clear bit at or before {@code from}, or -1. */
    int previousClearBit(int from);

    /** Returns one past the highest set bit, or 0. */
    int length();

    /** Returns whether no bit is set. */
    boolean isEmpty();

    /** Returns the bit capacity of the current words. */
    int capacityBits();

    /** Returns the payload size in bytes. */
    long payloadBytes();

    /** Returns whether this set and {@code other} share a set bit. */
    boolean intersects(LongBitSet other);

    /** Calls {@code action} for every set bit in ascending order. */
    void forEachSetBit(IntConsumer action);

    /** Sets this set to its union with {@code other}. */
    void or(LongBitSet other);

    /** Sets this set to its intersection with {@code other}. */
    void and(LongBitSet other);

    /** Sets this set to its symmetric difference with {@code other}. */
    void xor(LongBitSet other);

    /** Clears every bit that is set in {@code other}. */
    void andNot(LongBitSet other);

    /** Returns a copy of the backing words. */
    long[] wordsCopy();

    /** Returns a copy of the backing words (M3 name of {@link #wordsCopy()}). */
    default long[] toLongArray() { return wordsCopy(); }
}
