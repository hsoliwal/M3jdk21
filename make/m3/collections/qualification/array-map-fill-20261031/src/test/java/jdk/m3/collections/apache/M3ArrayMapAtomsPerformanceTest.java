/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * One performance row per ARRAY_MAP atom, labelled as in the perf_row column of ATOMS.tsv: the atom against its naive
 * or java.util reference on the same input, best of several timed rounds after warm-up, written to
 * {@code target/perf/ARRAY_MAP_ATOMS_PERF.tsv}. Asserted only where the gap is structural (word scans and popcounts
 * against bit-by-bit loops on sparse masks); the rest is a report.
 */
@Tag("performance")
class M3ArrayMapAtomsPerformanceTest {

    private static final int WARMUP = 3;
    private static final int ROUNDS = 7;
    private static final int BITS = 1 << 16;
    private static final int OPS = 20_000;
    private static final List<String> ROWS = new ArrayList<>();
    private static long sink;

    private static double nsPerOp(IntUnaryOperator op) {
        long best = Long.MAX_VALUE;
        for (int round = 0; round < WARMUP + ROUNDS; round++) {
            long t0 = System.nanoTime();
            long acc = 0;
            for (int i = 0; i < OPS; i++) {
                acc += op.applyAsInt(i);
            }
            long nanos = System.nanoTime() - t0;
            sink += acc;
            if (round >= WARMUP) {
                best = Math.min(best, nanos);
            }
        }
        return (double) best / OPS;
    }

    private static double row(String atom, IntUnaryOperator m3, IntUnaryOperator naive) {
        double fast = nsPerOp(m3);
        double slow = nsPerOp(naive);
        ROWS.add(String.format(Locale.ROOT, "%s\t%.2f\t%.2f\t%.1f", atom, fast, slow, slow / Math.max(fast, 1e-9)));
        return slow / Math.max(fast, 1e-9);
    }

    private static boolean[] sparse(Random random, int densityPerMille) {
        boolean[] bits = new boolean[BITS];
        for (int i = 0; i < BITS; i++) {
            bits[i] = random.nextInt(1000) < densityPerMille;
        }
        return bits;
    }

    private static long[] pack(boolean[] bits) {
        long[] words = new long[(bits.length + 63) >>> 6];
        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                words[i >>> 6] |= 1L << i;
            }
        }
        return words;
    }

    @Test
    void bitAtomsBeatABitByBitScan() {
        Random random = new Random(1);
        boolean[] bits = sparse(random, 10); // 1% live: the case after many removals
        long[] mask = pack(bits);
        int[] from = random.ints(OPS, 0, BITS).toArray();
        int live = 0;
        for (boolean bit : bits) {
            live += bit ? 1 : 0;
        }
        int[] ranks = random.ints(OPS, 0, Math.max(1, live)).toArray();
        double next = row("nextLive", i -> M3LinkedMap.Atoms.nextLive(mask, from[i], BITS), i -> {
            for (int s = from[i]; s < BITS; s++) {
                if (bits[s]) {
                    return s;
                }
            }
            return -1;
        });
        double prev = row("prevLive", i -> M3LinkedMap.Atoms.prevLive(mask, from[i]), i -> {
            for (int s = from[i]; s >= 0; s--) {
                if (bits[s]) {
                    return s;
                }
            }
            return -1;
        });
        double select = row("select", i -> M3LinkedMap.Atoms.select(mask, ranks[i]), i -> {
            int seen = 0;
            for (int s = 0; s < BITS; s++) {
                if (bits[s] && seen++ == ranks[i]) {
                    return s;
                }
            }
            return -1;
        });
        double rank = row("rank", i -> M3LinkedMap.Atoms.rank(mask, from[i]), i -> {
            int count = 0;
            for (int s = 0; s < from[i]; s++) {
                count += bits[s] ? 1 : 0;
            }
            return count;
        });
        assertTrue(next > 2 && prev > 2 && select > 2 && rank > 2,
                "word scans and popcounts beat bit-by-bit loops: " + next + " " + prev + " " + select + " " + rank);
    }

    @Test
    void probeAtomsAgainstAHashMap() {
        int n = 50_000;
        int[] index = new int[M3LinkedMap.Atoms.tableSize(n)];
        int[] hashes = new int[n];
        Object[] keys = new Object[n];
        Map<Integer, Integer> reference = new HashMap<>();
        for (int slot = 0; slot < n; slot++) {
            Integer key = slot * 7 + 3;
            keys[slot] = key;
            hashes[slot] = key.hashCode();
            M3LinkedMap.Atoms.insert(index, slot, hashes[slot]);
            reference.put(key, slot);
        }
        int[] probes = new Random(2).ints(OPS, 0, n).toArray();
        row("find(hit)", i -> M3LinkedMap.Atoms.find(index, hashes, keys, keys[probes[i]], hashes[probes[i]]),
                i -> reference.get(keys[probes[i]]));
        row("find(miss)", i -> M3LinkedMap.Atoms.find(index, hashes, keys, -probes[i] - 1, Integer.hashCode(-probes[i] - 1)),
                i -> reference.containsKey(-probes[i] - 1) ? 1 : -1);
        row("positionOf", i -> M3LinkedMap.Atoms.positionOf(index, hashes[probes[i]], probes[i]),
                i -> reference.get(keys[probes[i]]));
        int[] fresh = new int[M3LinkedMap.Atoms.tableSize(OPS)];
        Map<Object, Integer> table = new HashMap<>();
        row("insert", i -> {
            if (i == 0) {
                Arrays.fill(fresh, 0);
            }
            return M3LinkedMap.Atoms.insert(fresh, i, hashes[i]) ? 1 : 0;
        }, i -> {
            if (i == 0) {
                table.clear();
            }
            return table.put(keys[i], i) == null ? 0 : 1;
        });
        row("mix", i -> M3LinkedMap.Atoms.mix(i), i -> Integer.hashCode(i) ^ i >>> 16);
        row("tableSize", i -> M3LinkedMap.Atoms.tableSize(i), i -> {
            int size = 4;
            while (size < 2 * i) {
                size <<= 1;
            }
            return size;
        });
        assertTrue(sink != Long.MIN_VALUE);
    }

    @Test
    void keyAtomsAgainstTheirObjectsCounterparts() {
        Random random = new Random(4);
        Object[] keys = new Object[OPS];
        Object[] stored = new Object[OPS];
        for (int i = 0; i < OPS; i++) {
            int value = random.nextInt();
            keys[i] = i % 3 == 0 ? null : Integer.valueOf(value);
            stored[i] = i % 5 == 0 ? keys[i] : Integer.valueOf(i % 2 == 0 ? value : value + 1);
        }
        row("hash", i -> M3LinkedMap.Atoms.hash(keys[i]), i -> Objects.hashCode(keys[i]));
        row("sameKey", i -> M3LinkedMap.Atoms.sameKey(keys[i], stored[i]) ? 1 : 0,
                i -> Objects.equals(keys[i], stored[i]) ? 1 : 0);
        assertTrue(sink != Long.MIN_VALUE);
    }

    @Test
    void idAtomsAgainstABoxedHashMap() {
        int n = 50_000;
        int[] index = new int[M3LinkedMap.Atoms.tableSize(n)];
        long[] ids = new Random(5).longs(n).toArray();
        Map<Long, Integer> reference = new HashMap<>();
        for (int slot = 0; slot < n; slot++) {
            M3LinkedMap.Atoms.insert(index, slot, M3LinkedMap.Atoms.foldId(ids[slot]));
            reference.put(ids[slot], slot);
        }
        int[] probes = new Random(6).ints(OPS, 0, n).toArray();
        row("foldId", i -> M3LinkedMap.Atoms.foldId(ids[probes[i]]), i -> Long.valueOf(ids[probes[i]]).hashCode());
        row("findId(hit)", i -> M3LinkedMap.Atoms.findId(index, ids, ids[probes[i]],
                M3LinkedMap.Atoms.foldId(ids[probes[i]])), i -> reference.get(ids[probes[i]]));
        int used = 4096;
        Random random = new Random(8);
        boolean[] bits = new boolean[used];
        for (int i = 0; i < used; i++) {
            bits[i] = random.nextBoolean();
        }
        long[] live = pack(bits);
        Object[] values = new Object[used];
        long[] toIds = new long[used];
        Object[] toValues = new Object[used];
        long[] toLive = new long[live.length];
        row("compactIds(4096 slots)", i -> M3LinkedMap.Atoms.compactIds(ids, values, live, used, toIds, toValues,
                toLive), i -> {
                    List<Long> kept = new ArrayList<>();
                    for (int s = 0; s < used; s++) {
                        if (bits[s]) {
                            kept.add(ids[s]);
                        }
                    }
                    return kept.size();
                });
        assertTrue(sink != Long.MIN_VALUE);
    }

    @Test
    void compactAgainstAListFilter() {
        Random random = new Random(3);
        int used = 4096;
        boolean[] bits = new boolean[used];
        for (int i = 0; i < used; i++) {
            bits[i] = random.nextBoolean();
        }
        long[] live = pack(bits);
        Object[] keys = new Object[used];
        Object[] values = new Object[used];
        int[] hashes = new int[used];
        Object[] toKeys = new Object[used];
        Object[] toValues = new Object[used];
        int[] toHashes = new int[used];
        long[] toLive = new long[live.length];
        row("compact(4096 slots)", i -> M3LinkedMap.Atoms.compact(keys, values, hashes, live, used, toKeys, toValues,
                toHashes, toLive), i -> {
                    List<Object> kept = new ArrayList<>();
                    for (int s = 0; s < used; s++) {
                        if (bits[s]) {
                            kept.add(keys[s]);
                        }
                    }
                    return kept.size();
                });
    }

    @AfterAll
    static void writeReport() throws Exception {
        Path out = Path.of("target", "perf", "ARRAY_MAP_ATOMS_PERF.tsv");
        Files.createDirectories(out.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("atom\tm3_ns_op\treference_ns_op\tspeedup");
        lines.addAll(ROWS);
        Files.write(out, lines, StandardCharsets.UTF_8);
        lines.forEach(line -> System.out.println("ATOMPERF " + line));
    }
}
