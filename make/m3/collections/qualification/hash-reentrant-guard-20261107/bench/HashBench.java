/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import jdk.m3.collections.M3HashMap;

/**
 * Paired get/put microbench of M3HashMap, base vs head, in one JVM (qualification/hash-reentrant-guard-20261107).
 * main loads this class twice, through one class loader over the base classes and one over the head classes, and
 * alternates timed rounds between the two copies (the order flips every round), so drift and JIT state hit both
 * sides alike; the head/base ratio of each round pair is the measurement. Keys are prebuilt Integer and String
 * objects, 16384 mappings; operations, each timed over every key per repetition: get-hit, get-miss, put-overwrite
 * (existing key), put-fill (insert all keys into a fresh map). Prints per (key type, operation): median ns/op of
 * each side, and the median, 10th and 90th percentile of the per-round head/base ratios.
 * usage: java -cp <this dir> HashBench <base classpath> <head classpath> <warmup rounds> <measured rounds>
 */
public final class HashBench {
    private static final int SIZE = 1 << 14;
    private static final int REPETITIONS = 32;
    private static final String[] TYPES = {"Integer", "String"};
    private static final String[] OPS = {"get-hit", "get-miss", "put-overwrite", "put-fill"};
    private static long sink;

    private HashBench() { }

    /** Keys and maps of one class loader's copy; touched only by round, never by the driver's own copy. */
    private static final class Data {
        static final Object[][] KEYS = new Object[2][SIZE];
        static final Object[][] MISSES = new Object[2][SIZE];
        static final Object[] MAPS = new Object[2];

        static {
            for (int i = 0; i < SIZE; i++) {
                KEYS[0][i] = Integer.valueOf(i * 7 + 1_000_000);
                MISSES[0][i] = Integer.valueOf(-i - 1);
                KEYS[1][i] = "key-" + i;
                MISSES[1][i] = "miss-" + i;
            }
            for (int type = 0; type < 2; type++) {
                M3HashMap<Object, Object> map = new M3HashMap<>();
                for (Object key : KEYS[type]) map.put(key, key);
                MAPS[type] = map;
            }
        }

        private Data() { }

        @SuppressWarnings("unchecked")
        static M3HashMap<Object, Object> map(int type) { return (M3HashMap<Object, Object>) MAPS[type]; }

        static long body(int type, int op) {
            M3HashMap<Object, Object> map = map(type);
            long count = 0;
            switch (op) {
                case 0 -> { for (Object key : KEYS[type]) if (map.get(key) != null) count++; }
                case 1 -> { for (Object key : MISSES[type]) if (map.get(key) != null) count++; }
                case 2 -> { for (Object key : KEYS[type]) if (map.put(key, key) != null) count++; }
                default -> {
                    M3HashMap<Object, Object> fresh = new M3HashMap<>();
                    for (Object key : KEYS[type]) fresh.put(key, key);
                    count = fresh.size();
                }
            }
            return count;
        }
    }

    /** One timed round in this class loader's copy: elapsed nanoseconds of REPETITIONS passes over every key. */
    public static long round(int type, int op) {
        long start = System.nanoTime();
        for (int repetition = 0; repetition < REPETITIONS; repetition++) sink += Data.body(type, op);
        return System.nanoTime() - start;
    }

    private static Method side(URL self, String classpath) throws ReflectiveOperationException, java.io.IOException {
        String[] parts = classpath.split(File.pathSeparator);
        URL[] urls = new URL[parts.length + 1];
        urls[0] = self;
        for (int i = 0; i < parts.length; i++) urls[i + 1] = Path.of(parts[i]).toUri().toURL();
        @SuppressWarnings("resource") // the loaders live as long as this JVM
        URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
        return loader.loadClass("HashBench").getMethod("round", int.class, int.class);
    }

    private static double quantile(double[] sorted, double q) { return sorted[(int) Math.min(sorted.length - 1, q * sorted.length)]; }

    public static void main(String[] args) throws Exception {
        URL self = HashBench.class.getProtectionDomain().getCodeSource().getLocation();
        Method[] sides = {side(self, args[0]), side(self, args[1])};
        int warmup = Integer.parseInt(args[2]);
        int rounds = Integer.parseInt(args[3]);
        double perOp = (double) REPETITIONS * SIZE;
        double[][][] nanos = new double[2][TYPES.length * OPS.length][rounds];
        double[][] ratios = new double[TYPES.length * OPS.length][rounds];
        for (int round = -warmup; round < rounds; round++) {
            for (int type = 0; type < TYPES.length; type++) {
                for (int op = 0; op < OPS.length; op++) {
                    long[] elapsed = new long[2];
                    for (int turn = 0; turn < 2; turn++) {
                        int which = (round & 1) == 0 ? turn : 1 - turn;
                        elapsed[which] = (Long) sides[which].invoke(null, type, op);
                    }
                    if (round < 0) continue;
                    int cell = type * OPS.length + op;
                    nanos[0][cell][round] = elapsed[0] / perOp;
                    nanos[1][cell][round] = elapsed[1] / perOp;
                    ratios[cell][round] = (double) elapsed[1] / elapsed[0];
                }
            }
        }
        for (int cell = 0; cell < ratios.length; cell++) {
            Arrays.sort(nanos[0][cell]);
            Arrays.sort(nanos[1][cell]);
            Arrays.sort(ratios[cell]);
            System.out.printf(Locale.ROOT, "%s\t%s\tbase=%.3f\thead=%.3f\tratio=%.4f\tp10=%.4f\tp90=%.4f%n",
                    TYPES[cell / OPS.length], OPS[cell % OPS.length], quantile(nanos[0][cell], 0.5),
                    quantile(nanos[1][cell], 0.5), quantile(ratios[cell], 0.5), quantile(ratios[cell], 0.1),
                    quantile(ratios[cell], 0.9));
        }
        if (sink == 42) System.out.println();
    }
}
