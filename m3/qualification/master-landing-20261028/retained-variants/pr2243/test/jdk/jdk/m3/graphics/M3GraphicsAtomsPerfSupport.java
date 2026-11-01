/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Timing support of the generated perf harness (M3GraphicsAtomsPerf, M3GraphicsAtomsPerfGroupNN). Each generated row
 * holds two static final MethodHandles of type (Object[])Object, one per side, and two loops that call them; the JIT
 * inlines a static final handle like a direct call. The calls of a row come from M3GraphicsAtomsPerfCalls; a row whose
 * cases diverge is DIVERGENT, and a member returning a URL is ENVIRONMENT (the lookup runs in each side's own class
 * loader). Per row: both loops are first warmed up (at least {@link #WARMUP_CALLS}
 * calls and {@link #WARMUP_NANOS} per side, so the JIT has compiled them), then the passes are calibrated on the
 * compiled loops so a round takes about {@link #ROUND_NANOS}, then {@link #ROUNDS} rounds alternate which side runs
 * first. ns/op per side is the median round; the ratio is M3 over oracle; the spread is the interquartile range of the
 * per-round ratios over their median. Nothing is re-measured. A row above the slack (default 1.5x,
 * {@code -Dm3.graphics.perf.slack=<factor>}) is an OUTLIER and {@link #finish()} fails the harness. Rows whose code
 * is the same on both sides (code SAME) measure the harness itself; their ratio distribution is printed as PERF_SAME.
 */
final class M3GraphicsAtomsPerfSupport {
    /** One side's timed loop: calls every prepared call {@code passes} times and returns a checksum. */
    interface Op {
        long run(Object[][] calls, int passes) throws Throwable;
    }

    private static final int ROUNDS = 11;
    private static final int WARMUP_CALLS = 12;
    private static final long WARMUP_NANOS = 40_000_000L;
    private static final long ROUND_NANOS = 2_000_000L;
    private static final int MIN_PASSES = 1;
    private static final int MAX_PASSES = 1_000_000;
    private static final int ALTERNATE = 2;
    private static final int M3_SIDE = 0;
    private static final int ORACLE_SIDE = 1;
    private static final int SIDES = 2;
    private static final int NS_M3 = 0;
    private static final int NS_ORACLE = 1;
    private static final int RATIO = 2;
    private static final int SPREAD = 3;
    private static final int STATS = 4;
    private static final int SAME_RATIO = 0;
    private static final int SAME_SPREAD = 1;
    private static final double MIN = 0.0;
    private static final double MAX = 1.0;
    private static final double MEDIAN = 0.5;
    private static final double LOWER_QUARTILE = 0.25;
    private static final double UPPER_QUARTILE = 0.75;
    private static final double P10 = 0.1;
    private static final double P90 = 0.9;
    private static final double DEFAULT_SLACK = 1.5;
    private static final String SLACK_PROPERTY = "m3.graphics.perf.slack";
    private static final String SAME_CODE = "SAME";
    private static final String HEADER = "PERF\tatom\tpattern\tmember\tcode\tcalls\tns_per_op_m3\tns_per_op_oracle"
            + "\tratio\tspread\tverdict";
    private static final String ROW = "PERF\t%s\t%s\t%s\t%s\t%d\t%.2f\t%.2f\t%.3f\t%.3f\t%s%n";
    private static final String OK = "OK";
    private static final String OUTLIER = "OUTLIER";
    private static final String EXCEPTION_PATH = "OK_EXCEPTION_PATH";
    private static final String OUTLIER_EXCEPTION_PATH = "OUTLIER_EXCEPTION_PATH";
    private static final String DIVERGENT = "DIVERGENT";
    private static final String ENVIRONMENT = "ENVIRONMENT";
    private static final String NOT_MEASURED = "NOT_MEASURED";
    private static final String SUMMARY = "PERF_SUMMARY\trows=%d\toutliers=%d\tdivergent=%d\tenvironment=%d\tslack=%.2f"
            + "\tchecksum=%d%n";
    private static final String SAME_SUMMARY = "PERF_SAME\trows=%d\tmedian=%.3f\tp10=%.3f\tp90=%.3f\tmin=%.3f"
            + "\tmax=%.3f\tmedian_spread=%.3f%n";
    private static final String SLOWER = "M3 slower than the oracle beyond the slack: ";
    private static final String SEPARATOR = " ";
    private static long checksum;
    private final double slack = Double.parseDouble(System.getProperty(SLACK_PROPERTY, String.valueOf(DEFAULT_SLACK)));
    private final List<String> outliers = new ArrayList<>();
    private final List<double[]> same = new ArrayList<>();
    private int rows;
    private int divergent;
    private int environment;

    /** A member as a handle of type (Object[])Object on one side; an instance member takes the receiver first. */
    static MethodHandle handle(M3GraphicsAtomsOracle.Side side, String atom, M3GraphicsAtoms.Kind kind, String name,
            String descriptor) {
        try {
            Class<?> owner = M3GraphicsAtomsOracle.atom(atom, side);
            Class<?>[] params = M3GraphicsAtomsOracle.types(descriptor, side);
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            MethodHandle h = switch (kind) {
                case CTOR -> l.unreflectConstructor(owner.getConstructor(params));
                case METHOD, STATIC_METHOD -> l.unreflect(owner.getMethod(name, params));
                case FIELD, STATIC_FIELD -> l.unreflectGetter(owner.getField(name));
            };
            MethodHandle fixed = h.asFixedArity();
            int arity = fixed.type().parameterCount();
            return fixed.asType(MethodType.genericMethodType(arity)).asSpreader(Object[].class, arity);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * The blackhole of the timed loops: empty, and the harness runs with
     * {@code -XX:CompileCommand=blackhole,M3GraphicsAtomsPerfSupport::consume}, so the JIT keeps every value handed
     * to it (no dead-code elimination of the measured call) at no cost of its own, the same on both sides.
     */
    static void consume(Object result) {
        // blackhole: intentionally empty
    }

    /** Prints the header row. */
    void header() {
        System.out.println(HEADER);
    }

    /** Measures one atom row: the same calls on both sides through the generated loops. */
    void row(String atom, String pattern, M3GraphicsAtoms.Kind kind, String name, String descriptor, String code,
            Op m3, Op oracle) throws Throwable {
        rows++;
        String member = name + descriptor;
        M3GraphicsAtomsPerfCalls.Calls c = M3GraphicsAtomsPerfCalls.of(atom, kind, name, descriptor);
        boolean env = returnsUrl(atom, kind, name, descriptor);
        if (c.divergent() > 0 || c.m3().length == 0) {
            divergent += c.divergent() > 0 ? 1 : 0;
            double[] none = new double[STATS];
            Arrays.fill(none, Double.NaN);
            print(atom, pattern, member, code, c.m3().length, none, c.divergent() > 0 ? DIVERGENT : NOT_MEASURED);
            return;
        }
        warm(m3, oracle, c);
        double[][] t = measure(m3, oracle, c, passes(m3, oracle, c));
        double[] ratios = IntStream.range(0, ROUNDS).mapToDouble(r -> t[M3_SIDE][r] / t[ORACLE_SIDE][r]).toArray();
        double[] s = new double[STATS];
        s[NS_M3] = quantile(t[M3_SIDE], MEDIAN);
        s[NS_ORACLE] = quantile(t[ORACLE_SIDE], MEDIAN);
        s[RATIO] = s[NS_M3] / s[NS_ORACLE];
        s[SPREAD] = (quantile(ratios, UPPER_QUARTILE) - quantile(ratios, LOWER_QUARTILE)) / quantile(ratios, MEDIAN);
        boolean slow = !env && s[RATIO] > slack;
        outliers.addAll(slow ? List.of(atom + SEPARATOR + member) : List.of());
        environment += env ? 1 : 0;
        if (!env && SAME_CODE.equals(code)) {
            same.add(new double[] {s[RATIO], s[SPREAD]});
        }
        print(atom, pattern, member, code, c.m3().length, s, env ? ENVIRONMENT
                : c.exceptionPath() ? (slow ? OUTLIER_EXCEPTION_PATH : EXCEPTION_PATH) : slow ? OUTLIER : OK);
    }

    /** Prints the summary and the identical-code distribution, and fails when a row is above the slack. */
    void finish() {
        System.out.printf(Locale.ROOT, SUMMARY, rows, outliers.size(), divergent, environment, slack, checksum);
        double[] r = same.stream().mapToDouble(x -> x[SAME_RATIO]).toArray();
        double[] sp = same.stream().mapToDouble(x -> x[SAME_SPREAD]).toArray();
        if (r.length > 0) {
            System.out.printf(Locale.ROOT, SAME_SUMMARY, r.length, quantile(r, MEDIAN), quantile(r, P10),
                    quantile(r, P90), quantile(r, MIN), quantile(r, MAX), quantile(sp, MEDIAN));
        }
        if (!outliers.isEmpty()) {
            throw new AssertionError(SLOWER + outliers);
        }
    }

    private static void print(String atom, String pattern, String member, String code, int calls, double[] s,
            String verdict) {
        System.out.printf(Locale.ROOT, ROW, atom, pattern, member, code, calls, s[NS_M3], s[NS_ORACLE], s[RATIO],
                s[SPREAD], verdict);
    }

    /** The q-quantile of values by nearest rank over the sorted copy (q = 0.5 of an odd count is the median). */
    private static double quantile(double[] values, double q) {
        double[] v = values.clone();
        Arrays.sort(v);
        return v[(int) Math.round(q * (v.length - 1))];
    }

    private static boolean returnsUrl(String atom, M3GraphicsAtoms.Kind kind, String name, String descriptor) {
        try {
            Class<?> owner = M3GraphicsAtomsOracle.atom(atom, M3GraphicsAtomsOracle.Side.M3);
            return (kind == M3GraphicsAtoms.Kind.METHOD || kind == M3GraphicsAtoms.Kind.STATIC_METHOD)
                    && owner.getMethod(name, M3GraphicsAtomsOracle.types(descriptor, M3GraphicsAtomsOracle.Side.M3))
                            .getReturnType() == URL.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** Runs both loops until each has made WARMUP_CALLS calls and spent WARMUP_NANOS; passes grow to ~ROUND_NANOS. */
    private static void warm(Op m3, Op oracle, M3GraphicsAtomsPerfCalls.Calls c) throws Throwable {
        long[] spent = new long[SIDES];
        int passes = MIN_PASSES;
        for (int k = 0; k < WARMUP_CALLS || spent[M3_SIDE] < WARMUP_NANOS || spent[ORACLE_SIDE] < WARMUP_NANOS; k++) {
            double a = time(m3, c.m3(), passes);
            double b = time(oracle, c.oracle(), passes);
            spent[M3_SIDE] += (long) a;
            spent[ORACLE_SIDE] += (long) b;
            passes = Math.max(a, b) < ROUND_NANOS ? Math.min(passes * ALTERNATE, MAX_PASSES) : passes;
        }
    }

    /** Calibration on the compiled loops: doubles the passes until one round of either side takes ROUND_NANOS. */
    private static int passes(Op m3, Op oracle, M3GraphicsAtomsPerfCalls.Calls c) throws Throwable {
        int passes = MIN_PASSES;
        while (passes < MAX_PASSES
                && Math.max(time(m3, c.m3(), passes), time(oracle, c.oracle(), passes)) < ROUND_NANOS) {
            passes *= ALTERNATE;
        }
        return passes;
    }

    /** ROUNDS rounds, the side running first alternating: ns/op per side and round. */
    private static double[][] measure(Op m3, Op oracle, M3GraphicsAtomsPerfCalls.Calls c, int passes) throws Throwable {
        double[][] t = new double[SIDES][ROUNDS];
        long ops = (long) passes * c.m3().length;
        for (int r = 0; r < ROUNDS; r++) {
            boolean m3First = r % ALTERNATE == 0;
            double first = time(m3First ? m3 : oracle, m3First ? c.m3() : c.oracle(), passes) / ops;
            double second = time(m3First ? oracle : m3, m3First ? c.oracle() : c.m3(), passes) / ops;
            t[M3_SIDE][r] = m3First ? first : second;
            t[ORACLE_SIDE][r] = m3First ? second : first;
        }
        return t;
    }

    private static double time(Op op, Object[][] calls, int passes) throws Throwable {
        long t0 = System.nanoTime();
        checksum += op.run(calls, passes);
        return (double) (System.nanoTime() - t0);
    }
}
