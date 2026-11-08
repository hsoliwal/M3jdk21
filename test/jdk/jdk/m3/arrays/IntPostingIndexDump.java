// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Differential dump (gate 3 of the relocation, recipe/posting-index-20261106/differential.sh). Replays the oracle
 * fixtures of {@link M3IntPostingIndexContract} (the 1,024 public rounds with the same seed and pool),
 * {@link M3IntPostingIndexSkewContract} (the 65,536-row skew fixture) and
 * {@link M3IntPostingIndexConvergenceContract} (zero, sparse and 16,384-row dense fixtures), then 100,000 seeded
 * random posting lists, through the posting index class named by args[0], reached by reflection so that the donor
 * class and the relocated class run the same driver. For every input it prints the key directory, posting count,
 * payload bytes, and for every query the complete any/all row lanes (the first 256 rounds) or their length and
 * FNV-1a fingerprint, the four counts, the bounded counts, postingSize and contains probes and the rows(key) lane
 * of the first query key. Two posting index classes agree when their dumps are byte-identical.
 * Usage: {@code java -cp posting-classes;test-classes jdk.m3.arrays.IntPostingIndexDump FQN}
 */
//FLLDCIM: TFG - dump driver from the template M3CsrIndexDump (reflective, serves donor and relocated class); recipe m3/arrays/recipe/posting-index-20261106
public final class IntPostingIndexDump {
    private static final long RANDOM_SEED = 0x504F5354494E4753L;
    private static final int RANDOM_ROUNDS = 100_000;
    private static final int FULL_LANE_ROUNDS = 256;
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final int[] RANDOM_POOL = {Integer.MIN_VALUE, -99, -1, 0, 1, 2, 3, 17, 97, 1024, Integer.MAX_VALUE};
    private final Method ofPairs;
    private final Method any;
    private final Method all;
    private final Method countAny;
    private final Method countAll;
    private final Method countAnyUpTo;
    private final Method countAllUpTo;
    private final Method postingSize;
    private final Method contains;
    private final Method keyCount;
    private final Method postingCount;
    private final Method payloadBytes;
    private final Method keyDirectory;
    private final Method rowsOf;
    private final PrintStream out;
    private long inputs;
    private long queryCount;
    private long rowsAddressed;

    private IntPostingIndexDump(Class<?> posting, PrintStream out) throws ReflectiveOperationException {
        this.ofPairs = posting.getMethod("ofPairs", int[].class, int[].class, int.class);
        this.any = posting.getMethod("any", int[].class);
        this.all = posting.getMethod("all", int[].class);
        this.countAny = posting.getMethod("countAny", int[].class);
        this.countAll = posting.getMethod("countAll", int[].class);
        this.countAnyUpTo = posting.getMethod("countAnyUpTo", int.class, int[].class);
        this.countAllUpTo = posting.getMethod("countAllUpTo", int.class, int[].class);
        this.postingSize = posting.getMethod("postingSize", int.class);
        this.contains = posting.getMethod("contains", int.class, int.class);
        this.keyCount = posting.getMethod("keyCount");
        this.postingCount = posting.getMethod("postingCount");
        this.payloadBytes = posting.getMethod("primitivePayloadBytes");
        this.keyDirectory = posting.getMethod("keys");
        this.rowsOf = posting.getMethod("rows", int.class);
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        IntPostingIndexDump dump = new IntPostingIndexDump(Class.forName(args[0]), out);
        dump.gallopingPublicRounds();
        dump.skewFixture();
        dump.convergenceFixtures();
        dump.randomRounds();
        out.print("TOTAL inputs=" + dump.inputs + " queries=" + dump.queryCount + " rows=" + dump.rowsAddressed
                + " randomRounds=" + RANDOM_ROUNDS + "\n");
        out.flush();
    }

    /** The public rounds of the galloping oracle: same seed, universe, pool and query shape. */
    private void gallopingPublicRounds() throws ReflectiveOperationException {
        Random random = new Random(0x4d335055424c4943L);
        int[] pool = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        for (int round = 0; round < 1024; round++) {
            int universe = 1 + random.nextInt(513);
            int[] keys = new int[random.nextInt(600)], rows = new int[keys.length];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = pool[random.nextInt(pool.length)];
                rows[i] = random.nextInt(universe);
            }
            int[] query = new int[random.nextInt(8)];
            for (int i = 0; i < query.length; i++) query[i] = random.nextInt(8) == 0 ? 17 : pool[random.nextInt(pool.length)];
            dump("G" + round, keys, rows, universe, new int[][] {query}, true);
        }
    }

    /** The dense/sparse/odd/even skew fixture of the skew suite and its ten queries. */
    private void skewFixture() throws ReflectiveOperationException {
        final int universe = 65_536;
        final int sparse = 128;
        int[] keys = new int[universe + sparse + universe / 2 + universe / 2];
        int[] rows = new int[keys.length];
        int at = 0;
        for (int i = 0; i < universe; i++) { keys[at] = 0; rows[at++] = i; }
        for (int i = 0; i < sparse; i++) { keys[at] = -1; rows[at++] = i * 512; }
        for (int i = 0; i < universe; i += 2) { keys[at] = 1; rows[at++] = i; }
        for (int i = 1; i < universe; i += 2) { keys[at] = 2; rows[at++] = i; }
        int[][] queries = {{0, -1}, {0, -1, -1, 0}, {0, 1}, {0, 2}, {-1, 1}, {-1, 2}, {1, 2}, {-1, 1, 2}, {-1, 1000}, {}};
        dump("SKEW", keys, rows, universe, queries, false);
    }

    /** The zero, sparse and dense fixtures of the convergence oracle. */
    private void convergenceFixtures() throws ReflectiveOperationException {
        int[] empty = {};
        dump("C0", empty, empty, 0, new int[][] {{}, {Integer.MIN_VALUE, Integer.MAX_VALUE, 0}}, true);
        dump("C1", new int[] {-1, -1, 0, 0, 5, 5, 5, Integer.MIN_VALUE}, new int[] {0, 0, 1, 2, 2, 3, 3, 4}, 8,
                new int[][] {{-1, 0}, {5, 5, Integer.MIN_VALUE, -1, 42}, {}}, true);
        int rows = 16_384;
        int[] values = new int[rows * 2 + 8];
        int[] ids = new int[values.length];
        for (int i = 0; i < rows; i++) {
            values[i] = 0;
            ids[i] = i;
            values[rows + i] = 1 + (i % 7);
            ids[rows + i] = i;
        }
        for (int i = rows * 2; i < values.length; i++) {
            values[i] = 99;
            ids[i] = (i - rows * 2) * 2048;
        }
        dump("C2", values, ids, rows, new int[][] {{99, 0}, {0, 3, 3, 3}, {99, 0, 2}, {0}, {0, 1, 2, 3, 4, 5, 6, 7}}, false);
    }

    /** 100,000 seeded posting lists: universes up to 4,096, up to 2,048 pairs, skewed key pools, 0..6 query keys. */
    private void randomRounds() throws ReflectiveOperationException {
        Random random = new Random(RANDOM_SEED);
        for (int round = 0; round < RANDOM_ROUNDS; round++) {
            int universe = random.nextInt(4097);
            int pairs = universe == 0 ? 0 : random.nextInt(2049);
            int poolSize = 1 + random.nextInt(RANDOM_POOL.length);
            int[] keys = new int[pairs];
            int[] rows = new int[pairs];
            for (int i = 0; i < pairs; i++) {
                keys[i] = switch (round % 4) {
                    case 0 -> RANDOM_POOL[random.nextInt(poolSize)];
                    case 1 -> random.nextInt(3) == 0 ? RANDOM_POOL[random.nextInt(poolSize)] : 0;
                    case 2 -> random.nextInt(poolSize) * 7;
                    default -> random.nextInt(1 + universe / 8) - 3;
                };
                rows[i] = round % 3 == 2 ? random.nextInt(universe) : (random.nextInt(universe) / (1 + round % 5));
            }
            int[] query = new int[random.nextInt(7)];
            for (int i = 0; i < query.length; i++) {
                query[i] = random.nextInt(9) == 0 ? 5_000 : RANDOM_POOL[random.nextInt(RANDOM_POOL.length)];
                if (round % 4 == 2 && random.nextBoolean()) query[i] = random.nextInt(poolSize) * 7;
                if (round % 4 == 3 && random.nextBoolean()) query[i] = random.nextInt(1 + universe / 8) - 3;
            }
            dump("R" + round, keys, rows, universe, new int[][] {query}, round < FULL_LANE_ROUNDS);
        }
    }

    private void dump(String label, int[] keys, int[] rows, int universe, int[][] queries, boolean fullLanes)
            throws ReflectiveOperationException {
        inputs++;
        Object index = ofPairs.invoke(null, keys, rows, universe);
        int[] directory = (int[]) keyDirectory.invoke(index);
        out.print(label + " rows=" + universe + " pairs=" + keys.length + " keyCount=" + keyCount.invoke(index)
                + " postingCount=" + postingCount.invoke(index) + " payload=" + payloadBytes.invoke(index)
                + " keys=" + lane(directory, fullLanes) + "\n");
        for (int[] query : queries) {
            queryCount++;
            int[] anyLane = selected(any.invoke(index, (Object) query.clone()));
            int[] allLane = selected(all.invoke(index, (Object) query.clone()));
            rowsAddressed += anyLane.length + allLane.length;
            int limit = Math.max(1, anyLane.length / 2);
            StringBuilder probes = new StringBuilder();
            for (int key : query) {
                probes.append(' ').append(postingSize.invoke(index, key)).append(':')
                        .append(contains.invoke(index, key, 0)).append(':')
                        .append(contains.invoke(index, key, universe - 1));
            }
            int[] firstRows = query.length == 0 ? new int[0] : lane(rowsOf.invoke(index, query[0]));
            out.print(" q=" + Arrays.toString(query) + " any=" + lane(anyLane, fullLanes) + " all=" + lane(allLane, fullLanes)
                    + " countAny=" + countAny.invoke(index, (Object) query.clone())
                    + " countAll=" + countAll.invoke(index, (Object) query.clone())
                    + " anyUpTo(" + limit + ")=" + countAnyUpTo.invoke(index, limit, query.clone())
                    + " allUpTo(" + limit + ")=" + countAllUpTo.invoke(index, limit, query.clone())
                    + " anyUpTo(0)=" + countAnyUpTo.invoke(index, 0, query.clone())
                    + " allUpTo(1)=" + countAllUpTo.invoke(index, 1, query.clone())
                    + " probes=" + probes + " rows0=" + lane(firstRows, fullLanes) + "\n");
        }
    }

    private static int[] selected(Object selection) throws ReflectiveOperationException {
        return ((IntStream) selection.getClass().getMethod("rowsSelected").invoke(selection)).toArray();
    }

    private static int[] lane(Object list) throws ReflectiveOperationException {
        return (int[]) list.getClass().getMethod("toArray").invoke(list);
    }

    private static String lane(int[] values, boolean full) {
        if (full) return Arrays.toString(values);
        long hash = FNV_OFFSET;
        for (int value : values) {
            hash = (hash ^ (value & 0xffffffffL)) * FNV_PRIME;
        }
        return values.length + ":" + Long.toHexString(hash);
    }

}
