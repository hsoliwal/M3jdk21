/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Independent stable physical-row oracle for bounded query windows and count-only pages. */
public final class M3SignalViewportWindowSelfTest {
    private static final long VERSION = 1701L;
    private static final String SOURCE = "5".repeat(64);
    private static long checks;
    private M3SignalViewportWindowSelfTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("unexpected refusal instead of " + type, error);
        }
        throw new AssertionError("missing exception " + type.getName());
    }

    private static final class Progress implements InvocationHandler {
        boolean canceled;
        int checked, began, done, worked;
        int cancelAfterChecks = Integer.MAX_VALUE;
        Runnable onDone;
        M3ProgressMonitor monitor() {
            return (M3ProgressMonitor) Proxy.newProxyInstance(
                    M3ProgressMonitor.class.getClassLoader(),
                    new Class<?>[] {M3ProgressMonitor.class}, this);
        }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "checkCanceled" -> {
                    if (++checked >= cancelAfterChecks || canceled) {
                        throw new CancellationException("cancelled signal page");
                    }
                    yield null;
                }
                case "isCanceled" -> canceled;
                case "beginTask" -> { began++; yield null; }
                case "worked" -> { worked += Math.toIntExact((long) args[0]); yield null; }
                case "done" -> { done++; if (onDone != null) onDone.run(); yield null; }
                case "toString" -> "Page progress";
                default -> throw new AssertionError("unsupported monitor method " + method.getName());
            };
        }
    }

    private record Data(M3SignalQualitySchema schema, boolean[] valid, int[] ints,
            long[] longs, double[] doubles) { }

    private static long[] mask(boolean[] flags) {
        long[] result = new long[(flags.length + 63) >>> 6];
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) result[i >>> 6] |= 1L << (i & 63);
        }
        return result;
    }

    private static Data input(Random rnd, int n) {
        boolean[] valid = new boolean[n];
        int[] ints = new int[n];
        long[] longs = new long[n];
        double[] doubles = new double[n];
        for (int i = 0; i < n; i++) {
            valid[i] = rnd.nextInt(6) != 0;
            ints[i] = rnd.nextInt(37) - 18;
            longs[i] = (1L << 53) + rnd.nextInt(41) - 20;
            doubles[i] = switch (rnd.nextInt(8)) {
                case 0 -> Double.NaN;
                case 1 -> -0.0;
                case 2 -> +0.0;
                case 3 -> Double.POSITIVE_INFINITY;
                default -> (rnd.nextInt(19) - 9) / 2.0;
            };
        }
        long[] bits = mask(valid);
        var columns = new M3SignalColumns(n,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 1L, false, bits, ints, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.DOMAIN, 2L, false, bits, null, longs, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 3L, false, bits, null, null, doubles));
        var relations = M3SignalRelations.of(n, new int[0], new int[0],
                new byte[0], new int[0]);
        var epochs = M3SignalDependencyEpochs.from(relations, false);
        var schema = M3SignalQualitySchema.declared(columns, epochs, VERSION, SOURCE,
                new M3SignalQualitySchema.Spec("javac.int", M3SignalQualitySchema.Family.STRUCTURAL,
                        M3SignalQualitySchema.Phase.COMPILER,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "items", "javac", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("domain.long", M3SignalQualitySchema.Family.ORDERING,
                        M3SignalQualitySchema.Phase.INDEX,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "rank", "domain", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("runtime.double", M3SignalQualitySchema.Family.EXECUTION,
                        M3SignalQualitySchema.Phase.RUNTIME,
                        M3SignalQualitySchema.Reliability.ESTIMATE,
                        M3SignalQualitySchema.Invalidation.PROFILE_REFRESH,
                        "score", "profiler", 0.4, ""));
        return new Data(schema, valid, ints, longs, doubles);
    }

    // Independent row-by-row Boolean reference, not a composition of query-engine masks.
    private static int[] expected(Data data) {
        int[] rows = new int[data.ints.length];
        int found = 0;
        for (int i = 0; i < data.ints.length; i++) {
            boolean intHit = data.valid[i] && data.ints[i] >= -7 && data.ints[i] <= 7;
            boolean missing = !data.valid[i];
            boolean longHit = data.valid[i] && data.longs[i] >= (1L << 53) + 2
                    && data.longs[i] <= (1L << 53) + 13;
            boolean doubleHit = data.valid[i]
                    && Double.compare(data.doubles[i], -0.0) >= 0
                    && Double.compare(data.doubles[i], +0.0) <= 0;
            if (((intHit || missing) && longHit) || !doubleHit) rows[found++] = i;
        }
        return Arrays.copyOf(rows, found);
    }

    private static M3SignalBooleanQuery.Compiled compile(M3SignalQualitySchema schema) {
        var query = M3SignalBooleanQuery.build(schema, 0, 1, 2);
        return query.compile(
                new M3SignalBooleanQuery.Term(0, new M3SignalBooleanQuery.IntBetween(-7, 7)),
                new M3SignalBooleanQuery.Term(0, M3SignalBooleanQuery.Missing.INSTANCE),
                M3SignalBooleanQuery.Logic.OR,
                new M3SignalBooleanQuery.Term(1, new M3SignalBooleanQuery.LongBetween(
                        (1L << 53) + 2, (1L << 53) + 13)),
                M3SignalBooleanQuery.Logic.AND,
                new M3SignalBooleanQuery.Term(2, new M3SignalBooleanQuery.DoubleBetween(-0.0, +0.0)),
                M3SignalBooleanQuery.Logic.NOT,
                M3SignalBooleanQuery.Logic.OR);
    }

    private static void pagingOracle() {
        Random rnd = new Random(0x4d3356494557504cL);
        int[] boundaries = {0, 1, 2, 3, 63, 64, 65, 127, 128, 129, 257, 1001, 4097};
        for (int trial = 0; trial < 360; trial++) {
            int n = trial < boundaries.length ? boundaries[trial] : rnd.nextInt(500);
            var data = input(rnd, n);
            var compiled = compile(data.schema);
            int[] baseline = expected(data);
            check(Arrays.equals(compiled.evaluate(VERSION, SOURCE), baseline),
                    "complete evaluator agrees with independent row oracle");
            for (int iteration = 0; iteration < 36; iteration++) {
                int skip = switch (iteration % 5) {
                    case 0 -> 0;
                    case 1 -> baseline.length;
                    case 2 -> Integer.MAX_VALUE;
                    default -> rnd.nextInt(baseline.length + 19);
                };
                int limit = switch (iteration % 6) {
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> Integer.MAX_VALUE;
                    default -> rnd.nextInt(120);
                };
                var page = compiled.evaluateWindow(skip, limit, VERSION, SOURCE);
                int from = Math.min(skip, baseline.length);
                int count = Math.min(limit, baseline.length - from);
                check(page.totalMatches() == baseline.length && page.start() == from,
                        "correct total count / clamped physical position");
                check(page.size() == count && Arrays.equals(page.rows(),
                        Arrays.copyOfRange(baseline, from, from + count)),
                        "paged rows are exact and physical-order stable");
                int[] mutated = page.rows();
                if (mutated.length != 0) {
                    mutated[0] = Integer.MAX_VALUE;
                    check(page.rows()[0] != Integer.MAX_VALUE,
                            "result page owns its defensive row array");
                }
            }
        }
    }

    private static void stressCancellationAndFreshness() {
        int size = 262_153;
        var data = input(new Random(55), size);
        var query = M3SignalBooleanQuery.build(data.schema, 0);
        var compiled = query.compile(M3SignalBooleanQuery.Logic.ALL);
        var page = compiled.evaluateWindow(100_003, 7, VERSION, SOURCE);
        check(page.totalMatches() == size && page.size() == 7 && page.start() == 100_003,
                "large page enumerated without allocating all matching row IDs");
        check(Arrays.equals(page.rows(), new int[] {100_003,100_004,100_005,
                100_006,100_007,100_008,100_009}), "bounded full-mask result page");
        var countOnly = compiled.evaluateWindow(0, 0, VERSION, SOURCE);
        check(countOnly.totalMatches() == size && countOnly.rows().length == 0,
                "count-only selection materializes zero row IDs");
        var beyond = compiled.evaluateWindow(Integer.MAX_VALUE, Integer.MAX_VALUE,
                VERSION, SOURCE);
        check(beyond.start() == size && beyond.size() == 0,
                "offset beyond all matches is an empty, bounded page");
        refuse(IllegalArgumentException.class, () ->
                compiled.evaluateWindow(-1, 2, VERSION, SOURCE));
        refuse(IllegalArgumentException.class, () ->
                compiled.evaluateWindow(0, -1, VERSION, SOURCE));
        var progress = new Progress();
        var withMonitor = compiled.evaluateWindow(10, 4, VERSION, SOURCE, progress.monitor());
        check(withMonitor.totalMatches() == size && withMonitor.size() == 4,
                "cancellable page retains exact match count");
        check(progress.began == 1 && progress.worked == 1 && progress.done == 1,
                "successful page uses the existing progress contract");
        var canceled = new Progress(); canceled.canceled = true;
        refuse(CancellationException.class, () ->
                compiled.evaluateWindow(0, 5, VERSION, SOURCE, canceled.monitor()));
        check(canceled.began == 0 && canceled.done == 0, "pre-cancel never starts work");
        var lateCancel = new Progress(); lateCancel.cancelAfterChecks = 5;
        refuse(CancellationException.class, () ->
                compiled.evaluateWindow(0, 5, VERSION, SOURCE, lateCancel.monitor()));
        check(lateCancel.done == 0, "canceled page never publishes partial result");
        var mutatedOnDone = new Progress();
        mutatedOnDone.onDone = data.schema.invalidationEpochsOwner()::invalidateAll;
        refuse(IllegalStateException.class, () ->
                compiled.evaluateWindow(0, 5, VERSION, SOURCE, mutatedOnDone.monitor()));
        refuse(IllegalStateException.class, () ->
                compiled.evaluateWindow(0, 5, VERSION, SOURCE));
        refuse(IllegalStateException.class, () ->
                compiled.evaluateWindow(0, 5, VERSION, "1".repeat(64)));
    }

    public static void main(String[] args) {
        pagingOracle();
        stressCancellationAndFreshness();
        System.out.println("M3_SIGNAL_VIEWPORT_WINDOW_PASS checks=" + checks);
    }
}
