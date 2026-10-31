// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Append-only exact interner for text not present in a frozen standard lexicon.
 *
 * <p>Unknown text therefore remains addressable by an atom id. UTF-8 is the
 * compact storage path; malformed UTF-16 is retained in an exact sidecar so
 * Java String semantics are not silently changed.</p>
 */
public final class MIndexDynamicLiteralPool {
    private static final double LOAD_FACTOR = 0.65d;

    private byte[] arena = new byte[1024];
    private int arenaSize;
    private int[] offsets = new int[65];
    private int[] utf16Lengths = new int[64];
    private String[] exceptionalUtf16 = new String[64];
    private long[] hashes = new long[64];
    private int[] slots = new int[128];
    private int size;

    public synchronized long intern(CharSequence value) {
        Objects.requireNonNull(value, "value");
        String string = value.toString();
        byte[] utf8 = string.getBytes(StandardCharsets.UTF_8);
        long hash = MIndexDictionary.hash64(utf8);
        String exceptional = hasUnpairedSurrogate(string) ? string : null;
        int existing = find(utf8, hash, exceptional);
        if (existing >= 0) return existing;
        if ((long) size > MIndexAtomRef.MAX_ENTRY_ID) {
            throw new IllegalStateException("Dynamic literal pool exhausted 36-bit entry space");
        }

        ensureEntryCapacity(size + 1);
        ensureArenaCapacity(Math.addExact(arenaSize, utf8.length));
        ensureTableCapacity(size + 1);

        int id = size;
        offsets[id] = arenaSize;
        System.arraycopy(utf8, 0, arena, arenaSize, utf8.length);
        arenaSize += utf8.length;
        offsets[id + 1] = arenaSize;
        utf16Lengths[id] = string.length();
        exceptionalUtf16[id] = exceptional;
        hashes[id] = hash;
        size++;
        insert(id);
        return id;
    }

    public synchronized String materialize(long entryId) {
        int id = checkedId(entryId);
        if (exceptionalUtf16[id] != null) return exceptionalUtf16[id];
        return new String(arena, offsets[id], offsets[id + 1] - offsets[id], StandardCharsets.UTF_8);
    }

    public synchronized int utf16Length(long entryId) {
        return utf16Lengths[checkedId(entryId)];
    }

    public synchronized long hash64(long entryId) {
        return hashes[checkedId(entryId)];
    }

    public synchronized int size() {
        return size;
    }

    public synchronized long retainedUtf8Bytes() {
        return arenaSize;
    }

    private int find(byte[] query, long hash, String exceptional) {
        int mask = slots.length - 1;
        int slot = MIndexDictionary.mix(hash) & mask;
        while (true) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            int start = offsets[id];
            int length = offsets[id + 1] - start;
            if (hashes[id] == hash && length == query.length && Objects.equals(exceptionalUtf16[id], exceptional)) {
                boolean same = true;
                for (int i = 0; i < query.length; i++) {
                    if (arena[start + i] != query[i]) {
                        same = false;
                        break;
                    }
                }
                if (same) return id;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void insert(int id) {
        int mask = slots.length - 1;
        int slot = MIndexDictionary.mix(hashes[id]) & mask;
        while (slots[slot] != 0) slot = (slot + 1) & mask;
        slots[slot] = id + 1;
    }

    private void ensureTableCapacity(int required) {
        if (required <= (int) (slots.length * LOAD_FACTOR)) return;
        slots = new int[Math.multiplyExact(slots.length, 2)];
        for (int id = 0; id < size; id++) insert(id);
    }

    private void ensureEntryCapacity(int required) {
        if (required <= hashes.length) return;
        int capacity = hashes.length;
        while (capacity < required) capacity = Math.multiplyExact(capacity, 2);
        hashes = Arrays.copyOf(hashes, capacity);
        utf16Lengths = Arrays.copyOf(utf16Lengths, capacity);
        exceptionalUtf16 = Arrays.copyOf(exceptionalUtf16, capacity);
        offsets = Arrays.copyOf(offsets, capacity + 1);
    }

    private void ensureArenaCapacity(int required) {
        if (required <= arena.length) return;
        int capacity = arena.length;
        while (capacity < required) capacity = Math.multiplyExact(capacity, 2);
        arena = Arrays.copyOf(arena, capacity);
    }

    private int checkedId(long entryId) {
        if (entryId < 0 || entryId > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("dynamic entry id out of range: " + entryId);
        }
        return Objects.checkIndex((int) entryId, size);
    }

    private static boolean hasUnpairedSurrogate(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 == value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) return true;
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }
}
