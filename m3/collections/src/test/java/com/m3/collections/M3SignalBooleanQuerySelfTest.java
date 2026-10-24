/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent unindexed row-by-row reference for exact mixed-type AND/OR masks. */
public final class M3SignalBooleanQuerySelfTest {
    private static final String SHA = "d".repeat(64);
    private static final long VERSION = 31L;
    private static long checks;
    private M3SignalBooleanQuerySelfTest() { }

    private static void check(boolean pass, String why) {
        checks++;
        if (!pass) throw new AssertionError("failed: " + why + " check=" + checks);
    }
    private static void reject(Class<? extends Throwable> kind, Runnable call) {
        checks++;
        try { call.run(); }
        catch (Throwable actual) {
            if (kind.isInstance(actual)) return;
            throw new AssertionError("wrong refusal type: " + actual, actual);
        }
        throw new AssertionError("missing exception " + kind.getSimpleName());
    }
    private static long[] mask(boolean[] valid) {
        long[] result = new long[(valid.length + 63) >>> 6];
        for (int i = 0; i < valid.length; i++) {
            if (valid[i]) result[i >>> 6] |= 1L << (i & 63);
        }
        return result;
    }

    private record Data(int[] ints, long[] longs, double[] doubles,
                        boolean[][] valid, M3SignalQualitySchema schema) { }

    private static Data data(int[] ints, long[] longs, double[] doubles, boolean[][] validity) {
        int rows = ints.length;
        var columns = new M3SignalColumns(rows,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 11L, false, mask(validity[0]),
                        ints, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.DOMAIN, 12L, false, mask(validity[1]),
                        null, longs, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 13L, false, mask(validity[2]),
                        null, null, doubles));
        var graph = M3SignalRelations.of(rows, new int[0], new int[0],
                new byte[0], new int[0]);
        var epochs = M3SignalDependencyEpochs.from(graph, false);
        var schema = M3SignalQualitySchema.declared(columns, epochs, VERSION, SHA,
                new M3SignalQualitySchema.Spec("javac.declarations",
                        M3SignalQualitySchema.Family.STRUCTURAL,
                        M3SignalQualitySchema.Phase.COMPILER,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "count", "M3SourceSignals", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("domain.ordinal",
                        M3SignalQualitySchema.Family.ORDERING,
                        M3SignalQualitySchema.Phase.INDEX,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "ordinal", "DomainPolicy", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("runtime.costEstimate",
                        M3SignalQualitySchema.Family.EXECUTION,
                        M3SignalQualitySchema.Phase.RUNTIME,
                        M3SignalQualitySchema.Reliability.ESTIMATE,
                        M3SignalQualitySchema.Invalidation.PROFILE_REFRESH,
                        "cycles", "Profiling", 0.7, ""));
        return new Data(ints, longs, doubles, validity, schema);
    }

    private static boolean matches(Data data, int row, int dim,
            M3SignalBooleanQuery.Predicate predicate) {
        if (predicate instanceof M3SignalBooleanQuery.Missing) return !data.valid[dim][row];
        if (!data.valid[dim][row]) return false;
        if (predicate instanceof M3SignalBooleanQuery.IntBetween range) {
            return Integer.compare(data.ints[row], range.lower()) >= 0
                    && Integer.compare(data.ints[row], range.upper()) <= 0;
        }
        if (predicate instanceof M3SignalBooleanQuery.LongBetween range) {
            return Long.compare(data.longs[row], range.lower()) >= 0
                    && Long.compare(data.longs[row], range.upper()) <= 0;
        }
        if (predicate instanceof M3SignalBooleanQuery.DoubleBetween range) {
            return Double.compare(data.doubles[row], range.lower()) >= 0
                    && Double.compare(data.doubles[row], range.upper()) <= 0;
        }
        throw new AssertionError("unexpected predicate");
    }

    private static int[] reference(Data source, int[] dimensions,
            M3SignalBooleanQuery.Predicate[] predicates, boolean union) {
        int[] found = new int[source.ints.length];
        int cursor = 0;
        for (int row = 0; row < source.ints.length; row++) {
            boolean accepted = !union;
            for (int i = 0; i < dimensions.length; i++) {
                boolean passed = matches(source, row, dimensions[i], predicates[i]);
                accepted = union ? accepted || passed : accepted && passed;
            }
            if (accepted) found[cursor++] = row;
        }
        return Arrays.copyOf(found, cursor);
    }

    private static void fixedCases() {
        int[] ints = {-10, 0, 0, 9, Integer.MIN_VALUE, Integer.MAX_VALUE, 4};
        long[] longs = {Long.MIN_VALUE, (1L << 53) + 1, (1L << 53),
                Long.MAX_VALUE, -1, 0, 42};
        double[] doubles = {Double.NEGATIVE_INFINITY, -0.0, 0.0, Double.NaN,
                Double.POSITIVE_INFINITY, -0.0, 3.5};
        boolean[][] validity = {
            {true, true, false, true, true, true, true},
            {true, true, true, true, false, true, true},
            {true, true, true, true, true, false, true}
        };
        Data data = data(ints, longs, doubles, validity);
        var plan = M3SignalBooleanQuery.build(data.schema, 1, 0, 2);
        check(plan.predicateCount() == 3 && plan.dimensionAt(1) == 0,
                "stable caller-declared column coordinates");
        var matchLong = new M3SignalBooleanQuery.LongBetween((1L << 53) + 1,
                (1L << 53) + 1);
        var missing = M3SignalBooleanQuery.Missing.INSTANCE;
        var plusZero = new M3SignalBooleanQuery.DoubleBetween(0.0, 0.0);
        var predicates = new M3SignalBooleanQuery.Predicate[]{matchLong, missing, plusZero};
        check(Arrays.equals(plan.and(VERSION, SHA, predicates), new int[0]),
                "non-overlapping conjunction");
        check(Arrays.equals(plan.or(VERSION, SHA, predicates),
                reference(data, new int[]{1, 0, 2}, predicates, true)),
                "mixed-type OR and missing union");
        var p2 = M3SignalBooleanQuery.build(data.schema, 0, 1);
        check(Arrays.equals(p2.and(VERSION, SHA,
                new M3SignalBooleanQuery.IntBetween(0, 0), matchLong), new int[]{1}),
                "exact full-width long AND int equality");
        check(Arrays.equals(M3SignalBooleanQuery.build(data.schema, 2).and(VERSION, SHA,
                new M3SignalBooleanQuery.DoubleBetween(-0.0, -0.0)), new int[]{1}),
                "signed negative zero selected only when present");
        check(Arrays.equals(M3SignalBooleanQuery.build(data.schema, 2).and(VERSION, SHA,
                new M3SignalBooleanQuery.DoubleBetween(Double.NaN, Double.NaN)), new int[]{3}),
                "Double.compare NaN total-order equivalence");
        check(Arrays.equals(M3SignalBooleanQuery.build(data.schema, 0).and(VERSION, SHA,
                M3SignalBooleanQuery.Missing.INSTANCE), new int[]{2}),
                "explicit missing not confused with zero");
        reject(IllegalArgumentException.class, () -> M3SignalBooleanQuery.build(data.schema));
        reject(IllegalArgumentException.class, () -> M3SignalBooleanQuery.build(data.schema, 1, 1));
        reject(IndexOutOfBoundsException.class, () -> M3SignalBooleanQuery.build(data.schema, 3));
        reject(IllegalArgumentException.class, () -> new M3SignalBooleanQuery.IntBetween(3, 2));
        reject(IllegalArgumentException.class, () -> new M3SignalBooleanQuery.LongBetween(3, 2));
        reject(IllegalArgumentException.class, () -> new M3SignalBooleanQuery.DoubleBetween(+0.0, -0.0));
        reject(IllegalArgumentException.class, () -> plan.and(VERSION, SHA,
                new M3SignalBooleanQuery.IntBetween(0, 1), missing, plusZero));
        reject(IllegalArgumentException.class, () -> plan.or(VERSION, SHA, missing));
        reject(NullPointerException.class, () -> plan.and(VERSION, SHA,
                matchLong, null, plusZero));
        reject(IllegalStateException.class, () -> plan.or(VERSION + 1, SHA, predicates));
        reject(IllegalStateException.class, () -> plan.or(VERSION, "0".repeat(64), predicates));
        int[] old = plan.or(VERSION, SHA, predicates);
        old[0] = Integer.MAX_VALUE;
        check(!Arrays.equals(old, plan.or(VERSION, SHA, predicates)),
                "defensive result array ownership");
        data.schema.invalidationEpochsOwner().invalidateAll();
        reject(IllegalStateException.class, () -> plan.and(VERSION, SHA, predicates));
        reject(IllegalStateException.class, () -> plan.or(VERSION, SHA, predicates));
    }

    private static void fuzzCases() {
        Random random = new Random(0x5349474e414c3231L);
        for (int trial = 0; trial < 450; trial++) {
            int size = random.nextInt(165);
            int[] ints = new int[size];
            long[] longs = new long[size];
            double[] doubles = new double[size];
            boolean[][] validity = new boolean[3][size];
            for (int row = 0; row < size; row++) {
                ints[row] = random.nextInt(31) - 15;
                longs[row] = random.nextInt(12) == 0
                        ? (random.nextBoolean() ? Long.MAX_VALUE : Long.MIN_VALUE)
                        : (1L << 53) + random.nextInt(251) - 125;
                doubles[row] = switch (random.nextInt(12)) {
                    case 0 -> Double.NaN;
                    case 1 -> Double.NEGATIVE_INFINITY;
                    case 2 -> Double.POSITIVE_INFINITY;
                    case 3 -> -0.0;
                    case 4 -> +0.0;
                    default -> (random.nextInt(23) - 11) / 4.0;
                };
                for (int d = 0; d < 3; d++) validity[d][row] = random.nextInt(5) != 0;
            }
            var input = data(ints, longs, doubles, validity);
            int[] dimensions = {0, 1, 2};
            var plan = M3SignalBooleanQuery.build(input.schema, dimensions);
            for (int q = 0; q < 31; q++) {
                M3SignalBooleanQuery.Predicate[] predicates = new M3SignalBooleanQuery.Predicate[3];
                for (int dim = 0; dim < 3; dim++) {
                    if (random.nextInt(9) == 0) {
                        predicates[dim] = M3SignalBooleanQuery.Missing.INSTANCE;
                    } else if (dim == 0) {
                        int a = random.nextInt(41) - 20, b = random.nextInt(41) - 20;
                        predicates[dim] = new M3SignalBooleanQuery.IntBetween(Math.min(a,b), Math.max(a,b));
                    } else if (dim == 1) {
                        long a = (1L << 53) + random.nextInt(301) - 150;
                        long b = (1L << 53) + random.nextInt(301) - 150;
                        predicates[dim] = new M3SignalBooleanQuery.LongBetween(Math.min(a,b), Math.max(a,b));
                    } else {
                        double a = random.nextInt(9) == 0 ? Double.NaN : (random.nextInt(15)-7)*0.5;
                        double b = random.nextInt(9) == 0 ? Double.NaN : (random.nextInt(15)-7)*0.5;
                        predicates[dim] = Double.compare(a,b) <= 0
                                ? new M3SignalBooleanQuery.DoubleBetween(a,b)
                                : new M3SignalBooleanQuery.DoubleBetween(b,a);
                    }
                }
                int[] expectedAnd = reference(input, dimensions, predicates, false);
                int[] expectedOr = reference(input, dimensions, predicates, true);
                check(Arrays.equals(expectedAnd, plan.and(VERSION, SHA, predicates)),
                        "AND differential trial="+trial);
                check(Arrays.equals(expectedOr, plan.or(VERSION, SHA, predicates)),
                        "OR differential trial="+trial);
            }
        }
    }

    public static void main(String[] args) {
        fixedCases();
        fuzzCases();
        System.out.println("M3_SIGNAL_BOOLEAN_QUERY_PASS checks=" + checks);
    }
}
