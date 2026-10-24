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
 * M3 five-library superset scaffold {@code M3OffHeapLongLongMap}: API group {@code OFF_HEAP_LONG_MAP} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>CHRONICLE ChronicleMap: off-heap (optionally persisted, multi-process) hash map; fixed key/value sizing [license Apache-2.0; NEW_OWNER (ring-2: off-heap map absent from ring 1); priority P2; matrix owner PROPOSED:M3OffHeapLongLongMap]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code java.nio.ByteBuffer segmentLane}; offset scheme segment offset + slot * 16 bytes (key,value); state-mask bits OCCUPIED,DELETED,LOCKED; stored nodes NONE; API entry objects TRANSIENT_FACADE (transient facades over slots, never the stored representation); matrix note: direct ByteBuffer segment lane; slot offsets = segment*segmentSize + slot*entrySize / OCCUPIED,DELETED,LOCKED per slot.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3OffHeapLongLongMap {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;
    /** State-mask bit {@code LOCKED} (per-slot state word, bit 2). */
    static final long STATE_LOCKED = 1L << 2;

    /** Storage lane {@code segmentLane}: scalar lane. */
    private java.nio.ByteBuffer segmentLane;
    /** Per-slot state words (OCCUPIED,DELETED,LOCKED); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3OffHeapLongLongMap() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3OffHeapLongLongMap|OFF_HEAP_LONG_MAP|LONG|OCCUPIED,DELETED,LOCKED|java.nio.ByteBuffer segmentLane";
    }

    /** ChronicleMap.get (off-heap) (scaffold: not yet received). */
    public long getOrDefault(long key, long defaultValue) {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.getOrDefault");
    }

    /** ChronicleMap.put (scaffold: not yet received). */
    public long put(long key, long value) {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.put");
    }

    /** ChronicleMap.putIfAbsent (scaffold: not yet received). */
    public long putIfAbsent(long key, long value) {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.putIfAbsent");
    }

    /** ChronicleMap.remove (scaffold: not yet received). */
    public long remove(long key) {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.remove");
    }

    /** ChronicleMap.containsKey (scaffold: not yet received). */
    public boolean containsKey(long key) {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.containsKey");
    }

    /** ChronicleMap.offHeapMemoryUsed (scaffold: not yet received). */
    public long offHeapMemoryUsed() {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.offHeapMemoryUsed");
    }

    /** ChronicleMap.size (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.size");
    }

    /** ChronicleMap.clear (scaffold: not yet received). */
    public void clear() {
        throw new M3NotYetReceived("M3OffHeapLongLongMap.clear");
    }
}
