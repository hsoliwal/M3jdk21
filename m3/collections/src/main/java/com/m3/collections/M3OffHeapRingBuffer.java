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
 * M3 five-library superset scaffold {@code M3OffHeapRingBuffer}: API group {@code OFF_HEAP_RING_BUFFER} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>AGRONA ManyToOneRingBuffer/OneToOneRingBuffer: off-heap record ring buffer; msgTypeId framing; claim/commit/abort [license Apache-2.0; NEW_OWNER (ring-2: off-heap record ring absent from ring 1); priority P2; matrix owner PROPOSED:M3OffHeapRingBuffer]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind OBJECT; lanes {@code java.nio.ByteBuffer bufferLane;long[] cursorLane}; offset scheme record offset = tail &amp; (capacity - 1); header length+type; state-mask bits PADDING,COMMITTED,ABORTED; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: direct ByteBuffer lane; record header offsets (length,type); head/tail cursor lane / PADDING,COMMITTED,ABORTED per record.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3OffHeapRingBuffer {
    /** State-mask bit {@code PADDING} (per-slot state word, bit 0). */
    static final long STATE_PADDING = 1L << 0;
    /** State-mask bit {@code COMMITTED} (per-slot state word, bit 1). */
    static final long STATE_COMMITTED = 1L << 1;
    /** State-mask bit {@code ABORTED} (per-slot state word, bit 2). */
    static final long STATE_ABORTED = 1L << 2;

    /** Storage lane {@code bufferLane}: scalar lane. */
    private java.nio.ByteBuffer bufferLane;
    /** Storage lane {@code cursorLane}: dynamic primitive/reference array lane. */
    private long[] cursorLane = new long[0];
    /** Per-slot state words (PADDING,COMMITTED,ABORTED); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3OffHeapRingBuffer() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3OffHeapRingBuffer|OFF_HEAP_RING_BUFFER|OBJECT|PADDING,COMMITTED,ABORTED|java.nio.ByteBuffer bufferLane;long[] cursorLane";
    }

    /** RingBuffer.capacity (scaffold: not yet received). */
    public int capacity() {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.capacity");
    }

    /** RingBuffer.write (scaffold: not yet received). */
    public boolean write(int msgTypeId, java.nio.ByteBuffer source, int index, int length) {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.write");
    }

    /** RingBuffer.tryClaim (scaffold: not yet received). */
    public int tryClaim(int msgTypeId, int length) {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.tryClaim");
    }

    /** RingBuffer.commit (scaffold: not yet received). */
    public void commit(int index) {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.commit");
    }

    /** RingBuffer.abort (scaffold: not yet received). */
    public void abort(int index) {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.abort");
    }

    /** RingBuffer.read (offsets of committed records) (scaffold: not yet received). */
    public int read(java.util.function.IntConsumer recordOffsets, int messageCountLimit) {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.read");
    }

    /** RingBuffer.producerPosition (scaffold: not yet received). */
    public long producerPosition() {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.producerPosition");
    }

    /** RingBuffer.consumerPosition (scaffold: not yet received). */
    public long consumerPosition() {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.consumerPosition");
    }

    /** RingBuffer.size (bytes) (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3OffHeapRingBuffer.size");
    }
}
