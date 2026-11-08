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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Byte-sequence -> long map: one byte arena, primitive offset/length/value lanes and
 * an open-addressed integer index. Key bytes are copied on insertion; caller mutation
 * cannot corrupt the map. Hashes only choose probes; equality compares actual bytes.
 * No byte[]/String/key/entry object is retained per mapping. Empty and arbitrary binary
 * keys are supported. String convenience methods allocate UTF-8 conversion buffers;
 * byte-slice point operations avoid that conversion. Not thread-safe.
 *
 * Deletion leaves dead arena bytes until explicit compaction or growth pressure. Stable
 * row handles are NOT promised: swap-delete invalidates row ordinals. Export uses
 * caller-owned buffers rather than exposing the mutable key arena.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedByteLongMap.java}
 * blob {@code 86510df005ab5cc59baadc78a213eea9316d2f7f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedByteLongMap {
    private byte[] arena;
    private int[] offsets;
    private int[] lengths;
    private long[] values;
    private int[] slots;
    private int size;
    private int usedBytes;
    private int liveBytes;
    private final int maxArenaBytes;
    private int callbacks;

    public PackedByteLongMap() { this(8, 64, Integer.MAX_VALUE - 8); }
    public PackedByteLongMap(int expectedKeys, int initialArenaBytes, int maxArenaBytes) {
        int capacity = PackedLongRowIndex.capacityFor(expectedKeys);
        if (initialArenaBytes < 0 || maxArenaBytes < initialArenaBytes || maxArenaBytes > Integer.MAX_VALUE - 8)
            throw new IllegalArgumentException("invalid arena limits");
        this.maxArenaBytes = maxArenaBytes;
        arena = new byte[initialArenaBytes];
        offsets = new int[Math.max(1, expectedKeys)]; lengths = new int[offsets.length];
        values = new long[offsets.length]; slots = new int[capacity];
    }
    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    private static void bounds(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes"); Objects.checkFromIndexSize(offset, length, bytes.length);
    }
    private static int hash(byte[] bytes, int offset, int length) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < length; i++) h = (h ^ (bytes[offset + i] & 255)) * 0x100000001b3L;
        return PackedLongRowIndex.hash(h, 0, false);
    }
    private boolean equal(int row, byte[] bytes, int offset, int length) {
        if (lengths[row] != length) return false;
        int base = offsets[row];
        for (int i = 0; i < length; i++) if (arena[base + i] != bytes[offset + i]) return false;
        return true;
    }
    private int findSlot(byte[] bytes, int offset, int length) {
        int mask = slots.length - 1, at = hash(bytes, offset, length) & mask;
        while (slots[at] != 0 && !equal(slots[at] - 1, bytes, offset, length)) at = (at + 1) & mask;
        return at;
    }
    private void reserveRows() {
        int required = Math.addExact(size, 1);
        int tableCapacity = PackedLongRowIndex.capacityFor(required);
        if (tableCapacity > slots.length) {
            int[] fresh = new int[tableCapacity];
            for (int i = 0; i < size; i++) {
                int at = hash(arena, offsets[i], lengths[i]) & (tableCapacity - 1);
                while (fresh[at] != 0) at = (at + 1) & (tableCapacity - 1);
                fresh[at] = i + 1;
            }
            slots = fresh;
        }
        if (required > offsets.length) {
            int n = PackedLongRowIndex.grownLength(offsets.length, required);
            int[] o = Arrays.copyOf(offsets, n), l = Arrays.copyOf(lengths, n);
            long[] v = Arrays.copyOf(values, n);
            offsets = o; lengths = l; values = v;
        }
    }
    private void reserveBytes(int length) {
        if ((long) liveBytes + length > maxArenaBytes)
            throw new IllegalStateException("live key bytes exceed arena budget");
        if ((long) usedBytes + length <= arena.length) return;
        if (usedBytes != liveBytes) compactKeys();
        int required = Math.addExact(usedBytes, length);
        if (required > arena.length) {
            int n = (int) Math.min(maxArenaBytes,
                    Math.max(required, Math.max(8L, arena.length + (arena.length >>> 1) + 1L)));
            arena = Arrays.copyOf(arena, n);
        }
    }
    /** Returns true for a new key, false for replacement. */
    public boolean put(byte[] bytes, int offset, int length, long value) {
        writable(); bounds(bytes, offset, length);
        int at = findSlot(bytes, offset, length);
        if (slots[at] != 0) { values[slots[at] - 1] = value; return false; }
        // Budget refusal precedes any growth or content mutation.
        if ((long) liveBytes + length > maxArenaBytes)
            throw new IllegalStateException("live key bytes exceed arena budget");
        reserveRows(); reserveBytes(length);
        at = findSlot(bytes, offset, length);
        System.arraycopy(bytes, offset, arena, usedBytes, length);
        offsets[size] = usedBytes; lengths[size] = length; values[size] = value;
        slots[at] = ++size; usedBytes += length; liveBytes += length;
        return true;
    }
    public boolean put(byte[] key, long value) { Objects.requireNonNull(key, "key"); return put(key, 0, key.length, value); }
    public boolean putUtf8(String key, long value) { return put(Objects.requireNonNull(key, "key").getBytes(StandardCharsets.UTF_8), value); }
    public boolean contains(byte[] bytes, int offset, int length) {
        bounds(bytes, offset, length); return slots[findSlot(bytes, offset, length)] != 0;
    }
    public long getOrDefault(byte[] bytes, int offset, int length, long fallback) {
        bounds(bytes, offset, length); int encoded = slots[findSlot(bytes, offset, length)];
        return encoded == 0 ? fallback : values[encoded - 1];
    }
    public long getUtf8OrDefault(String key, long fallback) {
        byte[] bytes = Objects.requireNonNull(key, "key").getBytes(StandardCharsets.UTF_8);
        return getOrDefault(bytes, 0, bytes.length, fallback);
    }
    public boolean remove(byte[] bytes, int offset, int length) {
        writable(); bounds(bytes, offset, length);
        int gap = findSlot(bytes, offset, length);
        if (slots[gap] == 0) return false;
        int row = slots[gap] - 1, mask = slots.length - 1, scan = (gap + 1) & mask;
        while (slots[scan] != 0) {
            int r = slots[scan] - 1;
            int home = hash(arena, offsets[r], lengths[r]) & mask;
            if (((scan - home) & mask) >= ((scan - gap) & mask)) { slots[gap] = slots[scan]; gap = scan; }
            scan = (scan + 1) & mask;
        }
        slots[gap] = 0;
        liveBytes -= lengths[row]; int tail = --size;
        if (row != tail) {
            int at = findSlot(arena, offsets[tail], lengths[tail]);
            if (slots[at] != tail + 1) throw new IllegalStateException("missing arena relocation row");
            slots[at] = row + 1;
            offsets[row] = offsets[tail]; lengths[row] = lengths[tail]; values[row] = values[tail];
        }
        return true;
    }
    /** O(total live key bytes), allocating one replacement arena and offset lane. */
    public void compactKeys() {
        writable();
        byte[] replacement = new byte[liveBytes]; int[] positions = new int[offsets.length]; int at = 0;
        for (int i = 0; i < size; i++) {
            System.arraycopy(arena, offsets[i], replacement, at, lengths[i]);
            positions[i] = at; at += lengths[i];
        }
        arena = replacement; offsets = positions; usedBytes = liveBytes;
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int liveKeyBytes() { return liveBytes; }
    public int deadKeyBytes() { return usedBytes - liveBytes; }
    public int keyLengthAt(int row) { Objects.checkIndex(row, size); return lengths[row]; }
    public long valueAt(int row) { Objects.checkIndex(row, size); return values[row]; }
    public int copyKeyAt(int row, byte[] destination, int offset) {
        Objects.checkIndex(row, size); bounds(destination, offset, lengths[row]);
        System.arraycopy(arena, offsets[row], destination, offset, lengths[row]); return lengths[row];
    }
    /** Visitor receives ephemeral row ordinal and value. Use copyKeyAt with a reusable buffer. */
    public void forEachRow(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) consumer.accept(i, values[i]); }
        finally { callbacks--; }
    }
    public long arrayPayloadBytes() {
        return arena.length + 16L * offsets.length + 4L * slots.length;
    }
    public void clear() { writable(); Arrays.fill(slots, 0); size = 0; usedBytes = 0; liveBytes = 0; }
}
