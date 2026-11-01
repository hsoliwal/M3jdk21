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
 * per-round ratios over their median. Nothing is re-measured. Rows whose code is the same on both sides (code SAME)
 * measure the harness itself: they are reported as NOISE and never fail, and their ratio distribution is the run's
 * noise (PERF_SAME); its largest ratio is the noise ceiling. A DIFF row fails ({@link #finish()}, OUTLIER) only above
 * the threshold max(slack, noise ceiling), slack default 1.5x ({@code -Dm3.graphics.perf.slack=<factor>}); between
 * the slack and the ceiling it is OK_NOISE_BAND. With fewer than {@link #MIN_NOISE_ROWS} SAME rows the threshold is
 * the slack. Rows are printed when the run's noise is known, at the end.
 */
final class M3GraphicsAtomsPerfSupport {
    /** One side's timed loop: calls every prepared call {@code passes} times and returns a checksum. */
    interface Op {
        long run(Object[][] calls, int passes) throws Throwable;
    }

    /** A measured row, printed by {@link #finish()}: verdict is set for rows that were not timed. */
    private record Row(String atom, String pattern, String member, String code, int calls, double[] stats,
            boolean exceptionPath, String verdict) {}

    private static final int MIN_NOISE_ROWS = 20;
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
    private static final String NOISE = "NOISE";
    private static final String NOISE_BAND = "OK_NOISE_BAND";
    private static final String EXCEPTION_SUFFIX = "_EXCEPTION_PATH";
    private static final String DIVERGENT = "DIVERGENT";
    private static final String ENVIRONMENT = "ENVIRONMENT";
    private static final String NOT_MEASURED = "NOT_MEASURED";
    private static final String SUMMARY = "PERF_SUMMARY\trows=%d\toutliers=%d\tdivergent=%d\tenvironment=%d\tslack=%.2f"
            + "\tnoise_ceiling=%.3f\tthreshold=%.3f\tdiff_rows=%d\tnoise_band=%d\tchecksum=%d%n";
    private static final String SAME_SUMMARY = "PERF_SAME\trows=%d\tmedian=%.3f\tp10=%.3f\tp90=%.3f\tmin=%.3f"
            + "\tmax=%.3f\tmedian_spread=%.3f%n";
    private static final String SLOWER = "M3 slower than the oracle beyond max(slack, SAME-code noise ceiling): ";
    private static final String SEPARATOR = " ";
    private static long checksum;
    private final double slack = Double.parseDouble(System.getProperty(SLACK_PROPERTY, String.valueOf(DEFAULT_SLACK)));
    private final List<Row> measured = new ArrayList<>();

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
        String member = name + descriptor;
        M3GraphicsAtomsPerfCalls.Calls c = M3GraphicsAtomsPerfCalls.of(atom, kind, name, descriptor);
        double[] s = new double[STATS];
        Arrays.fill(s, Double.NaN);
        if (c.divergent() > 0 || c.m3().length == 0) {
            measured.add(new Row(atom, pattern, member, code, c.m3().length, s, false,
                    c.divergent() > 0 ? DIVERGENT : NOT_MEASURED));
            return;
        }
        warm(m3, oracle, c);
        double[][] t = measure(m3, oracle, c, passes(m3, oracle, c));
        double[] ratios = IntStream.range(0, ROUNDS).mapToDouble(r -> t[M3_SIDE][r] / t[ORACLE_SIDE][r]).toArray();
        s[NS_M3] = quantile(t[M3_SIDE], MEDIAN);
        s[NS_ORACLE] = quantile(t[ORACLE_SIDE], MEDIAN);
        s[RATIO] = s[NS_M3] / s[NS_ORACLE];
        s[SPREAD] = (quantile(ratios, UPPER_QUARTILE) - quantile(ratios, LOWER_QUARTILE)) / quantile(ratios, MEDIAN);
        boolean env = returnsUrl(atom, kind, name, descriptor);
        measured.add(new Row(atom, pattern, member, code, c.m3().length, s, c.exceptionPath(), env ? ENVIRONMENT
                : null));
    }

    //FLLDCIM: BLLSE - v3 perf gate (#2243 verifier c): SAME rows are the run's noise; DIFF rows gated above it
    /**
     * Prints every row with its verdict, the summary and the SAME-code noise distribution, and fails when a DIFF row
     * is above max(slack, noise ceiling).
     */
    void finish() {
        List<Row> noise = measured.stream().filter(r -> r.verdict() == null && SAME_CODE.equals(r.code())).toList();
        double[] r = noise.stream().mapToDouble(x -> x.stats()[RATIO]).toArray();
        double ceiling = r.length >= MIN_NOISE_ROWS ? quantile(r, MAX) : Double.NaN;
        double threshold = Double.isNaN(ceiling) ? slack : Math.max(slack, ceiling);
        List<String> verdicts = measured.stream().map(x -> verdict(x, threshold)).toList();
        IntStream.range(0, measured.size()).forEach(i -> print(measured.get(i), verdicts.get(i)));
        List<String> outliers = IntStream.range(0, measured.size()).filter(i -> verdicts.get(i).startsWith(OUTLIER))
                .mapToObj(i -> measured.get(i).atom() + SEPARATOR + measured.get(i).member()).toList();
        System.out.printf(Locale.ROOT, SUMMARY, measured.size(), outliers.size(), count(verdicts, DIVERGENT),
                count(verdicts, ENVIRONMENT), slack, ceiling, threshold,
                measured.stream().filter(x -> x.verdict() == null && !SAME_CODE.equals(x.code())).count(),
                count(verdicts, NOISE_BAND), checksum);
        if (r.length > 0) {
            double[] sp = noise.stream().mapToDouble(x -> x.stats()[SPREAD]).toArray();
            System.out.printf(Locale.ROOT, SAME_SUMMARY, r.length, quantile(r, MEDIAN), quantile(r, P10),
                    quantile(r, P90), quantile(r, MIN), quantile(r, MAX), quantile(sp, MEDIAN));
        }
        if (!outliers.isEmpty()) {
            throw new AssertionError(SLOWER + threshold + SEPARATOR + outliers);
        }
    }

    /** NOISE for SAME code; for DIFF code OUTLIER above the threshold, OK_NOISE_BAND above the slack, else OK. */
    private String verdict(Row x, double threshold) {
        if (x.verdict() != null) {
            return x.verdict();
        }
        double ratio = x.stats()[RATIO];
        String base = SAME_CODE.equals(x.code()) ? NOISE : ratio > threshold ? OUTLIER : ratio > slack ? NOISE_BAND : OK;
        return x.exceptionPath() ? base + EXCEPTION_SUFFIX : base;
    }

    private static long count(List<String> verdicts, String prefix) {
        return verdicts.stream().filter(v -> v.startsWith(prefix)).count();
    }

    private static void print(Row x, String verdict) {
        double[] s = x.stats();
        System.out.printf(Locale.ROOT, ROW, x.atom(), x.pattern(), x.member(), x.code(), x.calls(), s[NS_M3],
                s[NS_ORACLE], s[RATIO], s[SPREAD], verdict);
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
