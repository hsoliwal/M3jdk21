// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Small evidence probe, not JMH. Compare stock Java21 to Java21 --patch-module. */
public final class M3HashMapBeforeAfterBench {
    private static volatile Object escape;
    private static final int ROUNDS = 7;
    private static final int WARM = 5;

    private record Case(String shape, Map<Integer, Integer> source, int iterations) { }

    public static void main(String[] args) {
        String variant = System.getProperty("m3.variant", "unknown");
        Map<Integer, Integer> five = makeMap(5);
        Map<Integer, Integer> big = makeMap(150);
        Map<Integer, Integer> unmodifiable = Collections.unmodifiableMap(big);
        Map<Integer, Integer> linked = new LinkedHashMap<>(big);
        Case[] workload = new Case[] {
                new Case("hashmap5", five, 20000),
                new Case("hashmap150", big, 3000),
                new Case("wrapped150", unmodifiable, 3000),
                new Case("linked150-fallback", linked, 3000)
        };
        for (Case data : workload) {
            for (String operation : new String[]{"copyConstructor", "putAll"}) {
                measure(variant, data, operation);
            }
        }
    }

    private static HashMap<Integer, Integer> makeMap(int size) {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) map.put(i * 37 + 1, i + 1);
        return map;
    }

    private static long execute(Case data, String operation) {
        long checksum = 0;
        if ("copyConstructor".equals(operation)) {
            for (int j = 0; j < data.iterations; j++) {
                HashMap<Integer, Integer> target = new HashMap<>(data.source);
                checksum += target.size();
                escape = target;
            }
        } else {
            HashMap<Integer, Integer> target = new HashMap<>();
            for (int j = 0; j < data.iterations; j++) {
                target.clear();
                target.putAll(data.source);
                checksum += target.size();
                escape = target;
            }
        }
        return checksum;
    }

    private static void measure(String variant, Case data, String operation) {
        long expected = (long) data.iterations * data.source.size();
        for (int j = 0; j < WARM; j++) {
            if (execute(data, operation) != expected) {
                throw new AssertionError("warmup mismatch");
            }
        }
        long[] raw = new long[ROUNDS];
        for (int j = 0; j < ROUNDS; j++) {
            long t0 = System.nanoTime();
            long check = execute(data, operation);
            raw[j] = System.nanoTime() - t0;
            if (check != expected) throw new AssertionError("measurement mismatch");
        }
        long[] sorted = raw.clone();
        Arrays.sort(sorted);
        double median = (double) sorted[ROUNDS / 2] / data.iterations;
        System.out.printf(Locale.ROOT,
                "M3_HASHMAP_SAMPLE variant=%s shape=%s op=%s ns_per_op=%.3f"
                + " map_size=%d iterations=%d samples=%d%n",
                variant, data.shape, operation, median, data.source.size(),
                data.iterations, ROUNDS);
        System.out.println("M3_HASHMAP_SAMPLE_RAW variant=" + variant
                + " shape=" + data.shape + " op=" + operation
                + " nanos=" + Arrays.toString(raw));
    }
}
