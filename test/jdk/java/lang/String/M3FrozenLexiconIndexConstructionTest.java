/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Frozen lexicon row markers, exact key groups and impossible displacement seeds
 * @modules java.base/java.lang:+open
 * @run main/othervm M3FrozenLexiconIndexConstructionTest
 * @run main/othervm -Xint M3FrozenLexiconIndexConstructionTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Exercises the actual java.lang index; no substitute implementation is loaded. */
public class M3FrozenLexiconIndexConstructionTest {
    private static final Class<?> INDEX;
    private static final Class<?> GROUPS;
    private static final Method BUILD;
    private static final Method CANDIDATE;
    private static final Method ROWS;
    private static final Method DISPLACEMENTS;
    private static final Method SEED;
    private static final Method PLACE;
    private static final Method MIX;
    private static final Constructor<?> GROUP_CONSTRUCTOR;
    private static long checks;

    static {
        try {
            INDEX = Class.forName("java.lang.M3FrozenLexiconIndex");
            GROUPS = Class.forName("java.lang.M3FrozenLexiconIndex$Groups");
            BUILD = method("build", int[].class, int[].class);
            CANDIDATE = method("candidate", int.class, int.class);
            ROWS = method("rows");
            DISPLACEMENTS = method("displacements");
            SEED = method("seed");
            PLACE = method("place", GROUPS, long.class, int.class, int.class);
            MIX = method("mix64", long.class);
            GROUP_CONSTRUCTOR = GROUPS.getDeclaredConstructor(long[].class, int[].class);
            GROUP_CONSTRUCTOR.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args[0].equals("geometry")) {
            impossibleSeeds();
            System.out.println("M3FrozenLexiconIndexConstructionTest geometry checks=" + checks);
            return;
        }
        rowMarkerRegression();
        impossibleSeeds();
        exactGroups();
        System.out.println("M3FrozenLexiconIndexConstructionTest checks=" + checks);
    }

    private static void rowMarkerRegression() throws Exception {
        // Aa and BB are different exact UTF-16 spellings with the same existing JDK key.
        int hash = "Aa".hashCode();
        check(hash == "BB".hashCode(), "fixture shares the Java hash");
        Object index = build(new int[] {hash, hash}, new int[] {2, 2});
        int encoded = candidate(index, hash, 2);
        int row = (encoded & Integer.MAX_VALUE) - 1;
        check(encoded < 0, "duplicate key selects the binary spelling authority");
        check(row >= 0 && row < 2,
                "ambiguous marker must preserve a valid row: encoded=" + encoded + ", row=" + row);
        check(encoded == (Integer.MIN_VALUE | 1), "ambiguous marker preserves first row + 1");
    }

    private static void impossibleSeeds() throws Exception {
        for (int slots : new int[] {2, 4, 16, 256, 4096}) {
            long seed = 0x512ecbad73019f46L ^ slots;
            long[] pair = aliasedKeys(seed, slots);
            // In one bucket, adding the same displacement modulo slots cannot separate
            // these two distinct keys. A seed must be rejected, never silently overwrite a row.
            Object groups = GROUP_CONSTRUCTOR.newInstance(pair, new int[] {1, 2});
            check(PLACE.invoke(null, groups, seed, 1, slots) == null,
                    "aliased base slots reject the seed, slots=" + slots);
            Object singleton = GROUP_CONSTRUCTOR.newInstance(new long[] {pair[0]}, new int[] {1});
            check(PLACE.invoke(null, singleton, seed, 1, slots) != null,
                    "same seed admits the singleton, slots=" + slots);
        }
    }

    private static long[] aliasedKeys(long seed, int slots) throws Exception {
        long[] first = new long[slots];
        boolean[] occupied = new boolean[slots];
        // slots + 1 distinct keys guarantee a collision in the selected base-slot lane.
        for (int hash = 0; hash <= slots; hash++) {
            long key = key(hash, 1);
            long mixed = (long) MIX.invoke(null, key ^ seed);
            int slot = ((int) (mixed >>> Integer.SIZE)) & (slots - 1);
            if (occupied[slot]) return new long[] {first[slot], key};
            occupied[slot] = true;
            first[slot] = key;
        }
        throw new AssertionError("pigeonhole fixture failed");
    }

    private static void exactGroups() throws Exception {
        Random random = new Random(0x63a17ef08b2L);
        for (int trial = 0; trial < 320; trial++) {
            int count = trial < 32 ? trial : random.nextInt(1025);
            int[] hashes = new int[count];
            int[] lengths = new int[count];
            for (int row = 0; row < count; row++) {
                hashes[row] = random.nextInt();
                lengths[row] = random.nextInt(257);
                if (row > 0 && row % 5 == 0) {
                    int prior = random.nextInt(row);
                    hashes[row] = hashes[prior];
                    lengths[row] = lengths[prior];
                }
            }
            verifyGroups(hashes, lengths, random);
        }
        // Include signed hash boundaries, zero-length facts and the same hash at different lengths.
        verifyGroups(new int[] {0, 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, -1},
                new int[] {0, 1, 0, Integer.MAX_VALUE, 2, 0}, random);
    }

    private static void verifyGroups(int[] hashes, int[] lengths, Random random) throws Exception {
        int[] beforeHashes = hashes.clone();
        int[] beforeLengths = lengths.clone();
        Map<Long, Integer> expectedRows = new LinkedHashMap<>();
        for (int row = 0; row < hashes.length; row++) {
            long key = key(hashes[row], lengths[row]);
            Integer previous = expectedRows.get(key);
            expectedRows.put(key, previous == null ? row + 1 : previous | Integer.MIN_VALUE);
        }
        Object index = build(hashes, lengths);
        int[] rows = (int[]) ROWS.invoke(index);
        check(hashes.length == 0 ? rows.length == 0 : rows.length > 0,
                "bounded test groups admit direct geometry");
        for (Map.Entry<Long, Integer> expected : expectedRows.entrySet()) {
            int hash = (int) (expected.getKey() >>> Integer.SIZE);
            int length = (int) (long) expected.getKey();
            int encoded = candidate(index, hash, length);
            check(encoded == expected.getValue(), "candidate preserves exact key group and row marker");
            int row = (encoded & Integer.MAX_VALUE) - 1;
            check(row >= 0 && row < hashes.length, "candidate row remains in its source image");
            check(hashes[row] == hash && lengths[row] == length, "candidate is the source key");
        }
        for (int probe = 0; probe < 48; probe++) {
            int hash = random.nextInt();
            int length = random.nextInt(513);
            if (expectedRows.containsKey(key(hash, length))) continue;
            int encoded = candidate(index, hash, length);
            if (encoded == 0) {
                check(true, "missing key has no candidate");
            } else {
                int row = (encoded & Integer.MAX_VALUE) - 1;
                check(row >= 0 && row < hashes.length, "missing-key candidate remains in bounds");
                check(hashes[row] != hash || lengths[row] != length,
                        "existing hash/length guard rejects a nonmember candidate");
            }
        }
        Object repeat = build(hashes, lengths);
        check(Arrays.equals(rows, (int[]) ROWS.invoke(repeat)), "deterministic row geometry");
        check(Arrays.equals((int[]) DISPLACEMENTS.invoke(index),
                (int[]) DISPLACEMENTS.invoke(repeat)), "deterministic displacement geometry");
        check(SEED.invoke(index).equals(SEED.invoke(repeat)), "deterministic seed");
        check(Arrays.equals(beforeHashes, hashes) && Arrays.equals(beforeLengths, lengths),
                "source fact lanes remain unchanged");
    }

    private static long key(int hash, int length) {
        return (Integer.toUnsignedLong(hash) << Integer.SIZE) | Integer.toUnsignedLong(length);
    }

    private static Object build(int[] hashes, int[] lengths) throws Exception {
        return BUILD.invoke(null, hashes, lengths);
    }

    private static int candidate(Object index, int hash, int length) throws Exception {
        return (int) CANDIDATE.invoke(index, hash, length);
    }

    private static Method method(String name, Class<?>... parameters) throws NoSuchMethodException {
        Method method = INDEX.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
