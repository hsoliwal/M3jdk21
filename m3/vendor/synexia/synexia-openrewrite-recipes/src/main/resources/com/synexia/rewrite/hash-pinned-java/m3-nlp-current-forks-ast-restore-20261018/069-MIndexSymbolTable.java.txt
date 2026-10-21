// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.TreeSet;

/**
 * Immutable canonical UTF-8 symbol store shared by all standard lexicons in a snapshot.
 *
 * <p>The same surface therefore has one symbol id even when it belongs to
 * several namespaces (for example NASA as acronym + organization + proper noun).</p>
 */
public final class MIndexSymbolTable {
    private static final double LOAD_FACTOR = 0.65d;

    private final byte[] arena;
    private final int[] offsets;
    private final long[] hashes;
    private final int[] slots;
    private final long regionId = MIndexRegionIds.next();

    private MIndexSymbolTable(byte[] arena, int[] offsets, long[] hashes, int[] slots) {
        this.arena = arena;
        this.offsets = offsets;
        this.hashes = hashes;
        this.slots = slots;
    }

    public static MIndexSymbolTable freeze(Collection<String> supplied) {
        Objects.requireNonNull(supplied, "supplied");
        TreeSet<String> unique = new TreeSet<>(MIndexDictionary::compareCodePoint);
        for (String value : supplied) unique.add(Objects.requireNonNull(value, "symbol"));
        List<String> ordered = new ArrayList<>(unique);

        int byteCount = 0;
        for (String value : ordered) {
            byteCount = Math.addExact(byteCount, value.getBytes(StandardCharsets.UTF_8).length);
        }
        byte[] arena = new byte[byteCount];
        int[] offsets = new int[ordered.size() + 1];
        long[] hashes = new long[ordered.size()];

        int cursor = 0;
        for (int id = 0; id < ordered.size(); id++) {
            byte[] utf8 = ordered.get(id).getBytes(StandardCharsets.UTF_8);
            offsets[id] = cursor;
            System.arraycopy(utf8, 0, arena, cursor, utf8.length);
            cursor += utf8.length;
            offsets[id + 1] = cursor;
            hashes[id] = MIndexDictionary.hash64(utf8);
        }

        int tableSize = 2;
        while (ordered.size() > (int) (tableSize * LOAD_FACTOR)) {
            tableSize = Math.multiplyExact(tableSize, 2);
        }
        int[] slots = new int[tableSize];
        for (int id = 0; id < ordered.size(); id++) {
            int slot = MIndexDictionary.mix(hashes[id]) & (tableSize - 1);
            while (slots[slot] != 0) slot = (slot + 1) & (tableSize - 1);
            slots[slot] = id + 1;
        }

        return new MIndexSymbolTable(arena, offsets, hashes, slots);
    }

    public int size() {
        return hashes.length;
    }

    public OptionalLong lookup(String surface) {
        Objects.requireNonNull(surface, "surface");
        byte[] query = surface.getBytes(StandardCharsets.UTF_8);
        long hash = MIndexDictionary.hash64(query);
        int mask = slots.length - 1;
        int slot = MIndexDictionary.mix(hash) & mask;
        while (true) {
            int stored = slots[slot];
            if (stored == 0) return OptionalLong.empty();
            int id = stored - 1;
            if (hashes[id] == hash && bytesEqual(id, query)) return OptionalLong.of(id);
            slot = (slot + 1) & mask;
        }
    }

    public String materialize(long symbolId) {
        int id = checkedId(symbolId);
        return new String(arena, offsets[id], offsets[id + 1] - offsets[id], StandardCharsets.UTF_8);
    }

    public long hash64(long symbolId) {
        return hashes[checkedId(symbolId)];
    }

    /** Stable logical address for this immutable snapshot-local symbol. */
    public MIndexAddress storageAddress(long symbolId) {
        int id = checkedId(symbolId);
        return new MIndexAddress(regionId, offsets[id], offsets[id + 1] - offsets[id], 0L);
    }

    /** Symbol identity paired with its immutable snapshot-local storage coordinate. */
    public MIndexRef ref(long symbolId) {
        int id = checkedId(symbolId);
        return new MIndexRef(id, storageAddress(id));
    }

    public List<String> symbols() {
        List<String> result = new ArrayList<>(size());
        for (int id = 0; id < size(); id++) result.add(materialize(id));
        return List.copyOf(result);
    }

    private int checkedId(long symbolId) {
        if (symbolId < 0 || symbolId > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("symbol id out of range: " + symbolId);
        }
        return Objects.checkIndex((int) symbolId, size());
    }

    private boolean bytesEqual(int id, byte[] query) {
        int start = offsets[id];
        int length = offsets[id + 1] - start;
        if (length != query.length) return false;
        for (int i = 0; i < length; i++) {
            if (arena[start + i] != query[i]) return false;
        }
        return true;
    }
}
