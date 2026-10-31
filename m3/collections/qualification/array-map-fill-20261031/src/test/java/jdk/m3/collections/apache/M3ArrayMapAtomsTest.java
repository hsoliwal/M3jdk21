/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * One JUnit test per ARRAY_MAP atom (the {@code Atoms} class of the generated members), named in the atom's row of
 * ATOMS.tsv: each checks the atom's Ensures and Effects clauses against a naive reference over random and edge inputs
 * that meet its Requires clause. The find test composes find, insert and positionOf through tombstones; the members are
 * generated from one template, so the last test asserts that every member carries the same atoms.
 */
class M3ArrayMapAtomsTest {

    private static final int SEEDS = 25;
    private static final int[] DENSITY_PERCENT = {0, 1, 7, 50, 93, 100};

    /** A key whose hash code is chosen, so probes collide on purpose. */
    private record Key(int id, int hash) {
        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.id == id;
        }
    }

    /** A key that counts the hashCode and equals calls made on it, for the Effects clauses. */
    private static final class Counting {
        private int hashCodes;
        private int equalsCalls;

        @Override
        public int hashCode() {
            hashCodes++;
            return 1;
        }

        @Override
        public boolean equals(Object other) {
            equalsCalls++;
            return other instanceof Counting;
        }
    }

    private static boolean[] randomBits(Random random, int length, int densityPercent) {
        boolean[] bits = new boolean[length];
        for (int i = 0; i < length; i++) {
            bits[i] = random.nextInt(100) < densityPercent;
        }
        return bits;
    }

    private static long[] pack(boolean[] bits) {
        long[] words = new long[Math.max(1, (bits.length + 63) >>> 6)];
        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                words[i >>> 6] |= 1L << i;
            }
        }
        return words;
    }

    /** An index over CAPACITY slots whose keys share few hash codes, so probe chains are long. */
    private static int[] collidingIndex(Random random, int capacity, int[] hashes, Object[] keys) {
        int[] index = new int[M3LinkedMap.Atoms.tableSize(capacity)];
        int distinctHashes = 1 + random.nextInt(13);
        for (int slot = 0; slot < capacity; slot++) {
            Key key = new Key(slot, random.nextInt(distinctHashes));
            keys[slot] = key;
            hashes[slot] = key.hashCode();
            M3LinkedMap.Atoms.insert(index, slot, hashes[slot]);
        }
        return index;
    }

    @Test
    void tableSizeIsTheSmallestPowerOfTwoKeepingLoadAtMostHalf() {
        for (int slots = 0; slots <= 5_000; slots++) {
            int size = M3LinkedMap.Atoms.tableSize(slots);
            int need = Math.max(2 * slots, 4);
            assertEquals(1, Integer.bitCount(size), "power of two for " + slots);
            assertTrue(size >= need, "room for twice " + slots);
            assertTrue(size / 2 < need, "smallest for " + slots);
        }
        assertEquals(1 << 30, M3LinkedMap.Atoms.tableSize(1 << 29), "the largest admitted SLOTS");
    }

    @Test
    void hashIsZeroForNullAndTheHashCodeOtherwise() {
        assertEquals(0, M3LinkedMap.Atoms.hash(null));
        assertEquals("abc".hashCode(), M3LinkedMap.Atoms.hash("abc"));
        assertEquals(-7, M3LinkedMap.Atoms.hash(new Key(1, -7)));
        Counting key = new Counting();
        M3LinkedMap.Atoms.hash(key);
        assertEquals(1, key.hashCodes, "hashCode is called once");
    }

    @Test
    void mixIsABijectionThatSpreadsSequentialKeys() {
        Set<Integer> seen = new HashSet<>();
        for (int h = -50_000; h < 50_000; h++) {
            assertTrue(seen.add(M3LinkedMap.Atoms.mix(h)), "mix is injective on " + h);
        }
        assertTrue(seen.add(M3LinkedMap.Atoms.mix(Integer.MIN_VALUE)) && seen.add(M3LinkedMap.Atoms.mix(Integer.MAX_VALUE)));
        Set<Integer> buckets = new HashSet<>();
        for (int h = 0; h < 1024; h++) {
            buckets.add(M3LinkedMap.Atoms.mix(h) & 1023);
        }
        assertTrue(buckets.size() > 600, "sequential keys spread over the low bits: " + buckets.size());
    }

    @Test
    void sameKeyIsNullSafeEqualsWithAnIdentityShortCut() {
        assertTrue(M3LinkedMap.Atoms.sameKey(null, null));
        assertFalse(M3LinkedMap.Atoms.sameKey(null, "x"));
        assertFalse(M3LinkedMap.Atoms.sameKey("x", null));
        assertTrue(M3LinkedMap.Atoms.sameKey(new Key(1, 9), new Key(1, 5)), "equals decides, not the hash");
        assertFalse(M3LinkedMap.Atoms.sameKey(new Key(1, 9), new Key(2, 9)));
        Counting key = new Counting();
        Counting stored = new Counting();
        assertTrue(M3LinkedMap.Atoms.sameKey(key, key));
        assertEquals(0, key.equalsCalls, "the same reference never calls equals");
        assertTrue(M3LinkedMap.Atoms.sameKey(key, stored));
        assertEquals(1, key.equalsCalls, "the lookup key's equals is called once");
        assertEquals(0, stored.equalsCalls, "the stored key's equals is never called");
    }

    @Test
    void findAgreesWithAReferenceMapThroughTombstones() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int capacity = 1 + random.nextInt(400);
            int[] hashes = new int[capacity];
            Object[] keys = new Object[capacity];
            int[] index = collidingIndex(random, capacity, hashes, keys);
            Map<Object, Integer> reference = new HashMap<>();
            for (int slot = 0; slot < capacity; slot++) {
                reference.put(keys[slot], slot);
            }
            for (int round = 0; round < capacity / 3; round++) { // tombstones on the probe chains
                int slot = random.nextInt(capacity);
                if (reference.remove(keys[slot]) != null) {
                    index[M3LinkedMap.Atoms.positionOf(index, hashes[slot], slot)] = -1;
                    assertEquals(-1, M3LinkedMap.Atoms.find(index, hashes, keys, keys[slot], hashes[slot]));
                }
            }
            int[] before = index.clone();
            for (Map.Entry<Object, Integer> entry : reference.entrySet()) {
                assertEquals(entry.getValue(), M3LinkedMap.Atoms.find(index, hashes, keys, entry.getKey(),
                        entry.getKey().hashCode()));
            }
            Key absent = new Key(-1, 0);
            assertEquals(-1, M3LinkedMap.Atoms.find(index, hashes, keys, absent, absent.hashCode()));
            assertArrayEquals(before, index, "find reads only");
        }
    }

    @Test
    void positionOfFindsTheEntryOfEveryIndexedSlot() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int capacity = 1 + random.nextInt(400);
            int[] hashes = new int[capacity];
            int[] index = collidingIndex(random, capacity, hashes, new Object[capacity]);
            Set<Integer> positions = new HashSet<>();
            for (int slot = 0; slot < capacity; slot++) {
                int position = M3LinkedMap.Atoms.positionOf(index, hashes[slot], slot);
                assertEquals(slot + 1, index[position], "entry of slot " + slot);
                assertTrue(positions.add(position), "one entry per slot");
            }
        }
    }

    @Test
    void insertTakesTheFirstFreeEntryAndReportsTombstoneReuse() {
        int[] index = new int[16];
        int mask = index.length - 1;
        int h = 5;
        int home = M3LinkedMap.Atoms.mix(h) & mask;
        for (int slot = 0; slot < 4; slot++) { // a collision chain fills consecutive entries from the home entry
            assertFalse(M3LinkedMap.Atoms.insert(index, slot, h), "no tombstone yet");
            assertEquals(slot + 1, index[home + slot & mask]);
        }
        index[home + 1 & mask] = -1;
        assertTrue(M3LinkedMap.Atoms.insert(index, 9, h), "the tombstone is the first free entry of the probe");
        assertEquals(10, index[home + 1 & mask]);
        assertFalse(M3LinkedMap.Atoms.insert(index, 10, h), "then the first empty entry after the chain");
        assertEquals(11, index[home + 4 & mask]);
        assertEquals(5, Arrays.stream(index).filter(e -> e != 0).count(), "one entry per insert, none overwritten");
        int[] wrap = new int[4];
        int last = 3;
        int key = 0;
        while ((M3LinkedMap.Atoms.mix(key) & 3) != last) {
            key++;
        }
        M3LinkedMap.Atoms.insert(wrap, 0, key);
        M3LinkedMap.Atoms.insert(wrap, 1, key);
        assertArrayEquals(new int[] {2, 0, 0, 1}, wrap, "the probe wraps from the last entry to the first");
    }

    @Test
    void nextLiveMatchesANaiveScan() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            for (int density : DENSITY_PERCENT) {
                int length = 1 + random.nextInt(300);
                boolean[] bits = randomBits(random, length, density);
                long[] mask = pack(bits);
                for (int from = 0; from <= length + 1; from++) {
                    int expected = -1;
                    for (int i = from; i < length; i++) {
                        if (bits[i]) {
                            expected = i;
                            break;
                        }
                    }
                    assertEquals(expected, M3LinkedMap.Atoms.nextLive(mask, from, length), "next " + from);
                }
            }
        }
    }

    @Test
    void prevLiveMatchesANaiveScan() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            for (int density : DENSITY_PERCENT) {
                int length = 1 + random.nextInt(300);
                boolean[] bits = randomBits(random, length, density);
                long[] mask = pack(bits);
                for (int from = -2; from < 64 * mask.length; from++) {
                    int expected = -1;
                    for (int i = Math.min(from, length - 1); i >= 0; i--) {
                        if (bits[i]) {
                            expected = i;
                            break;
                        }
                    }
                    assertEquals(expected, M3LinkedMap.Atoms.prevLive(mask, from), "prev " + from);
                }
            }
        }
    }

    @Test
    void selectMatchesANaiveCount() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            for (int density : DENSITY_PERCENT) {
                int length = 1 + random.nextInt(500);
                boolean[] bits = randomBits(random, length, density);
                long[] mask = pack(bits);
                int rank = 0;
                for (int slot = 0; slot < length; slot++) {
                    if (bits[slot]) {
                        assertEquals(slot, M3LinkedMap.Atoms.select(mask, rank), "select " + rank);
                        rank++;
                    }
                }
            }
        }
    }

    @Test
    void rankMatchesANaiveCount() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            for (int density : DENSITY_PERCENT) {
                int length = 1 + random.nextInt(500);
                boolean[] bits = randomBits(random, length, density);
                long[] mask = pack(bits);
                int count = 0;
                for (int slot = 0; slot < 64 * mask.length; slot++) {
                    assertEquals(count, M3LinkedMap.Atoms.rank(mask, slot), "rank " + slot);
                    if (slot < length && bits[slot]) {
                        count++;
                    }
                }
            }
        }
    }

    @Test
    void compactKeepsTheOrderAndEveryLiveSlot() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int used = 1 + random.nextInt(300);
            boolean[] bits = randomBits(random, used, 1 + random.nextInt(99));
            Object[] keys = new Object[used];
            Object[] values = new Object[used];
            int[] hashes = new int[used];
            for (int i = 0; i < used; i++) {
                keys[i] = "k" + i;
                values[i] = "v" + i;
                hashes[i] = i * 31;
            }
            long[] live = pack(bits);
            Object[][] source = {keys.clone(), values.clone()};
            long[] liveBefore = live.clone();
            Object[] toKeys = new Object[used];
            Object[] toValues = new Object[used];
            int[] toHashes = new int[used];
            long[] toLive = new long[(used + 63) >>> 6];
            int moved = M3LinkedMap.Atoms.compact(keys, values, hashes, live, used, toKeys, toValues, toHashes, toLive);
            int write = 0;
            for (int read = 0; read < used; read++) {
                if (bits[read]) {
                    assertEquals(keys[read], toKeys[write]);
                    assertEquals(values[read], toValues[write]);
                    assertEquals(hashes[read], toHashes[write]);
                    write++;
                }
            }
            assertEquals(write, moved);
            for (int i = 0; i < used; i++) {
                assertEquals(i < moved, (toLive[i >>> 6] & 1L << i) != 0, "live bit " + i);
            }
            assertArrayEquals(source[0], keys, "source keys unchanged");
            assertArrayEquals(source[1], values, "source values unchanged");
            assertArrayEquals(liveBefore, live, "LIVE unchanged");
        }
    }

    @Test
    void foldIdIsLongHashCode() {
        Random random = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            long id = random.nextLong();
            assertEquals(Long.hashCode(id), M3LinkedMap.Atoms.foldId(id));
        }
        for (long id : new long[] {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L << 32, (1L << 32) + 1}) {
            assertEquals(Long.hashCode(id), M3LinkedMap.Atoms.foldId(id), "edge " + id);
        }
    }

    @Test
    void findIdAgreesWithAReferenceMapThroughTombstones() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int capacity = 1 + random.nextInt(400);
            int[] index = new int[M3LinkedMap.Atoms.tableSize(capacity)];
            long[] ids = new long[capacity];
            Map<Long, Integer> reference = new HashMap<>();
            int distinctFolds = 1 + random.nextInt(13);
            for (int slot = 0; slot < capacity; slot++) {
                long id;
                do { // (high << 32) | (high ^ fold) folds to fold: few folds, long probe chains, ids still distinct
                    int high = random.nextInt();
                    id = (long) high << 32 | Integer.toUnsignedLong(high ^ random.nextInt(distinctFolds));
                } while (reference.containsKey(id));
                ids[slot] = id;
                M3LinkedMap.Atoms.insert(index, slot, M3LinkedMap.Atoms.foldId(id));
                reference.put(id, slot);
            }
            for (int round = 0; round < capacity / 3; round++) {
                int slot = random.nextInt(capacity);
                if (reference.remove(ids[slot]) != null) {
                    int h = M3LinkedMap.Atoms.foldId(ids[slot]);
                    index[M3LinkedMap.Atoms.positionOf(index, h, slot)] = -1;
                    assertEquals(-1, M3LinkedMap.Atoms.findId(index, ids, ids[slot], h));
                }
            }
            int[] before = index.clone();
            for (Map.Entry<Long, Integer> entry : reference.entrySet()) {
                long id = entry.getKey();
                assertEquals(entry.getValue(), M3LinkedMap.Atoms.findId(index, ids, id, M3LinkedMap.Atoms.foldId(id)));
            }
            long absent = Long.MIN_VALUE;
            assertEquals(-1, M3LinkedMap.Atoms.findId(index, ids, absent, M3LinkedMap.Atoms.foldId(absent)));
            assertArrayEquals(before, index, "findId reads only");
        }
    }

    @Test
    void compactIdsKeepsTheOrderAndEveryLiveSlot() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int used = 1 + random.nextInt(300);
            boolean[] bits = randomBits(random, used, 1 + random.nextInt(99));
            long[] ids = random.longs(used).toArray();
            Object[] values = new Object[used];
            for (int i = 0; i < used; i++) {
                values[i] = "v" + i;
            }
            long[] live = pack(bits);
            long[] idsBefore = ids.clone();
            long[] liveBefore = live.clone();
            long[] toIds = new long[used];
            Object[] toValues = new Object[used];
            long[] toLive = new long[(used + 63) >>> 6];
            int moved = M3LinkedMap.Atoms.compactIds(ids, values, live, used, toIds, toValues, toLive);
            int write = 0;
            for (int read = 0; read < used; read++) {
                if (bits[read]) {
                    assertEquals(ids[read], toIds[write]);
                    assertEquals(values[read], toValues[write]);
                    write++;
                }
            }
            assertEquals(write, moved);
            for (int i = 0; i < used; i++) {
                assertEquals(i < moved, (toLive[i >>> 6] & 1L << i) != 0, "live bit " + i);
            }
            assertArrayEquals(idsBefore, ids, "source ids unchanged");
            assertArrayEquals(liveBefore, live, "LIVE unchanged");
        }
    }

    @Test
    void everyMemberCarriesTheSameAtoms() throws Exception {
        Path implementations = Path.of(System.getProperty("user.dir")).resolve("../../implementations");
        String reference = null;
        for (String member : new String[] {"apache-hashed-map/M3HashedMap", "apache-linked-map/M3LinkedMap",
                "apache-lru-map/M3LRUMap", "m3-long-object-linked-hash-map/M3LongObjectLinkedHashMap"}) {
            String[] parts = member.split("/");
            String pkg = parts[1].startsWith("M3LongObject") ? "id" : "apache";
            String source = Files.readString(implementations.resolve(parts[0])
                    .resolve("src/main/java/jdk/m3/collections/" + pkg + "/" + parts[1] + ".java"), StandardCharsets.UTF_8);
            String atoms = source.substring(source.indexOf("    static final class Atoms {"));
            if (reference == null) {
                reference = atoms;
            }
            assertEquals(reference, atoms, parts[1] + " carries different atoms");
        }
    }
}
