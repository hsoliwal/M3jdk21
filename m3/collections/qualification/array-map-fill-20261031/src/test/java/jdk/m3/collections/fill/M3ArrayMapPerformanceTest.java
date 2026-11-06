/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.fill;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.m3.collections.apache.M3HashedMap;
import jdk.m3.collections.apache.M3LRUMap;
import jdk.m3.collections.apache.M3LinkedMap;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Performance of the ARRAY_MAP members against Apache Commons Collections 4.5.0: the same workloads on both, best
 * of several timed rounds after warm-up, nanoseconds and allocated bytes per operation, written to
 * {@code target/perf/ARRAY_MAP_PERF.tsv}. Only robust facts are asserted: the asymptotic win of index access
 * (rank/select over the live mask, O(n/64), against a list walk, O(n)) and no workload slower than the provider by
 * more than {@link #SLOWDOWN_LIMIT} (a regression guard, not a speed claim).
 */
@Tag("performance")
class M3ArrayMapPerformanceTest {

    private static final int N = 50_000;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 5;
    private static final double SLOWDOWN_LIMIT = 5.0;
    private static final double INDEX_SPEEDUP_FLOOR = 3.0;
    private static final List<String> ROWS = new ArrayList<>();

    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    /** One measured workload: setup builds a fresh map, body runs OPS operations on it. */
    private record Measure(double nsPerOp, double bytesPerOp) {
    }

    private static <M> Measure measure(Supplier<M> setup, Consumer<M> body, int ops) {
        long bestNanos = Long.MAX_VALUE;
        long bestBytes = Long.MAX_VALUE;
        for (int round = 0; round < WARMUP + ROUNDS; round++) {
            M map = setup.get();
            long bytes0 = THREADS.getCurrentThreadAllocatedBytes();
            long t0 = System.nanoTime();
            body.accept(map);
            long nanos = System.nanoTime() - t0;
            long bytes = THREADS.getCurrentThreadAllocatedBytes() - bytes0;
            if (round >= WARMUP) {
                bestNanos = Math.min(bestNanos, nanos);
                bestBytes = Math.min(bestBytes, bytes);
            }
        }
        return new Measure((double) bestNanos / ops, (double) bestBytes / ops);
    }

    private static double record(String member, String workload, Measure m3, Measure provider) {
        double ratio = m3.nsPerOp / Math.max(provider.nsPerOp, 1e-9);
        ROWS.add(String.format(java.util.Locale.ROOT, "%s\t%s\t%.1f\t%.1f\t%.2f\t%.1f\t%.1f", member, workload, m3.nsPerOp,
                provider.nsPerOp, ratio, m3.bytesPerOp, provider.bytesPerOp));
        return ratio;
    }

    private static List<Integer> keys(int n, long seed) {
        List<Integer> keys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            keys.add(i * 7 + 3);
        }
        Collections.shuffle(keys, new Random(seed));
        return keys;
    }

    private static <M extends Map<Integer, String>> M filled(Supplier<M> fresh, List<Integer> keys) {
        M map = fresh.get();
        for (Integer k : keys) {
            map.put(k, "v");
        }
        return map;
    }

    /** The Map workloads shared by every member; returns the worst slowdown ratio. */
    private static <A extends Map<Integer, String>, B extends Map<Integer, String>> double mapWorkloads(String member,
            Supplier<A> m3, Supplier<B> provider) {
        List<Integer> keys = keys(N, 1);
        List<Integer> probe = keys(N, 2);
        List<Integer> misses = new ArrayList<>();
        for (Integer k : probe) {
            misses.add(-k);
        }
        double worst = 0;
        worst = Math.max(worst, record(member, "put", measure(m3, m -> keys.forEach(k -> m.put(k, "v")), N),
                measure(provider, m -> keys.forEach(k -> m.put(k, "v")), N)));
        worst = Math.max(worst, record(member, "get-hit", measure(() -> filled(m3, keys), m -> probe.forEach(m::get), N),
                measure(() -> filled(provider, keys), m -> probe.forEach(m::get), N)));
        worst = Math.max(worst, record(member, "get-miss", measure(() -> filled(m3, keys), m -> misses.forEach(m::get), N),
                measure(() -> filled(provider, keys), m -> misses.forEach(m::get), N)));
        Consumer<Map<Integer, String>> iterate = m -> {
            long sum = 0;
            for (Map.Entry<Integer, String> e : m.entrySet()) {
                sum += e.getKey();
            }
            assertTrue(sum != 1);
        };
        worst = Math.max(worst, record(member, "iterate-entries", measure(() -> filled(m3, keys), iterate::accept, N),
                measure(() -> filled(provider, keys), iterate::accept, N)));
        Consumer<Map<Integer, String>> removeHalf = m -> {
            for (int i = 0; i < N; i += 2) {
                m.remove(probe.get(i));
            }
        };
        worst = Math.max(worst, record(member, "remove-half", measure(() -> filled(m3, keys), removeHalf::accept, N / 2),
                measure(() -> filled(provider, keys), removeHalf::accept, N / 2)));
        Consumer<Map<Integer, String>> churn = m -> {
            for (int i = 0; i < N; i++) {
                Integer k = probe.get(i);
                m.remove(k);
                m.put(k, "w");
            }
        };
        worst = Math.max(worst, record(member, "remove-put-churn", measure(() -> filled(m3, keys), churn::accept, 2 * N),
                measure(() -> filled(provider, keys), churn::accept, 2 * N)));
        return worst;
    }

    @Test
    void hashedMapPerformance() {
        double worst = mapWorkloads("M3HashedMap", M3HashedMap::new, HashedMap::new);
        assertTrue(worst <= SLOWDOWN_LIMIT, "M3HashedMap is slower than the provider by " + worst + "x on a workload");
    }

    @Test
    void linkedMapPerformanceAndTheIndexAccessWin() {
        double worst = mapWorkloads("M3LinkedMap", M3LinkedMap::new, LinkedMap::new);
        List<Integer> keys = keys(N, 1);
        int[] positions = new Random(3).ints(2_000, 0, N).toArray();
        Measure m3Get = measure(() -> filled(M3LinkedMap<Integer, String>::new, keys), m -> {
            for (int p : positions) {
                m.get(p);
            }
        }, positions.length);
        Measure providerGet = measure(() -> filled(LinkedMap<Integer, String>::new, keys), m -> {
            for (int p : positions) {
                m.get(p);
            }
        }, positions.length);
        double getRatio = record("M3LinkedMap", "get(int)", m3Get, providerGet);
        Measure m3IndexOf = measure(() -> filled(M3LinkedMap<Integer, String>::new, keys), m -> {
            for (int p : positions) {
                m.indexOf(keys.get(p));
            }
        }, positions.length);
        Measure providerIndexOf = measure(() -> filled(LinkedMap<Integer, String>::new, keys), m -> {
            for (int p : positions) {
                m.indexOf(keys.get(p));
            }
        }, positions.length);
        double indexOfRatio = record("M3LinkedMap", "indexOf", m3IndexOf, providerIndexOf);
        assertTrue(worst <= SLOWDOWN_LIMIT, "M3LinkedMap is slower than the provider by " + worst + "x on a workload");
        assertTrue(1 / getRatio >= INDEX_SPEEDUP_FLOOR, "get(int) speed-up " + 1 / getRatio + "x below " + INDEX_SPEEDUP_FLOOR);
        assertTrue(1 / indexOfRatio >= INDEX_SPEEDUP_FLOOR, "indexOf speed-up " + 1 / indexOfRatio + "x below "
                + INDEX_SPEEDUP_FLOOR);
    }

    @Test
    void lruMapPerformance() {
        int max = 10_000;
        double worst = mapWorkloads("M3LRUMap", () -> new M3LRUMap<>(max), () -> new LRUMap<>(max));
        int[] hot = new Random(4).ints(4 * N, 0, 3 * max).toArray(); // a working set three times the bound: evictions
        Consumer<Map<Integer, String>> access = m -> {
            for (int k : hot) {
                if (m.get(k) == null) {
                    m.put(k, "v");
                }
            }
        };
        worst = Math.max(worst, record("M3LRUMap", "get-or-put-evicting", measure(() -> new M3LRUMap<Integer, String>(max),
                access::accept, hot.length), measure(() -> new LRUMap<Integer, String>(max), access::accept, hot.length)));
        assertTrue(worst <= SLOWDOWN_LIMIT, "M3LRUMap is slower than the provider by " + worst + "x on a workload");
    }

    @org.junit.jupiter.api.AfterAll
    static void writeReport() throws Exception {
        Path out = Path.of("target", "perf", "ARRAY_MAP_PERF.tsv");
        Files.createDirectories(out.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("member\tworkload\tm3_ns_op\tprovider_ns_op\tratio_m3_over_provider\tm3_bytes_op\tprovider_bytes_op");
        lines.addAll(ROWS);
        Files.write(out, lines, StandardCharsets.UTF_8);
        for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
            System.out.println("PERF " + it.next());
        }
    }
}
