/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenMap construction preserves user hashCode and equals callback traces
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenMapCallbackTraceTest
 */
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import jdk.internal.m3.collections.FrozenMap;

public final class FrozenMapCallbackTraceTest {
    private static int hashes;
    private static int equals;
    private static long trace;
    private static long signatures;

    public static void main(String[] args) {
        for (int size : new int[] {0, 1, 7, 8, 9, 13, 20, 21, 64, 256}) {
            exercise(size, false, false);
            exercise(size, true, false);
            exercise(size, false, true);
        }
        System.out.println("FrozenMapCallbackTraceTest PASS: " + signatures + " signatures");
    }

    private static void exercise(int size, boolean hashMap, boolean untrusted) {
        Map<Key, Integer> source = hashMap ? new HashMap<>() : new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            source.put(new Key(i), i);
        }
        if (untrusted) {
            Map<Key, Integer> backing = source;
            source = new AbstractMap<>() {
                @Override public int size() {
                    throw new AssertionError("unknown map size() queried");
                }
                @Override public Set<Entry<Key, Integer>> entrySet() {
                    return backing.entrySet();
                }
            };
        }
        hashes = 0;
        equals = 0;
        trace = 0xcbf29ce484222325L;
        FrozenMap<Key, Integer> frozen = FrozenMap.copyOf(source);
        if (frozen.size() != size) throw new AssertionError("size");
        System.out.println((hashMap ? "hash" : untrusted ? "untrusted" : "linked")
                + "\t" + size + "\t" + hashes + "\t" + equals
                + "\t" + Long.toUnsignedString(trace, 16));
        signatures++;
    }

    private static final class Key implements Comparable<Key> {
        private final int id;
        Key(int id) { this.id = id; }
        @Override public int hashCode() {
            hashes++;
            trace = Long.rotateLeft(trace ^ ((long) id * 17 + 1), 5);
            return id & 15;
        }
        @Override public boolean equals(Object other) {
            equals++;
            int that = other instanceof Key key ? key.id : -1;
            trace = Long.rotateLeft(trace ^ ((long) id * 31 + that * 7L + 3), 3);
            return other instanceof Key key && id == key.id;
        }
        @Override public int compareTo(Key other) {
            return Integer.compare(id, other.id);
        }
    }
}
