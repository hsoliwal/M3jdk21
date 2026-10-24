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
 * M3 five-library superset scaffold {@code M3SpscLongQueue}: API group {@code SPSC_LONG_QUEUE} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JCTOOLS MpscArrayQueue/SpscArrayQueue/MpmcArrayQueue (MessagePassingQueue): primitive long lanes of the same queues [license Apache-2.0; NEW_OWNER (ring-2); priority P2; matrix owner PROPOSED:M3SpscLongQueue (donor PackedLongSpscQueue@7184e5e8)]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] ringLane}; offset scheme ring: producerIndex &amp; mask; consumerIndex &amp; mask; state-mask bits PUBLISHED; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: long ring lane; head/tail offsets / slot published bits.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3SpscLongQueue {
    /** State-mask bit {@code PUBLISHED} (per-slot state word, bit 0). */
    static final long STATE_PUBLISHED = 1L << 0;

    /** Storage lane {@code ringLane}: dynamic primitive/reference array lane. */
    private long[] ringLane = new long[0];
    /** Per-slot state words (PUBLISHED); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3SpscLongQueue() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3SpscLongQueue|SPSC_LONG_QUEUE|LONG|PUBLISHED|long[] ringLane";
    }

    /** SpscArrayQueue.offer (long lane) (scaffold: not yet received). */
    public boolean offer(long value) {
        throw new M3NotYetReceived("M3SpscLongQueue.offer");
    }

    /** SpscArrayQueue.poll (scaffold: not yet received). */
    public long pollOrDefault(long defaultValue) {
        throw new M3NotYetReceived("M3SpscLongQueue.pollOrDefault");
    }

    /** Queue.peek (scaffold: not yet received). */
    public long peekOrDefault(long defaultValue) {
        throw new M3NotYetReceived("M3SpscLongQueue.peekOrDefault");
    }

    /** MessagePassingQueue.drain (scaffold: not yet received). */
    public int drain(java.util.function.LongConsumer consumer, int limit) {
        throw new M3NotYetReceived("M3SpscLongQueue.drain");
    }

    /** MessagePassingQueue.capacity (scaffold: not yet received). */
    public int capacity() {
        throw new M3NotYetReceived("M3SpscLongQueue.capacity");
    }

    /** Queue.size (approximate) (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3SpscLongQueue.size");
    }
}
