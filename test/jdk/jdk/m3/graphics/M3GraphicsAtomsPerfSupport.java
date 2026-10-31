/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Timing support of the generated perf harness (M3GraphicsAtomsPerf, M3GraphicsAtomsPerfGroupNN). Each generated row
 * holds two static final MethodHandles of type (Object[])Object, one per side, and two loops that call them; the JIT
 * inlines a static final handle like a direct call. The calls of a row are the first cases of the atom's tests
 * (M3GraphicsAtoms.logicalCase), materialized once per side and probed once per side. Cases that return on both sides
 * are timed; when there is none, cases that throw the same type on both sides are timed (exception path). A case
 * whose outcome differs between the sides makes the row DIVERGENT (the tests report the difference; timing different
 * behaviour says nothing), and a member returning a URL is ENVIRONMENT (the lookup runs in each side's own class
 * loader). The number of passes is calibrated so a round takes about {@link #ROUND_NANOS}; rounds alternate which
 * side runs first; after the warm-up rounds the best round per side gives ns/op. A row whose M3 time exceeds the
 * oracle time by more than the slack (default 1.5x, {@code -Dm3.graphics.perf.slack=<factor>}) is measured again up
 * to {@link #RETRIES} times; a row still above it is an OUTLIER, and {@link #finish()} fails the harness when there
 * is one.
 */
final class M3GraphicsAtomsPerfSupport {
    /** One side's timed loop: calls every prepared call {@code passes} times and returns a checksum. */
    interface Op {
        long run(Object[][] calls, int passes) throws Throwable;
    }

    /** The prepared calls of a row per side, how they behave, and how many cases diverged. */
    record Calls(Object[][] m3, Object[][] oracle, boolean exceptionPath, int divergent) {}

    private static final int WARMUP = 5;
    private static final int ROUNDS = 7;
    private static final int RETRIES = 3;
    private static final int MAX_CALLS = 64;
    private static final long ROUND_NANOS = 2_000_000L;
    private static final int MIN_PASSES = 1;
    private static final int MAX_PASSES = 1_000_000;
    private static final int ALTERNATE = 2;
    private static final int M3_SIDE = 0;
    private static final int ORACLE_SIDE = 1;
    private static final double DEFAULT_SLACK = 1.5;
    private static final String SLACK_PROPERTY = "m3.graphics.perf.slack";
    private static final String HEADER = "PERF\tatom\tpattern\tmember\tcalls\tns_per_op_m3\tns_per_op_oracle"
            + "\tratio\tverdict";
    private static final String ROW = "PERF\t%s\t%s\t%s\t%d\t%.2f\t%.2f\t%.3f\t%s%n";
    private static final String OK = "OK";
    private static final String OUTLIER = "OUTLIER";
    private static final String EXCEPTION_PATH = "OK_EXCEPTION_PATH";
    private static final String OUTLIER_EXCEPTION_PATH = "OUTLIER_EXCEPTION_PATH";
    private static final String DIVERGENT = "DIVERGENT";
    private static final String ENVIRONMENT = "ENVIRONMENT";
    private static final String NOT_MEASURED = "NOT_MEASURED";
    private static final String SUMMARY = "PERF_SUMMARY\trows=%d\toutliers=%d\tdivergent=%d\tenvironment=%d\tslack=%.2f"
            + "\tchecksum=%d%n";
    private static final String SLOWER = "M3 slower than the oracle beyond the slack: ";
    private static final String SEPARATOR = " ";
    private static long checksum;
    private final double slack = Double.parseDouble(System.getProperty(SLACK_PROPERTY, String.valueOf(DEFAULT_SLACK)));
    private final List<String> outliers = new ArrayList<>();
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
    void row(String atom, String pattern, M3GraphicsAtoms.Kind kind, String name, String descriptor, Op m3, Op oracle)
            throws Throwable {
        rows++;
        String member = name + descriptor;
        MethodHandle probeM3 = handle(M3GraphicsAtomsOracle.Side.M3, atom, kind, name, descriptor);
        Calls c = calls(atom, kind, name, descriptor, probeM3);
        boolean env = returnsUrl(atom, kind, name, descriptor);
        if (c.divergent() > 0 || c.m3().length == 0) {
            divergent += c.divergent() > 0 ? 1 : 0;
            print(atom, pattern, member, c.m3().length, Double.NaN, Double.NaN,
                    c.divergent() > 0 ? DIVERGENT : NOT_MEASURED);
            return;
        }
        int passes = passes(m3, oracle, c);
        double[] best = measure(m3, oracle, c, passes);
        for (int r = 0; r < RETRIES && !env && best[M3_SIDE] > best[ORACLE_SIDE] * slack; r++) {
            double[] again = measure(m3, oracle, c, passes);
            best = again[M3_SIDE] / again[ORACLE_SIDE] < best[M3_SIDE] / best[ORACLE_SIDE] ? again : best;
        }
        boolean slow = !env && best[M3_SIDE] > best[ORACLE_SIDE] * slack;
        if (slow) {
            outliers.add(atom + SEPARATOR + member);
        }
        environment += env ? 1 : 0;
        String verdict = env ? ENVIRONMENT : c.exceptionPath() ? (slow ? OUTLIER_EXCEPTION_PATH : EXCEPTION_PATH)
                : slow ? OUTLIER : OK;
        print(atom, pattern, member, c.m3().length, best[M3_SIDE], best[ORACLE_SIDE], verdict);
    }

    /** Prints the summary and fails when a row stayed above the slack. */
    void finish() {
        System.out.printf(Locale.ROOT, SUMMARY, rows, outliers.size(), divergent, environment, slack, checksum);
        if (!outliers.isEmpty()) {
            throw new AssertionError(SLOWER + outliers);
        }
    }

    private static void print(String atom, String pattern, String member, int calls, double m3, double oracle,
            String verdict) {
        System.out.printf(Locale.ROOT, ROW, atom, pattern, member, calls, m3, oracle, m3 / oracle, verdict);
    }

    private static boolean returnsUrl(String atom, M3GraphicsAtoms.Kind kind, String name, String descriptor) {
        try {
            Class<?> owner = M3GraphicsAtomsOracle.atom(atom, M3GraphicsAtomsOracle.Side.M3);
            return kind == M3GraphicsAtoms.Kind.METHOD || kind == M3GraphicsAtoms.Kind.STATIC_METHOD
                    ? owner.getMethod(name, M3GraphicsAtomsOracle.types(descriptor, M3GraphicsAtomsOracle.Side.M3))
                            .getReturnType() == URL.class : false;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** Doubles the passes until one round of either side takes ROUND_NANOS; this also warms both loops up. */
    private static int passes(Op m3, Op oracle, Calls c) throws Throwable {
        int passes = MIN_PASSES;
        while (passes < MAX_PASSES
                && Math.max(time(m3, c.m3(), passes), time(oracle, c.oracle(), passes)) < ROUND_NANOS) {
            passes *= ALTERNATE;
        }
        return passes;
    }

    private double[] measure(Op m3, Op oracle, Calls c, int passes) throws Throwable {
        double[] best = {Double.MAX_VALUE, Double.MAX_VALUE};
        long ops = (long) passes * c.m3().length;
        for (int r = 0; r < WARMUP + ROUNDS; r++) {
            boolean m3First = r % ALTERNATE == 0;
            double first = time(m3First ? m3 : oracle, m3First ? c.m3() : c.oracle(), passes) / ops;
            double second = time(m3First ? oracle : m3, m3First ? c.oracle() : c.m3(), passes) / ops;
            if (r >= WARMUP) {
                best[M3_SIDE] = Math.min(best[M3_SIDE], m3First ? first : second);
                best[ORACLE_SIDE] = Math.min(best[ORACLE_SIDE], m3First ? second : first);
            }
        }
        return best;
    }

    private static double time(Op op, Object[][] calls, int passes) throws Throwable {
        long t0 = System.nanoTime();
        checksum += op.run(calls, passes);
        return (double) (System.nanoTime() - t0);
    }

    private static Calls calls(String atom, M3GraphicsAtoms.Kind kind, String name, String descriptor,
            MethodHandle m3) {
        List<String> tokens = M3GraphicsAtomsOracle.params(descriptor);
        String key = M3GraphicsAtoms.key(atom, name, descriptor);
        int n = Math.min(MAX_CALLS, M3GraphicsAtoms.cases(kind, tokens.size(), M3GraphicsAtomsValues.constants(atom)));
        MethodHandle oracle = handle(M3GraphicsAtomsOracle.Side.ORACLE, atom, kind, name, descriptor);
        List<Object[]> returnsM3 = new ArrayList<>();
        List<Object[]> returnsOracle = new ArrayList<>();
        List<Object[]> throwsM3 = new ArrayList<>();
        List<Object[]> throwsOracle = new ArrayList<>();
        int[] divergent = {0};
        IntStream.range(0, n).forEach(i -> {
            List<M3GraphicsAtomsValues.Logical> l = M3GraphicsAtoms.logicalCase(atom, kind, key, tokens, i);
            Optional<Throwable> a = probe(l, kind, M3GraphicsAtomsOracle.Side.M3, m3);
            Optional<Throwable> b = probe(l, kind, M3GraphicsAtomsOracle.Side.ORACLE, oracle);
            boolean same = a.isEmpty() == b.isEmpty() && (a.isEmpty()
                    || M3GraphicsAtomsCompare.sameType(a.get().getClass(), b.get().getClass()));
            divergent[0] += same ? 0 : 1;
            List<Object[]> toM3 = a.isEmpty() ? returnsM3 : throwsM3;
            List<Object[]> toOracle = a.isEmpty() ? returnsOracle : throwsOracle;
            prepared(l, kind, M3GraphicsAtomsOracle.Side.M3).filter(x -> same).ifPresent(x -> {
                prepared(l, kind, M3GraphicsAtomsOracle.Side.ORACLE).ifPresent(y -> {
                    toM3.add(x);
                    toOracle.add(y);
                });
            });
        });
        boolean exceptionPath = returnsM3.isEmpty();
        List<Object[]> m3Calls = exceptionPath ? throwsM3 : returnsM3;
        List<Object[]> oracleCalls = exceptionPath ? throwsOracle : returnsOracle;
        return new Calls(m3Calls.toArray(Object[][]::new), oracleCalls.toArray(Object[][]::new), exceptionPath,
                divergent[0]);
    }

    private static Optional<Throwable> probe(List<M3GraphicsAtomsValues.Logical> l, M3GraphicsAtoms.Kind kind,
            M3GraphicsAtomsOracle.Side side, MethodHandle h) {
        try {
            Object unused = h.invokeExact(materialize(l, kind, side));
            return Optional.empty();
        } catch (InvocationTargetException e) {
            return Optional.of(e.getCause());
        } catch (Throwable t) {
            return Optional.of(t);
        }
    }

    private static Optional<Object[]> prepared(List<M3GraphicsAtomsValues.Logical> l, M3GraphicsAtoms.Kind kind,
            M3GraphicsAtomsOracle.Side side) {
        try {
            return Optional.of(materialize(l, kind, side));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return Optional.empty();
        }
    }

    private static Object[] materialize(List<M3GraphicsAtomsValues.Logical> l, M3GraphicsAtoms.Kind kind,
            M3GraphicsAtomsOracle.Side side) throws ReflectiveOperationException {
        boolean instance = kind == M3GraphicsAtoms.Kind.METHOD || kind == M3GraphicsAtoms.Kind.FIELD;
        List<M3GraphicsAtomsValues.Logical> used = instance ? l : l.subList(1, l.size());
        Object[] call = new Object[used.size()];
        for (int p = 0; p < call.length; p++) {
            call[p] = M3GraphicsAtomsValues.materialize(used.get(p), side);
        }
        return call;
    }
}
