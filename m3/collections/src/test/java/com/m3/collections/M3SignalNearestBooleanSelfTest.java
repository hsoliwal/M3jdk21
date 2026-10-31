/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Independent BigInteger nearest-neighbor oracle against exact Boolean bitmap input. */
public final class M3SignalNearestBooleanSelfTest {
    private static final String SOURCE = "c".repeat(64);
    private static final long VERSION = 71L;
    private static long checks;

    private M3SignalNearestBooleanSelfTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }
    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception instead of " + type.getSimpleName(), failure);
        }
        throw new AssertionError("missing expected " + type.getSimpleName());
    }
    private record Fixture(int[] ints, long[] longs, long[] valid,
            M3SignalQualitySchema schema, M3SignalDependencyEpochs epochs,
            M3SignalExactIntegerDistance metric, M3SignalBooleanQuery.Compiled program) { }

    private static Fixture fixture(int[] ints, long[] longs, long[] valid,
            int weightInt, int weightLong) {
        int n = ints.length;
        var columns = new M3SignalColumns(n,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 11L, false,
                        valid, ints, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.DOMAIN, 12L, false,
                        valid, null, longs, null));
        var graph = M3SignalRelations.of(n, new int[0], new int[0], new byte[0], new int[0]);
        var epochs = M3SignalDependencyEpochs.from(graph, false);
        var specA = new M3SignalQualitySchema.Spec("javac.exactInteger",
                M3SignalQualitySchema.Family.STRUCTURAL,
                M3SignalQualitySchema.Phase.COMPILER,
                M3SignalQualitySchema.Reliability.EXACT_FACT,
                M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                "count", "original javac", Double.NaN, "");
        var specB = new M3SignalQualitySchema.Spec("domain.exactLong",
                M3SignalQualitySchema.Family.ORDERING,
                M3SignalQualitySchema.Phase.INDEX,
                M3SignalQualitySchema.Reliability.EXACT_FACT,
                M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                "index", "original index", Double.NaN, "");
        var quality = M3SignalQualitySchema.declared(columns, epochs,
                VERSION, SOURCE, specA, specB);
        var metric = M3SignalExactIntegerDistance.of(quality,
                new int[]{0, 1}, new int[]{weightInt, weightLong});
        var query = M3SignalBooleanQuery.build(quality, 0, 1);
        var filter = query.compile(
                new M3SignalBooleanQuery.Term(0,
                        new M3SignalBooleanQuery.IntBetween(-8, 8)),
                new M3SignalBooleanQuery.Term(1,
                        new M3SignalBooleanQuery.LongBetween(Long.MIN_VALUE, Long.MAX_VALUE)),
                M3SignalBooleanQuery.Logic.AND);
        return new Fixture(ints, longs, valid, quality, epochs, metric, filter);
    }

    private static BigInteger unsignedExactDistance(Fixture f, int anchor, int row,
            int weightInt, int weightLong) {
        var integer = BigInteger.valueOf(f.ints[anchor])
                .subtract(BigInteger.valueOf(f.ints[row])).abs()
                .multiply(BigInteger.valueOf(weightInt));
        var longPart = BigInteger.valueOf(f.longs[anchor])
                .subtract(BigInteger.valueOf(f.longs[row])).abs()
                .multiply(BigInteger.valueOf(weightLong));
        return integer.add(longPart);
    }

    private static int[] oracle(Fixture f, int anchor, int[] selected, int k,
            int weightInt, int weightLong) {
        Integer[] ordinal = new Integer[selected.length];
        for (int i = 0; i < ordinal.length; i++) ordinal[i] = i;
        Arrays.sort(ordinal, (left, right) -> {
            var a = unsignedExactDistance(f, anchor, selected[left], weightInt, weightLong);
            var b = unsignedExactDistance(f, anchor, selected[right], weightInt, weightLong);
            int comparison = a.compareTo(b);
            return comparison == 0 ? Integer.compare(left, right) : comparison;
        });
        int[] result = new int[Math.min(k, selected.length)];
        for (int i = 0; i < result.length; i++) result[i] = selected[ordinal[i]];
        return result;
    }

    private static long[] packed(int n, int[] selected) {
        long[] membership = new long[(n + 63) >>> 6];
        for (int row : selected) membership[row >>> 6] |= 1L << (row & 63);
        return membership;
    }

    private static void edgeAndValidation() {
        int[] ints = {Integer.MIN_VALUE, 1, 1, -1, 3, 5, 0, 0, -8};
        long[] longs = {Long.MIN_VALUE, Long.MAX_VALUE, (1L << 53) + 1,
                Long.MIN_VALUE, Long.MAX_VALUE, 0L, -(1L << 53) - 1, 0L, 0L};
        long[] valid = {0x1feL}; // row 0 missing, others present
        var f = fixture(ints, longs, valid, 1_000_000, 999_999);
        int anchor = 1;
        int[] selected = f.program.evaluate(VERSION, SOURCE);
        int[] expected = oracle(f, anchor, selected, 5, 1_000_000, 999_999);
        check(Arrays.equals(expected, f.program.evaluateNearest(anchor, 5,
                f.metric, VERSION, SOURCE)), "exact Boolean-to-metric row order");
        long[] bitmap = packed(ints.length, selected);
        check(Arrays.equals(expected, f.metric.nearestByMask(anchor, bitmap, 5, VERSION, SOURCE)),
                "direct bitmap nearest parity");
        check(Arrays.equals(expected, f.metric.nearest(anchor, selected, 5, VERSION, SOURCE)),
                "original row-array nearest API preserved");
        check(f.program.evaluateNearest(anchor, 0, f.metric, VERSION, SOURCE).length == 0,
                "zero limit returns no row without modifying source");
        rejects(IllegalArgumentException.class,
                () -> f.metric.nearestByMask(anchor, new long[]{-1L}, 3, VERSION, SOURCE));
        rejects(IllegalArgumentException.class,
                () -> f.metric.nearestByMask(anchor, new long[]{0, 0}, 3, VERSION, SOURCE));
        rejects(IllegalArgumentException.class,
                () -> f.metric.nearestByMask(anchor, bitmap, -1, VERSION, SOURCE));
        rejects(IllegalArgumentException.class,
                () -> f.program.evaluateNearest(anchor, -1, f.metric, VERSION, SOURCE));
        rejects(IndexOutOfBoundsException.class,
                () -> f.program.evaluateNearest(-1, 1, f.metric, VERSION, SOURCE));
        rejects(IllegalStateException.class,
                () -> f.program.evaluateNearest(anchor, 1, f.metric, VERSION + 1, SOURCE));
        rejects(IllegalStateException.class,
                () -> f.metric.nearestByMask(anchor, bitmap, 1, VERSION, "0".repeat(64)));
        var foreign = fixture(ints, longs, valid, 1_000_000, 999_999);
        rejects(IllegalArgumentException.class,
                () -> f.program.evaluateNearest(anchor, 1, foreign.metric, VERSION, SOURCE));
        long[] missing = {1L};
        rejects(IllegalArgumentException.class,
                () -> f.metric.nearestByMask(anchor, missing, 1, VERSION, SOURCE));
        f.epochs.invalidateAll();
        rejects(IllegalStateException.class,
                () -> f.program.evaluateNearest(anchor, 1, f.metric, VERSION, SOURCE));
        rejects(IllegalStateException.class,
                () -> f.metric.nearestByMask(anchor, bitmap, 1, VERSION, SOURCE));
    }

    private static void randomizedDistanceAndBoolean() {
        Random random = new Random(0x4d33455841435425L);
        for (int trial = 0; trial < 630; trial++) {
            int n = 2 + random.nextInt(155);
            int[] ints = new int[n];
            long[] longs = new long[n];
            long[] valid = new long[(n + 63) >>> 6];
            for (int row = 0; row < n; row++) {
                ints[row] = random.nextInt(17) - 8;
                longs[row] = switch (random.nextInt(12)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> (1L << 53) + 1;
                    case 3 -> -(1L << 53) - 1;
                    default -> random.nextInt(9) - 4L;
                };
                if (random.nextInt(7) != 0) valid[row >>> 6] |= 1L << (row & 63);
            }
            int weightInt = 1 + random.nextInt(1_000_000);
            int weightLong = 1 + random.nextInt(1_000_000);
            var f = fixture(ints, longs, valid, weightInt, weightLong);
            int[] matches = f.program.evaluate(VERSION, SOURCE);
            if (matches.length == 0) continue;
            int anchor = matches[random.nextInt(matches.length)];
            long[] bitmap = packed(n, matches);
            long[] immutable = bitmap.clone();
            for (int q = 0; q < 10; q++) {
                int k = random.nextInt(n + 5);
                int[] expected = oracle(f, anchor, matches, k, weightInt, weightLong);
                check(Arrays.equals(expected,
                        f.program.evaluateNearest(anchor, k, f.metric, VERSION, SOURCE)),
                        "random Boolean nearest with original unsigned 128-bit source facts");
                check(Arrays.equals(expected,
                        f.metric.nearestByMask(anchor, bitmap, k, VERSION, SOURCE)),
                        "random direct bit mask nearest");
                check(Arrays.equals(expected,
                        f.metric.nearest(anchor, matches, k, VERSION, SOURCE)),
                        "preserved original array nearest oracle");
                check(Arrays.equals(immutable, bitmap), "bitmask input ownership unchanged");
            }
        }
    }

    private static void cancellationAndProgress() {
        int[] ints = new int[193];
        long[] longs = new long[193];
        long[] valid = M3SignalColumns.allValid(ints.length);
        var f = fixture(ints, longs, valid, 3, 2);
        int[] selected = f.program.evaluate(VERSION, SOURCE);
        long[] membership = packed(ints.length, selected);
        class Observation implements InvocationHandler {
            int begins, dones, checks;long worked;boolean canceled;boolean changeOnDone;
            M3ProgressMonitor monitor() {
                return (M3ProgressMonitor) Proxy.newProxyInstance(
                        M3ProgressMonitor.class.getClassLoader(),
                        new Class<?>[]{M3ProgressMonitor.class}, this);
            }
            @Override public Object invoke(Object proxy, java.lang.reflect.Method method,
                    Object[] args) {
                return switch (method.getName()) {
                    case "checkCanceled" -> {
                        checks++;
                        if (canceled) throw new CancellationException("source canceled");
                        yield null;
                    }
                    case "isCanceled" -> canceled;
                    case "beginTask" -> { begins++; yield null; }
                    case "worked" -> { worked += ((Number) args[0]).longValue(); yield null; }
                    case "done" -> { dones++; if (changeOnDone) f.epochs.invalidateAll(); yield null; }
                    case "toString" -> "Probe";
                    default -> null;
                };
            }
        }
        Observation direct = new Observation();
        int[] expected = oracle(f, 0, selected, 5, 3, 2);
        check(Arrays.equals(expected, f.metric.nearestByMask(0, membership,
                5, VERSION, SOURCE, direct.monitor())), "standalone nearest successful");
        check(direct.begins == 1 && direct.dones == 1 && direct.worked == selected.length,
                "standalone M3 monitor exactly one task and per-candidate credits");
        Observation embedded = new Observation();
        check(Arrays.equals(expected, f.program.evaluateNearest(0, 5,
                f.metric, VERSION, SOURCE, embedded.monitor())), "embedded nearest parity");
        check(embedded.begins == 1 && embedded.dones == 1 && embedded.worked == 3,
                "embedded distance reuses Boolean progress task, not nested begin/done");
        Observation canceled = new Observation();
        canceled.canceled = true;
        rejects(CancellationException.class, () -> f.program.evaluateNearest(0,
                5, f.metric, VERSION, SOURCE, canceled.monitor()));
        check(canceled.dones == 0, "cancellation has no successful completion callback");
        Observation changed = new Observation();
        changed.changeOnDone = true;
        rejects(IllegalStateException.class, () -> f.program.evaluateNearest(0,
                5, f.metric, VERSION, SOURCE, changed.monitor()));
        check(changed.dones == 1, "owner source invalidation on completion refused");
    }

    public static void main(String[] args) {
        edgeAndValidation();
        randomizedDistanceAndBoolean();
        cancellationAndProgress();
        System.out.println("M3_SIGNAL_BOOLEAN_EXACT_INTEGER_NEAREST_PASS checks=" + checks);
    }
}
