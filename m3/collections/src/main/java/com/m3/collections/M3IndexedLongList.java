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
 * M3 five-library superset scaffold {@code M3IndexedLongList}: API group {@code INDEXED_LONG_LIST} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>COMMONS TreeList: positional list on AVL; fast insert/remove mid [license Apache-2.0; NEW_OWNER (order-statistics; adjacent only: m3/long-range-orderstats-receiver-20261017 is range order-stats, not positional list); priority P2; matrix owner PROPOSED:indexed order-statistic lanes]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] valueLane;int[] leftLane;int[] rightLane;int[] sizeLane}; offset scheme node slot + order-statistic subtree sizes; state-mask bits OCCUPIED,DELETED,BALANCE; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: value lane + left/right/size index lanes / occupancy mask; balance bits.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3IndexedLongList {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;
    /** State-mask bit {@code BALANCE} (per-slot state word, bit 2). */
    static final long STATE_BALANCE = 1L << 2;

    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private long[] valueLane = new long[0];
    /** Storage lane {@code leftLane}: dynamic primitive/reference array lane. */
    private int[] leftLane = new int[0];
    /** Storage lane {@code rightLane}: dynamic primitive/reference array lane. */
    private int[] rightLane = new int[0];
    /** Storage lane {@code sizeLane}: dynamic primitive/reference array lane. */
    private int[] sizeLane = new int[0];
    /** Per-slot state words (OCCUPIED,DELETED,BALANCE); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3IndexedLongList() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3IndexedLongList|INDEXED_LONG_LIST|LONG|OCCUPIED,DELETED,BALANCE|long[] valueLane;int[] leftLane;int[] rightLane;int[] sizeLane";
    }

    /** TreeList.get O(log n) (long lane) (scaffold: not yet received). */
    public long get(int index) {
        throw new M3NotYetReceived("M3IndexedLongList.get");
    }

    /** TreeList.add(index) (scaffold: not yet received). */
    public void add(int index, long value) {
        throw new M3NotYetReceived("M3IndexedLongList.add");
    }

    /** TreeList.add (scaffold: not yet received). */
    public boolean add(long value) {
        throw new M3NotYetReceived("M3IndexedLongList.add");
    }

    /** TreeList.set (scaffold: not yet received). */
    public long set(int index, long value) {
        throw new M3NotYetReceived("M3IndexedLongList.set");
    }

    /** TreeList.remove(index) (scaffold: not yet received). */
    public long removeAt(int index) {
        throw new M3NotYetReceived("M3IndexedLongList.removeAt");
    }

    /** TreeList.indexOf (scaffold: not yet received). */
    public int indexOf(long value) {
        throw new M3NotYetReceived("M3IndexedLongList.indexOf");
    }

    /** List.size (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3IndexedLongList.size");
    }

    /** List.clear (scaffold: not yet received). */
    public void clear() {
        throw new M3NotYetReceived("M3IndexedLongList.clear");
    }
}
