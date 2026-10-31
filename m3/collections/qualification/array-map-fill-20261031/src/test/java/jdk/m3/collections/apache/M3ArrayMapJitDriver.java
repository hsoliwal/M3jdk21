/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import java.util.Arrays;
import java.util.Random;

/**
 * The hot callers of the JIT gate: one loop per ARRAY_MAP atom, each called often enough for C2 to compile it, on
 * inputs inside the atom's Requires clause. M3ArrayMapJitTest runs this class in a forked C2-only JVM with
 * -XX:+PrintInlining and reads the inlining decisions for every atom.
 */
final class M3ArrayMapJitDriver {

    private static final int ROUNDS = 40;
    private static final int CALLS = 50_000;
    private static final int KEYS = 256;
    private static final int KEY_MASK = KEYS - 1;
    private static final int SMALL_INDEX = 16;
    private static final int SMALL_FILL = 8;
    private static final int LIVE_BITS = 64;
    private static long sink;

    private M3ArrayMapJitDriver() {
    }

    public static void main(String[] args) {
        Random random = new Random(1);
        Object[] keys = new Object[KEYS];
        Object[] equalKeys = new Object[KEYS];
        int[] hashes = new int[KEYS];
        long[] ids = random.longs(KEYS).toArray();
        int[] index = new int[M3LinkedMap.Atoms.tableSize(KEYS)];
        int[] idIndex = new int[M3LinkedMap.Atoms.tableSize(KEYS)];
        for (int slot = 0; slot < KEYS; slot++) {
            keys[slot] = "key" + slot;
            equalKeys[slot] = new String("key" + slot);
            hashes[slot] = keys[slot].hashCode();
            M3LinkedMap.Atoms.insert(index, slot, hashes[slot]);
            M3LinkedMap.Atoms.insert(idIndex, slot, M3LinkedMap.Atoms.foldId(ids[slot]));
        }
        long[] mask = random.longs(KEYS / LIVE_BITS).toArray();
        int live = 0;
        for (long word : mask) {
            live += Long.bitCount(word);
        }
        for (int round = 0; round < ROUNDS; round++) {
            sink += scalars(keys, equalKeys, ids);
            sink += probes(index, idIndex, hashes, keys, ids);
            sink += bits(mask, live);
            sink += compactions(keys, hashes, ids, mask);
        }
        System.out.println("JIT_DRIVER_DONE " + (sink != Long.MIN_VALUE));
    }

    private static long scalars(Object[] keys, Object[] equalKeys, long[] ids) {
        long acc = 0;
        for (int i = 0; i < CALLS; i++) {
            acc += M3LinkedMap.Atoms.tableSize(i & KEY_MASK);
            acc += M3LinkedMap.Atoms.hash(keys[i & KEY_MASK]);
            acc += M3LinkedMap.Atoms.mix(i);
            acc += M3LinkedMap.Atoms.sameKey(keys[i & KEY_MASK], equalKeys[i & KEY_MASK]) ? 1 : 0;
            acc += M3LinkedMap.Atoms.foldId(ids[i & KEY_MASK]);
        }
        return acc;
    }

    private static long probes(int[] index, int[] idIndex, int[] hashes, Object[] keys, long[] ids) {
        long acc = 0;
        int[] small = new int[SMALL_INDEX];
        for (int i = 0; i < CALLS; i++) {
            int slot = i & KEY_MASK;
            acc += M3LinkedMap.Atoms.find(index, hashes, keys, keys[slot], hashes[slot]);
            acc += M3LinkedMap.Atoms.positionOf(index, hashes[slot], slot);
            acc += M3LinkedMap.Atoms.findId(idIndex, ids, ids[slot], M3LinkedMap.Atoms.foldId(ids[slot]));
            if ((i & (SMALL_FILL - 1)) == 0) {
                Arrays.fill(small, 0);
            }
            acc += M3LinkedMap.Atoms.insert(small, i & (SMALL_FILL - 1), i) ? 1 : 0;
        }
        return acc;
    }

    private static long bits(long[] mask, int live) {
        long acc = 0;
        for (int i = 0; i < CALLS; i++) {
            acc += M3LinkedMap.Atoms.nextLive(mask, i & KEY_MASK, KEYS);
            acc += M3LinkedMap.Atoms.prevLive(mask, i & KEY_MASK);
            acc += M3LinkedMap.Atoms.select(mask, i % live);
            acc += M3LinkedMap.Atoms.rank(mask, i & KEY_MASK);
        }
        return acc;
    }

    private static long compactions(Object[] keys, int[] hashes, long[] ids, long[] mask) {
        long acc = 0;
        Object[] toKeys = new Object[LIVE_BITS];
        Object[] toValues = new Object[LIVE_BITS];
        int[] toHashes = new int[LIVE_BITS];
        long[] toIds = new long[LIVE_BITS];
        long[] toLive = new long[1];
        for (int i = 0; i < CALLS; i++) {
            toLive[0] = 0;
            acc += M3LinkedMap.Atoms.compact(keys, keys, hashes, mask, LIVE_BITS, toKeys, toValues, toHashes, toLive);
            toLive[0] = 0;
            acc += M3LinkedMap.Atoms.compactIds(ids, keys, mask, LIVE_BITS, toIds, toValues, toLive);
        }
        return acc;
    }
}
