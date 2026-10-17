// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import com.m3.collections.M3ProgressMonitor;
import com.m3.collections.M3ProgressState;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongBinaryOperator;

/** Frozen standalone behavioral corpus; invoked by the Maven verify lifecycle. */
public final class FlowV3Proof {
    private static long assertions, windowCases;
    private static int groups;
    private static boolean nativeEnabled;
    private FlowV3Proof() { }

    private static void require(boolean condition, String signature) {
        assertions++;
        if (!condition) throw new AssertionError(signature);
    }
    private static void same(Object expected, Object actual, String signature) {
        require(Objects.equals(expected, actual), signature + " expected=" + expected + " actual=" + actual);
    }
    private static void array(long[] expected, long[] actual, String signature) {
        require(Arrays.equals(expected, actual), signature + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }
    private static Throwable thrown(Runnable action) {
        try { action.run(); }
        catch (Throwable failure) { assertions++; return failure; }
        throw new AssertionError("EXPECTED_FAILURE");
    }
    private static void type(Class<? extends Throwable> type, Runnable action, String signature) {
        require(type.isInstance(thrown(action)), signature);
    }
    private static void group(String name, Runnable action) {
        action.run(); groups++; System.out.println("PASS " + name);
    }
    private static long[] oracle(long[] values, int width, int step, M3Windows.Tail tail, int kind) {
        var output = new ArrayList<Long>();
        for (long start = 0; start < values.length; start += step) {
            long end = Math.min(values.length, start + width);
            if (tail == M3Windows.Tail.DROP && end - start < width) break;
            long state = kind == 0 ? 0 : kind == 1 ? Long.MAX_VALUE : Long.MIN_VALUE;
            for (long i = start; i < end; i++) {
                long v = values[(int) i];
                state = kind == 0 ? state + v : kind == 1 ? Math.min(state, v) : Math.max(state, v);
            }
            output.add(state);
        }
        return output.stream().mapToLong(Long::longValue).toArray();
    }
    private static M3LongFlow windows(long[] v, int width, int step, M3Windows.Tail tail, int kind) {
        var source = M3LongFlow.of(v);
        return kind == 0 ? source.windowSum(width, step, tail)
                : kind == 1 ? source.windowMin(width, step, tail) : source.windowMax(width, step, tail);
    }
    private static void compare(long[] values, int width, int step, M3Windows.Tail tail) {
        for (int kind = 0; kind < 3; kind++) {
            array(oracle(values, width, step, tail, kind), windows(values, width, step, tail, kind).toArray(), "WINDOW_ORACLE");
            windowCases++;
        }
    }

    private static void cleanupWitness() {
        RuntimeException original = new RuntimeException("same failure instance");
        var actual = thrown(() -> M3LongFlow.of(new long[]{1}).accumulate(
                () -> new long[1], (state, v) -> { throw original; }, state -> state[0],
                state -> { throw original; }).sum());
        require(actual == original, "SCOPED_PRIMARY_IDENTITY_ON_SELF_SUPPRESSION");
    }

    private static final class Monitor implements M3ProgressMonitor {
        long worked, cancelAt = Long.MAX_VALUE;
        boolean canceled;
        RuntimeException failure;
        @Override public void beginTask(String name, long total) { }
        @Override public void done() { }
        @Override public void worked(long amount) {
            worked += amount;
            if (worked >= cancelAt) canceled = true;
            if (failure != null) throw failure;
        }
        @Override public void setProgress(long value, long total) { worked = value; }
        @Override public void setTaskName(String name) { }
        @Override public void subTask(String name) { }
        @Override public boolean isCanceled() { return canceled; }
        @Override public void setCanceled(boolean value) { canceled = value; }
        @Override public M3ProgressState state() { return new M3ProgressState("v3-proof", "", worked, UNKNOWN, canceled, false, 0, 0); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--cleanup-witness")) { cleanupWitness(); return; }
        if (args.length > 0) { System.load(args[0]); nativeEnabled = true; }
        if (args.length > 1 && args[1].equals("--missing-symbol")) {
            type(UnsatisfiedLinkError.class, () -> M3NativeLongs.windowSums(new long[]{1}, 1, 1,
                    M3Windows.Tail.DROP, M3NativeLongs.Backend.NATIVE, null), "MISSING_NEW_NATIVE_SYMBOL");
            System.out.println("MISSING_SYMBOL_REFUSAL=PASS"); return;
        }
        group("empty, single-element, extreme widths and tail boundaries", () -> {
            long[][] sources = { {}, {0}, {Long.MIN_VALUE}, {Long.MAX_VALUE, 1, -1, Long.MIN_VALUE}, {2, 3, 5, 7} };
            for (var source : sources) for (int width : new int[]{1, 2, 3, 5, Integer.MAX_VALUE})
                for (int step : new int[]{1, 2, 7, Integer.MAX_VALUE}) for (var tail : M3Windows.Tail.values()) compare(source, width, step, tail);
            array(new long[]{10, 12}, M3LongFlow.of(new long[]{2, 3, 5, 7})
                    .windowSum(3, 2, M3Windows.Tail.PARTIAL).toArray(), "TAIL_SUM_INCLUDES_NEXT_START");
        });
        group("frozen exhaustive short arrays", () -> {
            for (int length = 0; length <= 5; length++) {
                int combinations = (int) Math.pow(3, length);
                for (int code = 0; code < combinations; code++) {
                    long[] v = new long[length]; int k = code;
                    for (int i = 0; i < length; i++) { v[i] = k % 3 - 1; k /= 3; }
                    for (int width = 1; width <= 7; width++) for (int step = 1; step <= 8; step++)
                        for (var tail : M3Windows.Tail.values()) compare(v, width, step, tail);
                }
            }
        });
        group("1200 seeded rolling sum/min/max comparisons", () -> {
            var random = new Random(0x4d335633L);
            for (int test = 0; test < 1200; test++) {
                long[] v = new long[random.nextInt(300)];
                for (int i = 0; i < v.length; i++) v[i] = random.nextBoolean() ? random.nextLong() : random.nextInt(11) - 5;
                int width = 1 + random.nextInt(340), step = 1 + random.nextInt(370);
                for (var tail : M3Windows.Tail.values()) compare(v, width, step, tail);
            }
        });
        group("growth and wrap boundaries on monotone and equal inputs", () -> {
            for (int n : new int[]{8, 9, 16, 17, 1023, 1024, 1025, 4097}) {
                long[] ascending = new long[n], descending = new long[n], equal = new long[n];
                for (int i = 0; i < n; i++) { ascending[i] = i - 1000L; descending[i] = -ascending[i]; equal[i] = -7; }
                for (int width : new int[]{1, 8, 9, 17, 127, 1024}) for (var tail : M3Windows.Tail.values()) {
                    compare(ascending, width, 3, tail); compare(descending, width, 5, tail); compare(equal, width, 7, tail);
                }
            }
        });
        group("upstream selection, scans and short-circuit continuation", () -> {
            var reads = new AtomicInteger();
            var plan = M3LongFlow.of(new long[]{1, 2, 3, 4, 5, 6})
                    .mapLong(v -> { reads.incrementAndGet(); return v; })
                    .windowSum(3, 1, M3Windows.Tail.PARTIAL).limit(1);
            same(0, reads.get(), "ASSEMBLY_LAZY"); array(new long[]{6}, plan.toArray(), "SHORT_RESULT");
            same(3, reads.get(), "SHORT_READ_COUNT");
            array(new long[]{6}, plan.toArray(), "REPEATED_TERMINAL"); same(6, reads.get(), "FRESH_STATE");
            array(new long[]{9, 16}, M3LongFlow.of(new long[]{1, 2, 3, 4}).scan(0, Long::sum)
                    .windowSum(2, 1, M3Windows.Tail.DROP).skip(1).toArray(), "SCAN_WINDOW_COMPOSITION");
            array(new long[]{3, 2}, M3LongFlow.of(new long[]{1, 2, 3, 4}).takeWhile(v -> v < 3)
                    .windowSum(3, 1, M3Windows.Tail.PARTIAL).toArray(), "UPSTREAM_STOP_FLUSHES_PREFIX");
            array(new long[]{7, 4}, M3LongFlow.of(new long[]{1, 2, 3, 4}).dropWhile(v -> v < 3)
                    .windowSum(3, 1, M3Windows.Tail.PARTIAL).toArray(), "DROP_CONTINUATION");
        });
        group("metadata-only source never materializes elements", () -> {
            var materialized = new AtomicInteger();
            long[] actual = M3Flow.<Object>indexed(() -> 8, i -> { materialized.incrementAndGet(); throw new AssertionError("UNNECESSARY_OBJECT"); })
                    .whereIndex(i -> (i & 1) == 0).mapIndexToLong(i -> i)
                    .windowMax(2, 1, M3Windows.Tail.PARTIAL).toArray();
            array(new long[]{2, 4, 6, 6}, actual, "METADATA_WINDOWS"); same(0, materialized.get(), "NO_PAYLOAD_LOADS");
        });
        group("single-final-output accumulators remain intermediate", () -> {
            var calls = new AtomicInteger();
            var p = M3LongFlow.of(new long[]{2, 3, 4}).accumulate(20, (a, b) -> { calls.incrementAndGet(); return a - b; })
                    .mapLong(v -> v * 2);
            same(0, calls.get(), "FOLD_LAZY"); array(new long[]{22}, p.toArray(), "ORDERED_NONASSOCIATIVE_FOLD");
            same(3, calls.get(), "FOLD_CALL_COUNT");
            array(new long[]{9}, M3LongFlow.of(new long[0]).accumulate(9, Long::sum).toArray(), "EMPTY_IDENTITY");
            same(11L, M3LongFlow.of(new long[]{2, 3, 4}).fold(20, (a, b) -> a - b), "FOLD_TERMINAL");
        });
        group("scoped release runs exactly once before emission", () -> {
            var order = new ArrayList<String>();
            var p = M3LongFlow.of(new long[]{1, 2, 3}).accumulate(
                    () -> { order.add("create"); return new long[1]; },
                    (s, v) -> { order.add("add"); s[0] += v; },
                    s -> { order.add("finish"); return s[0]; },
                    s -> { order.add("release"); s[0] = -1; });
            same(List.of(), order, "SCOPED_LAZY");
            p.forEach(v -> { same(6L, v, "SCOPED_VALUE"); order.add("emit"); });
            same(List.of("create", "add", "add", "add", "finish", "release", "emit"), order, "SCOPED_PHASE_ORDER");
            order.clear(); p.limit(0).forEach(v -> { throw new AssertionError("ZERO_LIMIT"); });
            same(List.of(), order, "NO_SCOPED_WORK_AFTER_ZERO_LIMIT");
        });
        group("scoped failure identity and cleanup suppression", () -> {
            var primary = new IllegalArgumentException("update failure");
            var cleanup = new IllegalStateException("cleanup failure");
            var actual = thrown(() -> M3LongFlow.of(new long[]{1}).accumulate(() -> new long[1],
                    (s, v) -> { throw primary; }, s -> { throw new AssertionError("FINISH_AFTER_FAILURE"); },
                    s -> { throw cleanup; }).sum());
            require(actual == primary, "PRIMARY_FAILURE_IDENTITY");
            require(primary.getSuppressed().length == 1 && primary.getSuppressed()[0] == cleanup, "SUPPRESSED_CLEANUP");
            cleanupWitness();
            var releases = new AtomicInteger();
            var finish = new RuntimeException("finish");
            actual = thrown(() -> M3LongFlow.of(new long[]{1}).accumulate(() -> new long[1],
                    (s, v) -> s[0] += v, s -> { throw finish; }, s -> releases.incrementAndGet()).sum());
            require(actual == finish, "FINISH_FAILURE_IDENTITY"); same(1, releases.get(), "FINISH_RELEASE");
            actual = thrown(() -> M3LongFlow.of(new long[]{1}).accumulate(() -> new long[1],
                    (s, v) -> s[0] += v, s -> s[0], s -> { throw cleanup; }).sum());
            require(actual == cleanup, "CLEANUP_ONLY_FAILURE");
        });
        group("factory refusal, cancellation cleanup and pre-interruption", () -> {
            var count = new AtomicInteger();
            type(NullPointerException.class, () -> M3LongFlow.of(new long[]{1}).accumulate(
                    () -> null, (Object s, long v) -> { }, s -> 0, s -> count.incrementAndGet()).sum(), "NULL_STATE");
            same(0, count.get(), "NO_NULL_STATE_RELEASE");
            var m = new Monitor();
            type(CancellationException.class, () -> M3LongFlow.of(new long[]{1, 2}).accumulate(
                    () -> new long[1], (s, v) -> m.canceled = true, s -> 0, s -> count.incrementAndGet()).sum(m), "CANCEL_SCOPE");
            same(1, count.get(), "CANCEL_RELEASE");
            Thread.currentThread().interrupt();
            try {
                type(CancellationException.class, () -> M3LongFlow.of(new long[]{1}).windowMax(1, 1, M3Windows.Tail.DROP).sum(), "PRE_INTERRUPT");
                require(Thread.currentThread().isInterrupted(), "INTERRUPT_PRESERVED");
            } finally { Thread.interrupted(); }
        });
        group("matching terminals and summaries agree with JDK", () -> {
            long[][] inputs = { {}, {0}, {-3, -2, -1}, {Long.MAX_VALUE, 1, Long.MIN_VALUE} };
            for (var v : inputs) {
                same(Arrays.stream(v).anyMatch(x -> x > 0), M3LongFlow.of(v).anyMatch(x -> x > 0), "ANY_MATCH");
                same(Arrays.stream(v).allMatch(x -> x > 0), M3LongFlow.of(v).allMatch(x -> x > 0), "ALL_MATCH");
                same(Arrays.stream(v).noneMatch(x -> x > 0), M3LongFlow.of(v).noneMatch(x -> x > 0), "NONE_MATCH");
                same(Arrays.stream(v).findFirst(), M3LongFlow.of(v).findFirst(), "FIND_FIRST");
                LongSummaryStatistics expected = Arrays.stream(v).summaryStatistics();
                M3LongSummary actual = M3LongFlow.of(v).summary();
                same(new M3LongSummary(expected.getCount(), expected.getSum(), expected.getMin(), expected.getMax()), actual, "SUMMARY");
            }
            var read = new AtomicInteger();
            require(M3LongFlow.of(new long[]{1, 2, 3}).mapLong(v -> { read.incrementAndGet(); return v; }).anyMatch(v -> v == 2), "ANY_RESULT");
            same(2, read.get(), "ANY_SHORT_CIRCUIT");
        });
        group("rolling Java terminal input ownership and progress", () -> {
            long[] values = {1, 2, 3, Long.MAX_VALUE, 1}; long[] before = values.clone();
            for (var tail : M3Windows.Tail.values()) {
                var m = new Monitor();
                array(oracle(values, 3, 2, tail, 0), M3NativeLongs.windowSums(values, 3, 2, tail, M3NativeLongs.Backend.JAVA, m), "JAVA_WINDOW_TERMINAL");
                same((long) values.length, m.worked, "JAVA_PROGRESS");
            }
            array(before, values, "NO_INPUT_MUTATION");
            type(IllegalArgumentException.class, () -> M3LongFlow.of(values).windowMax(0, 1, M3Windows.Tail.DROP), "WIDTH_VALIDATION");
            type(IllegalArgumentException.class, () -> M3LongFlow.of(values).windowSum(1, 0, M3Windows.Tail.DROP), "STEP_VALIDATION");
            type(NullPointerException.class, () -> M3LongFlow.of(values).windowMin(1, 1, null), "TAIL_VALIDATION");
            type(IllegalArgumentException.class, () -> M3LongFlow.of(values).skip(-1), "SKIP_VALIDATION");
        });
        group("reusable plans have independent accumulator state across threads", () -> {
            var plan = M3LongFlow.of(new long[]{1, 2, 3, 4}).windowSum(3, 1, M3Windows.Tail.PARTIAL)
                    .accumulate(() -> new long[1], (s, v) -> s[0] += v, s -> s[0]);
            try (var pool = Executors.newFixedThreadPool(4)) {
                var tasks = new ArrayList<java.util.concurrent.Callable<Long>>();
                for (int i = 0; i < 64; i++) tasks.add(plan::sum);
                try { for (var future : pool.invokeAll(tasks)) same(26L, future.get(), "CONCURRENT_PLAN_STATE"); }
                catch (Exception failure) { throw new AssertionError(failure); }
            }
        });
        if (nativeEnabled) {
            group("JNI signed overflow, copy/output boundaries and partial windows", () -> {
                var random = new Random(0x4e41544956454cL);
                for (int n : new int[]{0, 1, 2, 1023, 1024, 1025, 2048, 4097}) {
                    long[] values = new long[n]; for (int i = 0; i < n; i++) values[i] = random.nextLong();
                    for (int width : new int[]{1, 2, 9, 127, 1024, Integer.MAX_VALUE})
                        for (int step : new int[]{1, 2, 129, 2048, Integer.MAX_VALUE}) for (var tail : M3Windows.Tail.values()) {
                            long[] before = values.clone(); var m = new Monitor();
                            array(oracle(values, width, step, tail, 0), M3NativeLongs.windowSums(values, width, step, tail,
                                    M3NativeLongs.Backend.NATIVE, m), "NATIVE_WINDOW_ORACLE");
                            array(before, values, "NATIVE_BORROWED_INPUT"); same((long)n, m.worked, "NATIVE_PROGRESS");
                        }
                }
            });
            group("1000 randomized Java/JNI rolling-sum differentials", () -> {
                var random = new Random(239);
                for (int test = 0; test < 1000; test++) {
                    long[] v = new long[random.nextInt(350)]; for (int i = 0; i < v.length; i++) v[i] = random.nextLong();
                    int width = 1 + random.nextInt(400), step = 1 + random.nextInt(400);
                    var tail = random.nextBoolean() ? M3Windows.Tail.DROP : M3Windows.Tail.PARTIAL;
                    array(M3NativeLongs.windowSums(v, width, step, tail), M3NativeLongs.windowSums(v, width, step, tail,
                            M3NativeLongs.Backend.NATIVE, null), "JAVA_JNI_WINDOW_PARITY");
                }
            });
            group("JNI cancellation, callback failure, and private-entry validation", () -> {
                long[] v = new long[4097]; var m = new Monitor(); m.cancelAt = 1024;
                type(CancellationException.class, () -> M3NativeLongs.windowSums(v, 17, 1, M3Windows.Tail.PARTIAL,
                        M3NativeLongs.Backend.NATIVE, m), "NATIVE_CANCEL");
                same(1024L, m.worked, "NATIVE_CANCEL_BATCH_BOUND");
                var bad = new Monitor(); var exact = new RuntimeException("checkpoint failure"); bad.failure = exact;
                require(thrown(() -> M3NativeLongs.windowSums(v, 17, 1, M3Windows.Tail.DROP,
                        M3NativeLongs.Backend.NATIVE, bad)) == exact, "NATIVE_CALLBACK_IDENTITY");
                try {
                    var method = M3NativeLongs.class.getDeclaredMethod("windowsNative", long[].class, int.class, int.class,
                            boolean.class, M3ProgressMonitor.class);
                    method.setAccessible(true);
                    try { method.invoke(null, new long[]{1}, 0, 1, false, null); throw new AssertionError("PRIVATE_NATIVE_INVALID"); }
                    catch (InvocationTargetException failure) { require(failure.getCause() instanceof IllegalArgumentException, "PRIVATE_NATIVE_GUARD"); }
                    try { method.invoke(null, null, 1, 1, false, null); throw new AssertionError("PRIVATE_NATIVE_NULL"); }
                    catch (InvocationTargetException failure) { require(failure.getCause() instanceof NullPointerException, "PRIVATE_NATIVE_NULL_GUARD"); }
                } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            });
        } else {
            group("explicit native mode refuses an unloaded library", () -> type(UnsatisfiedLinkError.class,
                    () -> M3NativeLongs.windowSums(new long[]{1}, 1, 1, M3Windows.Tail.DROP,
                            M3NativeLongs.Backend.NATIVE, null), "NO_SILENT_NATIVE_FALLBACK"));
        }
        System.out.println("V3_GROUPS=" + groups + " ASSERTIONS=" + assertions + " WINDOW_CASES=" + windowCases + " NATIVE=" + nativeEnabled + " PASS");
    }
}
