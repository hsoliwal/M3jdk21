/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent boxed oracle for the production primitive exact-range index. */
public final class M3SignalExactRangeIndexSelfTest {
    private static final String ID = "4".repeat(64);
    private static long checks;
    private M3SignalExactRangeIndexSelfTest() { }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message + " at check=" + checks);
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable actual) {
            if (kind.isInstance(actual)) return;
            throw new AssertionError("unexpected failure: " + actual, actual);
        }
        throw new AssertionError("missing expected " + kind.getSimpleName());
    }
    private static long[] validity(boolean[] valid) {
        long[] bits = new long[(valid.length + 63) >>> 6];
        for (int i = 0; i < valid.length; i++) {
            if (valid[i]) bits[i >>> 6] |= 1L << (i & 63);
        }
        return bits;
    }
    private static M3SignalQualitySchema schema(int[] ints, long[] longs,
            double[] doubles, boolean[] valid) {
        int rows = ints.length;
        long[] masks = validity(valid);
        var columns = new M3SignalColumns(rows,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 11, true, masks, ints, null, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.DOMAIN, 12, false, masks, null, longs, null),
                new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                        M3SignalColumns.Origin.PROFILED, 13, true, masks, null, null, doubles));
        var graph = M3SignalRelations.of(rows, new int[0], new int[0],
                new byte[0], new int[0]);
        var epochs = M3SignalDependencyEpochs.from(graph, false);
        return M3SignalQualitySchema.declared(columns, epochs, 55, ID,
                new M3SignalQualitySchema.Spec("source.size",
                        M3SignalQualitySchema.Family.STRUCTURAL,
                        M3SignalQualitySchema.Phase.COMPILER,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "count", "javac", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("domain.long",
                        M3SignalQualitySchema.Family.ORDERING,
                        M3SignalQualitySchema.Phase.INDEX,
                        M3SignalQualitySchema.Reliability.EXACT_FACT,
                        M3SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "ordinal", "domain", Double.NaN, ""),
                new M3SignalQualitySchema.Spec("profile.double",
                        M3SignalQualitySchema.Family.STATISTICAL,
                        M3SignalQualitySchema.Phase.RUNTIME,
                        M3SignalQualitySchema.Reliability.ESTIMATE,
                        M3SignalQualitySchema.Invalidation.PROFILE_REFRESH,
                        "score", "profiler", 0.8, ""));
    }
    private static int[] reference(boolean[] valid, int dimension, int[] ints,
            long[] longs, double[] doubles, double lowerDouble, double upperDouble,
            long lowerLong, long upperLong, int lowerInt, int upperInt) {
        List<Integer> hit = new ArrayList<>();
        for (int i = 0; i < valid.length; i++) {
            if (!valid[i]) continue;
            boolean inside = switch (dimension) {
                case 0 -> Integer.compare(ints[i], lowerInt) >= 0
                        && Integer.compare(ints[i], upperInt) <= 0;
                case 1 -> Long.compare(longs[i], lowerLong) >= 0
                        && Long.compare(longs[i], upperLong) <= 0;
                case 2 -> Double.compare(doubles[i], lowerDouble) >= 0
                        && Double.compare(doubles[i], upperDouble) <= 0;
                default -> throw new AssertionError();
            };
            if (inside) hit.add(i);
        }
        hit.sort((left, right) -> {
            int comparison = switch (dimension) {
                case 0 -> Integer.compare(ints[left], ints[right]);
                case 1 -> Long.compare(longs[left], longs[right]);
                case 2 -> Double.compare(doubles[left], doubles[right]);
                default -> throw new AssertionError();
            };
            return comparison == 0 ? Integer.compare(left, right) : comparison;
        });
        return hit.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void exactEdgeCases() {
        int[] ints = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, 0, 0, 7, 9};
        long[] longs = {Long.MIN_VALUE, -(1L << 53) - 1, 1L << 53,
                (1L << 53) + 1, Long.MAX_VALUE, 10, 10, 3, 9};
        double[] doubles = {Double.NEGATIVE_INFINITY, -0.0, 0.0,
                Double.POSITIVE_INFINITY, Double.NaN, Double.NaN, 0.0, 3.25, -0.0};
        boolean[] valid = {true, true, true, true, true, true, false, true, true};
        var schema = schema(ints, longs, doubles, valid);
        var intIndex = M3SignalExactRangeIndex.build(schema, 0);
        var longIndex = M3SignalExactRangeIndex.build(schema, 1);
        var doubleIndex = M3SignalExactRangeIndex.build(schema, 2);
        check(intIndex.presentCount() == 8 && intIndex.missingCount() == 1,
                "validity separates missing values from numerically equal zeros");
        check(Arrays.equals(intIndex.missingRows(55, ID), new int[] {6}), "missing row identity");
        check(Arrays.equals(intIndex.betweenInts(0, 0, 55, ID), new int[] {2, 5}),
                "stable equal integer keys, missing zero excluded");
        check(Arrays.equals(longIndex.betweenLongs((1L << 53) + 1,
                (1L << 53) + 1, 55, ID), new int[] {3}), "full 64-bit precision");
        check(Arrays.equals(longIndex.betweenLongs(Long.MIN_VALUE, Long.MAX_VALUE,
                55, ID), longIndex.presentRows(55, ID)), "full signed long domain");
        check(Arrays.equals(doubleIndex.betweenDoubles(-0.0, -0.0, 55, ID),
                new int[] {1, 8}), "negative zero distinct from positive zero");
        check(Arrays.equals(doubleIndex.betweenDoubles(0.0, 0.0, 55, ID),
                new int[] {2}), "positive zero distinct from negative zero");
        check(Arrays.equals(doubleIndex.betweenDoubles(Double.NaN, Double.NaN,
                55, ID), new int[] {4, 5}), "NaN JDK total-order equivalence");
        check(Arrays.equals(doubleIndex.betweenDoubles(Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, 55, ID), new int[] {0, 1, 8, 2, 7, 3}),
                "infinite interval excludes NaN");
        var copied = longIndex.presentRows(55, ID);
        copied[0] = Integer.MAX_VALUE;
        check(longIndex.presentRows(55, ID)[0] != Integer.MAX_VALUE,
                "owned index not corrupted by caller result mutation");
        rejects(IllegalArgumentException.class, () -> intIndex.betweenLongs(0, 2, 55, ID));
        rejects(IllegalArgumentException.class, () -> doubleIndex.betweenInts(0, 2, 55, ID));
        rejects(IllegalArgumentException.class, () -> longIndex.betweenLongs(2, 1, 55, ID));
        rejects(IllegalArgumentException.class, () -> doubleIndex.betweenDoubles(0, -0.0, 55, ID));
        rejects(IllegalStateException.class, () -> intIndex.betweenInts(0, 1, 56, ID));
        rejects(IllegalStateException.class, () -> intIndex.betweenInts(0, 1, 55, "0".repeat(64)));
        rejects(IndexOutOfBoundsException.class, () -> M3SignalExactRangeIndex.build(schema, 3));
        var original = intIndex.presentRows(55, ID);
        schema.invalidationEpochsOwner().invalidate(6); // Missing row changes still invalidate index.
        rejects(IllegalStateException.class, () -> intIndex.presentRows(55, ID));
        rejects(IllegalStateException.class, () -> longIndex.betweenLongs(1, 3, 55, ID));
        check(original.length == 8, "caller-owned past snapshot is unaffected");
    }

    private static void seededDifferential() {
        Random random = new Random(0x4d335349474e414cL);
        for (int trial = 0; trial < 360; trial++) {
            int n = random.nextInt(140);
            int[] ints = new int[n];
            long[] longs = new long[n];
            double[] doubles = new double[n];
            boolean[] valid = new boolean[n];
            for (int i = 0; i < n; i++) {
                valid[i] = random.nextInt(7) != 0;
                ints[i] = random.nextInt(23) - 11;
                longs[i] = random.nextInt(13) == 0
                        ? (random.nextBoolean() ? Long.MAX_VALUE : Long.MIN_VALUE)
                        : (1L << 53) + random.nextInt(61) - 30;
                doubles[i] = switch (random.nextInt(14)) {
                    case 0 -> Double.NaN;
                    case 1 -> Double.POSITIVE_INFINITY;
                    case 2 -> Double.NEGATIVE_INFINITY;
                    case 3 -> -0.0;
                    case 4 -> +0.0;
                    default -> (random.nextInt(23) - 11) * 0.5;
                };
            }
            var schema = schema(ints, longs, doubles, valid);
            var a = M3SignalExactRangeIndex.build(schema, 0);
            var b = M3SignalExactRangeIndex.build(schema, 1);
            var c = M3SignalExactRangeIndex.build(schema, 2);
            check(a.presentCount() + a.missingCount() == n, "every coordinate exactly once");
            int missing = 0;
            for (boolean v : valid) if (!v) missing++;
            check(a.missingRows(55, ID).length == missing, "exact missing mask cardinality");
            for (int j = 0; j < 35; j++) {
                int ai = random.nextInt(31) - 15, bi = random.nextInt(31) - 15;
                int ilo = Math.min(ai, bi), ihi = Math.max(ai, bi);
                long al = (1L << 53) + random.nextInt(71) - 35;
                long bl = (1L << 53) + random.nextInt(71) - 35;
                long llo = Math.min(al, bl), lhi = Math.max(al, bl);
                double ad = doubles.length == 0 ? Double.NaN : doubles[random.nextInt(n)];
                double bd = doubles.length == 0 ? Double.NaN : doubles[random.nextInt(n)];
                double dlo = Double.compare(ad, bd) <= 0 ? ad : bd;
                double dhi = Double.compare(ad, bd) <= 0 ? bd : ad;
                check(Arrays.equals(a.betweenInts(ilo, ihi, 55, ID),
                        reference(valid, 0, ints, longs, doubles, dlo, dhi, llo, lhi, ilo, ihi)),
                        "random exact int oracle " + trial);
                check(Arrays.equals(b.betweenLongs(llo, lhi, 55, ID),
                        reference(valid, 1, ints, longs, doubles, dlo, dhi, llo, lhi, ilo, ihi)),
                        "random exact long oracle " + trial);
                check(Arrays.equals(c.betweenDoubles(dlo, dhi, 55, ID),
                        reference(valid, 2, ints, longs, doubles, dlo, dhi, llo, lhi, ilo, ihi)),
                        "random exact double oracle " + trial);
            }
        }
    }

    public static void main(String[] args) {
        exactEdgeCases();
        seededDifferential();
        System.out.println("M3_SIGNAL_EXACT_RANGE_PASS checks=" + checks);
    }
}
