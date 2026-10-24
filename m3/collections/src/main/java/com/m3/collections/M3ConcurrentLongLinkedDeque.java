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
 * M3 five-library superset scaffold {@code M3ConcurrentLongLinkedDeque}: API group {@code CONCURRENT_LONG_DEQUE} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK ConcurrentLinkedQueue/Deque: lock-free linked CAS queue [license GPLv2+CPE; SUPERSET_VIEW VIEW_OF:M3LongQueue; priority P2; matrix owner M3LongQueue / M3LongDeque (interim)]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] valueLane;int[] prevLane;int[] nextLane}; offset scheme slot + CAS-published link lanes; state-mask bits OCCUPIED,DELETED; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: ring/chunk lanes + CAS indices / head/tail epoch words.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3ConcurrentLongLinkedDeque {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;

    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private long[] valueLane = new long[0];
    /** Storage lane {@code prevLane}: dynamic primitive/reference array lane. */
    private int[] prevLane = new int[0];
    /** Storage lane {@code nextLane}: dynamic primitive/reference array lane. */
    private int[] nextLane = new int[0];
    /** Per-slot state words (OCCUPIED,DELETED); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3ConcurrentLongLinkedDeque() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3ConcurrentLongLinkedDeque|CONCURRENT_LONG_DEQUE|LONG|OCCUPIED,DELETED|long[] valueLane;int[] prevLane;int[] nextLane";
    }

    /** ConcurrentLinkedDeque.addFirst (scaffold: not yet received). */
    public void addFirst(long value) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.addFirst");
    }

    /** ConcurrentLinkedDeque.addLast (scaffold: not yet received). */
    public void addLast(long value) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.addLast");
    }

    /** ConcurrentLinkedQueue.offer (scaffold: not yet received). */
    public boolean offer(long value) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.offer");
    }

    /** ConcurrentLinkedDeque.pollFirst (scaffold: not yet received). */
    public long pollFirstOrDefault(long defaultValue) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.pollFirstOrDefault");
    }

    /** ConcurrentLinkedDeque.pollLast (scaffold: not yet received). */
    public long pollLastOrDefault(long defaultValue) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.pollLastOrDefault");
    }

    /** ConcurrentLinkedDeque.peekFirst (scaffold: not yet received). */
    public long peekFirstOrDefault(long defaultValue) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.peekFirstOrDefault");
    }

    /** ConcurrentLinkedDeque.remove(Object) (scaffold: not yet received). */
    public boolean remove(long value) {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.remove");
    }

    /** Deque.isEmpty (scaffold: not yet received). */
    public boolean isEmpty() {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.isEmpty");
    }

    /** Deque.size (O(n) weakly consistent) (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.size");
    }

    /** Deque.clear (scaffold: not yet received). */
    public void clear() {
        throw new M3NotYetReceived("M3ConcurrentLongLinkedDeque.clear");
    }
}
