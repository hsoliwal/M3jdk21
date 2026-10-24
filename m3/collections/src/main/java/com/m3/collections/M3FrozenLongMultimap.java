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
package com.m3.collections;

/**
 * M3 five-library superset scaffold {@code M3FrozenLongMultimap}: API group {@code FROZEN_LONG_MULTIMAP} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>GUAVA ImmutableListMultimap/ImmutableSetMultimap: frozen multimap; builder; UOE mutate [license Apache-2.0; NEW_OWNER (LS3); priority P1; matrix owner PROPOSED:M3FrozenMultimap lanes]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] keyLane;int[] valueOffsetLane;long[] valueLane}; offset scheme CSR: key ordinal -&gt; [valueOffset[k], valueOffset[k+1]); state-mask bits FROZEN; stored nodes NONE; API entry objects TRANSIENT_FACADE (transient facades over slots, never the stored representation); matrix note: frozen key lane + offsets lane + packed values lane (CSR layout) / FROZEN=1; occupancy ALL.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3FrozenLongMultimap {
    /** State-mask bit {@code FROZEN} (per-slot state word, bit 0). */
    static final long STATE_FROZEN = 1L << 0;

    /** Storage lane {@code keyLane}: dynamic primitive/reference array lane. */
    private long[] keyLane = new long[0];
    /** Storage lane {@code valueOffsetLane}: dynamic primitive/reference array lane. */
    private int[] valueOffsetLane = new int[0];
    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private long[] valueLane = new long[0];
    /** Per-slot state words (FROZEN); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3FrozenLongMultimap() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongMultimap|FROZEN_LONG_MULTIMAP|LONG|FROZEN|long[] keyLane;int[] valueOffsetLane;long[] valueLane";
    }

    /** Guava ImmutableListMultimap.copyOf (pairs) (scaffold: not yet received). */
    public static M3FrozenLongMultimap copyOf(long[] keys, long[] values) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.copyOf");
    }

    /** Multimap.size (pairs) (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3FrozenLongMultimap.size");
    }

    /** Multimap.keySet().size (scaffold: not yet received). */
    public int keyCount() {
        throw new M3NotYetReceived("M3FrozenLongMultimap.keyCount");
    }

    /** Multimap.containsKey (scaffold: not yet received). */
    public boolean containsKey(long key) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.containsKey");
    }

    /** Multimap.containsEntry (scaffold: not yet received). */
    public boolean containsEntry(long key, long value) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.containsEntry");
    }

    /** Multimap.get(key).size (scaffold: not yet received). */
    public int valueCount(long key) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.valueCount");
    }

    /** CSR key ordinal (scaffold: not yet received). */
    public long keyAt(int keyOrdinal) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.keyAt");
    }

    /** CSR offset lane (scaffold: not yet received). */
    public int valueOffsetAt(int keyOrdinal) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.valueOffsetAt");
    }

    /** CSR value lane (scaffold: not yet received). */
    public long valueAt(int valueOrdinal) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.valueAt");
    }

    /** Multimap.get (copy of run) (scaffold: not yet received). */
    public long[] get(long key) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.get");
    }

    /** Multimap.forEach (scaffold: not yet received). */
    public void forEach(M3LongLongConsumer action) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.forEach");
    }

    /** UOE contract once received (scaffold: not yet received). */
    public boolean put(long key, long value) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.put");
    }

    /** UOE contract once received (scaffold: not yet received). */
    public long[] removeAll(long key) {
        throw new M3NotYetReceived("M3FrozenLongMultimap.removeAll");
    }
}
