// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * Tree-owned interner of name segments to dense int ids (TIL; pattern of Synexia {@code TokenInterner},
 * adapted to ids over one {@code char[]} pool instead of a String map).
 *
 * <p>Each distinct segment is stored once: rows keep a name id, so a tree with thousands of
 * {@code src}, {@code main} or {@code pom.xml} entries holds those chars once. Open addressing with
 * linear probing over a power-of-two table at load factor 1/2; no per-name object, no
 * {@code String.intern}. The interner lives and grows with its tree and is bounded by the tree's
 * distinct segments; ids are never evicted because rows reference them.</p>
 */
public final class M3FileNames {
    private int[] table;
    private int[] hashes;
    private int[] offsets;
    private char[] pool;
    private int count;
    private int poolTop;

    M3FileNames(int expected) {
        int capacity = Integer.highestOneBit(Math.max(4, expected) * 2 - 1) << 1;
        table = new int[capacity];
        hashes = new int[Math.max(4, expected)];
        offsets = new int[hashes.length + 1];
        pool = new char[Math.max(16, expected * 8)];
    }

    /** Id of {@code segment}, adding it on first sight. */
    int intern(String segment) {
        int hash = segment.hashCode();
        int slot = find(segment, hash);
        if (table[slot] != 0) return table[slot] - 1;
        int id = add(segment, hash);
        table[slot] = id + 1;
        if (count * 2 > table.length) rehash();
        return id;
    }

    /** Id of {@code segment}, or {@code -1} when it was never interned. */
    public int find(CharSequence segment) {
        Objects.requireNonNull(segment, "segment");
        int hash = 0;
        for (int i = 0; i < segment.length(); i++) hash = 31 * hash + segment.charAt(i);
        int id = table[find(segment, hash)];
        return id - 1;
    }

    /** Distinct segments. */
    public int count() {
        return count;
    }

    /** Chars held by the pool: the sum of distinct segment lengths. */
    public int poolChars() {
        return poolTop;
    }

    public int length(int id) {
        Objects.checkIndex(id, count);
        return offsets[id + 1] - offsets[id];
    }

    public char charAt(int id, int index) {
        Objects.checkIndex(index, length(id));
        return pool[offsets[id] + index];
    }

    /** Cold: materialises segment {@code id}. */
    public String name(int id) {
        Objects.checkIndex(id, count);
        return new String(pool, offsets[id], offsets[id + 1] - offsets[id]);
    }

    /** UTF-16 code unit order, then length (as {@link String#compareTo}); allocation-free. */
    public int compare(int a, int b) {
        return a == b ? 0 : compare(a, this, b);
    }

    /** {@link #compare(int, int)} across two interners (two trees); allocation-free. */
    public int compare(int id, M3FileNames other, int otherId) {
        int lengthA = length(id);
        int lengthB = other.length(otherId);
        int offA = offsets[id];
        int offB = other.offsets[otherId];
        int common = Math.min(lengthA, lengthB);
        for (int i = 0; i < common; i++) {
            int difference = pool[offA + i] - other.pool[offB + i];
            if (difference != 0) return difference;
        }
        return lengthA - lengthB;
    }

    void appendTo(int id, StringBuilder out) {
        out.append(pool, offsets[id], offsets[id + 1] - offsets[id]);
    }

    private int find(CharSequence segment, int hash) {
        int mask = table.length - 1;
        for (int slot = spread(hash) & mask; ; slot = (slot + 1) & mask) {
            int id = table[slot] - 1;
            if (id < 0 || (hashes[id] == hash && same(id, segment))) return slot;
        }
    }

    private boolean same(int id, CharSequence segment) {
        int length = offsets[id + 1] - offsets[id];
        if (length != segment.length()) return false;
        int offset = offsets[id];
        for (int i = 0; i < length; i++) {
            if (pool[offset + i] != segment.charAt(i)) return false;
        }
        return true;
    }

    private int add(String segment, int hash) {
        if (count == hashes.length) {
            hashes = Arrays.copyOf(hashes, count * 2);
            offsets = Arrays.copyOf(offsets, count * 2 + 1);
        }
        int length = segment.length();
        if (poolTop + length > pool.length) pool = Arrays.copyOf(pool, Math.max(pool.length * 2, poolTop + length));
        segment.getChars(0, length, pool, poolTop);
        hashes[count] = hash;
        offsets[count] = poolTop;
        poolTop += length;
        offsets[count + 1] = poolTop;
        return count++;
    }

    private void rehash() {
        int[] grown = new int[table.length * 2];
        int mask = grown.length - 1;
        for (int id = 0; id < count; id++) {
            int slot = spread(hashes[id]) & mask;
            while (grown[slot] != 0) slot = (slot + 1) & mask;
            grown[slot] = id + 1;
        }
        table = grown;
    }

    private static int spread(int hash) {
        return hash ^ (hash >>> 16);
    }
}
