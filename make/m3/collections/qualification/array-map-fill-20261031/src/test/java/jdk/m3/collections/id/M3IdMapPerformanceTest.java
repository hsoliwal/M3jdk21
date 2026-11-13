/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.id;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The M3 id map against {@code LinkedHashMap<Long, Object>} (its oracle) and {@code HashMap<Long, Object>} on the same
 * workloads: ns/op and allocated bytes/op, best of 5 rounds after 3 warm-up rounds, written to
 * {@code target/perf/ID_MAP_PERF.tsv}. Asserted: no boxing (zero bytes per lookup) and the positional access win; the
 * rest is a report.
 */
@Tag("performance")
class M3IdMapPerformanceTest {

    private static final int N = 200_000;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 5;
    private static final int POSITIONS = 2_000;
    private static final List<String> ROWS = new ArrayList<>();
    private static final Object VALUE = new Object();
    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static long sink;

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

    private static void record(String workload, Measure m3, Measure linked, Measure hashed) {
        ROWS.add(String.format(Locale.ROOT, "%s\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f", workload, m3.nsPerOp,
                linked.nsPerOp, hashed.nsPerOp, m3.bytesPerOp, linked.bytesPerOp, hashed.bytesPerOp));
    }

    private static final long[] IDS = new Random(1).longs(N).toArray();
    private static final long[] MISSES = new Random(2).longs(N).toArray();

    private static M3LongObjectLinkedHashMap<Object> filledM3() {
        M3LongObjectLinkedHashMap<Object> map = new M3LongObjectLinkedHashMap<>();
        for (long id : IDS) {
            map.put(id, VALUE);
        }
        return map;
    }

    private static <M extends Map<Long, Object>> M filled(Supplier<M> fresh) {
        M map = fresh.get();
        for (long id : IDS) {
            map.put(id, VALUE);
        }
        return map;
    }

    @Test
    void putGetMissChurnAndIteration() {
        record("put", measure(M3LongObjectLinkedHashMap::new, m -> {
            for (long id : IDS) {
                m.put(id, VALUE);
            }
        }, N), measure(LinkedHashMap<Long, Object>::new, m -> {
            for (long id : IDS) {
                m.put(id, VALUE);
            }
        }, N), measure(HashMap<Long, Object>::new, m -> {
            for (long id : IDS) {
                m.put(id, VALUE);
            }
        }, N));
        Measure m3Get = measure(M3IdMapPerformanceTest::filledM3, m -> {
            for (long id : IDS) {
                sink += m.get(id) == null ? 0 : 1;
            }
        }, N);
        record("get (hit)", m3Get, measure(() -> filled(LinkedHashMap::new), m -> {
            for (long id : IDS) {
                sink += m.get(id) == null ? 0 : 1;
            }
        }, N), measure(() -> filled(HashMap::new), m -> {
            for (long id : IDS) {
                sink += m.get(id) == null ? 0 : 1;
            }
        }, N));
        record("get (miss)", measure(M3IdMapPerformanceTest::filledM3, m -> {
            for (long id : MISSES) {
                sink += m.containsKey(id) ? 1 : 0;
            }
        }, N), measure(() -> filled(LinkedHashMap::new), m -> {
            for (long id : MISSES) {
                sink += m.containsKey(id) ? 1 : 0;
            }
        }, N), measure(() -> filled(HashMap::new), m -> {
            for (long id : MISSES) {
                sink += m.containsKey(id) ? 1 : 0;
            }
        }, N));
        record("remove-put churn", measure(M3IdMapPerformanceTest::filledM3, m -> {
            for (long id : IDS) {
                m.remove(id);
                m.put(id, VALUE);
            }
        }, N), measure(() -> filled(LinkedHashMap::new), m -> {
            for (long id : IDS) {
                m.remove(id);
                m.put(id, VALUE);
            }
        }, N), measure(() -> filled(HashMap::new), m -> {
            for (long id : IDS) {
                m.remove(id);
                m.put(id, VALUE);
            }
        }, N));
        record("iteration", measure(M3IdMapPerformanceTest::filledM3, m -> m.forEach((id, value) -> sink += id), N),
                measure(() -> filled(LinkedHashMap::new), m -> m.forEach((id, value) -> sink += id), N),
                measure(() -> filled(HashMap::new), m -> m.forEach((id, value) -> sink += id), N));
        assertTrue(m3Get.bytesPerOp < 1.0, "a lookup by id boxes nothing: " + m3Get.bytesPerOp + " bytes/op");
    }

    @Test
    void positionalAccessByRankAndSelect() {
        Random random = new Random(3);
        int[] positions = random.ints(POSITIONS, 0, N).toArray();
        Measure m3 = measure(M3IdMapPerformanceTest::filledM3, m -> {
            for (int p : positions) {
                sink += m.idAt(p);
            }
        }, POSITIONS);
        Measure linked = measure(() -> filled(LinkedHashMap::new), m -> {
            for (int p : positions) {
                Iterator<Long> it = m.keySet().iterator();
                for (int i = 0; i < p; i++) {
                    it.next();
                }
                sink += it.next();
            }
        }, POSITIONS);
        record("idAt (rank/select vs iterator walk)", m3, linked, linked);
        assertTrue(linked.nsPerOp > 10 * m3.nsPerOp, "select by popcount beats a walk: " + m3.nsPerOp + " vs "
                + linked.nsPerOp);
    }

    @AfterAll
    static void writeReport() throws Exception {
        Path out = Path.of("target", "perf", "ID_MAP_PERF.tsv");
        Files.createDirectories(out.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("workload\tm3_ns_op\tlinked_ns_op\thashed_ns_op\tm3_bytes_op\tlinked_bytes_op\thashed_bytes_op");
        lines.addAll(ROWS);
        Files.write(out, lines, StandardCharsets.UTF_8);
        lines.forEach(line -> System.out.println("IDPERF " + line));
        assertTrue(sink != Long.MIN_VALUE);
    }
}
