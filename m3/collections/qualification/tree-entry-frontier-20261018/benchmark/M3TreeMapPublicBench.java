package com.m3.bench;

import com.m3.collections.M3TreeMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Original finite public-API workload; no third-party benchmark code copied.
 * Scores are ns per WHOLE invocation. Normalize externally using BENCHMARK_PLAN.
 * JVM heap/temp/CPU flags belong to the bounded root launcher, not annotations.
 */
@State(Scope.Thread)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS, batchSize = 1)
@Measurement(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS, batchSize = 1)
@Fork(2)
public class M3TreeMapPublicBench {
    @Param({"4096", "65536"})
    public int size;

    @Param({"ascending", "descending", "seededshuffle"})
    public String order;

    private Integer[] keys;
    private Integer[] values;
    private int[] traversal;
    private M3TreeMap<Integer, Integer> prebuilt;
    private long expectedChecksum;

    @Setup(Level.Trial)
    public void setup() {
        if (size != 4096 && size != 65536) {
            throw new IllegalArgumentException("Size is outside the frozen plan");
        }
        keys = new Integer[size];
        values = new Integer[size];
        traversal = new int[size];
        expectedChecksum = 0;
        for (int i = 0; i < size; i++) {
            // Boxing happens once, outside measured methods.
            keys[i] = Integer.valueOf(i);
            values[i] = Integer.valueOf(i ^ 0x5A5A5A5A);
            traversal[i] = i;
            expectedChecksum += pairBits(keys[i], values[i]);
        }
        if ("descending".equals(order)) {
            for (int i = 0; i < size / 2; i++) {
                int j = size - 1 - i;
                int swap = traversal[i];
                traversal[i] = traversal[j];
                traversal[j] = swap;
            }
        } else if ("seededshuffle".equals(order)) {
            Random random = new Random(0x4d33545245454cL ^ size);
            for (int i = size - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swap = traversal[i];
                traversal[i] = traversal[j];
                traversal[j] = swap;
            }
        } else if (!"ascending".equals(order)) {
            throw new IllegalArgumentException("Order is outside the frozen plan");
        }

        // Preserve the insertion-order-built scan state; probe churn on another map.
        prebuilt = buildMap();
        validate(prebuilt);
        M3TreeMap<Integer, Integer> probe = buildMap();
        if (churn(probe) != expectedChecksum) {
            throw new IllegalStateException("Trial churn returned incorrect mappings");
        }
        validate(probe);
    }

    private M3TreeMap<Integer, Integer> buildMap() {
        M3TreeMap<Integer, Integer> result = new M3TreeMap<>();
        for (int i = 0; i < size; i++) {
            int slot = traversal[i];
            result.put(keys[slot], values[slot]);
        }
        return result;
    }

    private static long pairBits(Integer key, Integer value) {
        return ((long) key.intValue() << 32) ^ (value.longValue() & 0xffff_ffffL);
    }

    private static long scan(M3TreeMap<Integer, Integer> map) {
        long checksum = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            checksum += pairBits(entry.getKey(), entry.getValue());
        }
        return checksum;
    }

    private long churn(M3TreeMap<Integer, Integer> map) {
        long checksum = 0;
        for (int i = 0; i < size; i++) {
            Integer key = keys[traversal[i]];
            Integer removed = map.remove(key);
            checksum += pairBits(key, removed);
            map.put(key, removed);
        }
        return checksum;
    }

    private void validate(M3TreeMap<Integer, Integer> map) {
        if (map.size() != size) {
            throw new IllegalStateException("Trial map size differs");
        }
        int next = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (next >= size || entry.getKey().intValue() != next
                    || entry.getValue().intValue() != values[next].intValue()) {
                throw new IllegalStateException("Trial ordered content differs at " + next);
            }
            next++;
        }
        if (next != size || scan(map) != expectedChecksum) {
            throw new IllegalStateException("Trial traversal/checksum differs");
        }
        for (int i = 0; i < size; i++) {
            Integer found = map.get(keys[i]);
            if (found == null || found.intValue() != values[i].intValue()) {
                throw new IllegalStateException("Trial lookup differs at " + i);
            }
        }
    }

    @Benchmark
    public long entrySetFullScan() {
        return scan(prebuilt);
    }

    @Benchmark
    public int freshInsertion(Blackhole blackhole) {
        M3TreeMap<Integer, Integer> result = buildMap();
        blackhole.consume(result);
        return result.size();
    }

    @Benchmark
    public long removeReinsertChurn() {
        return churn(prebuilt);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        validate(prebuilt);
    }
}
