/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * The measuring loop shared by the generated perf harnesses: warm-up, interleaved rounds (the order rotates every
 * round so no atom always runs first or after the same neighbour), the median, minimum and maximum ns per operation,
 * and the machine load before and after (other sessions share the host). Output lines are tab-separated:
 * {@code LOAD <phase> <system cpu> <process cpu> <processors>} and
 * {@code PERF <atom> <label> <route> <median ns> <min ns> <max ns> <ops per round> <rounds>}.
 */
final class PerfSupport {

    static final int WARMUP_ROUNDS = Integer.getInteger("perf.warmup", 5);
    static final int ROUNDS = Integer.getInteger("perf.rounds", 7);
    /** Operations per round by cost class: a round of one atom takes milliseconds, not microseconds. */
    static final int FAST = 1 << 20;
    static final int MEDIUM = 1 << 16;
    static final int SLOW = 1 << 11;
    static final int HEAVY = 1 << 6;
    /** Index streams are this long (a power of two) so the loops mask instead of dividing. */
    static final int STREAM = 1 << 12;
    static final int MASK = STREAM - 1;
    static final double PERCENT = 100.0;
    static final double NANOS = 1.0;
    /** The CPU load is sampled over this many milliseconds of this JVM sleeping: what the other sessions use. */
    static final long LOAD_SAMPLE_MS = 500;

    interface Body {
        long run(int ops) throws Throwable;
    }

    record Bench(String atom, String label, String route, int ops, Body body) {
    }

    private PerfSupport() {
    }

    /** A constructor handle, private or not. */
    static MethodHandle ctor(Class<?> owner, Class<?>... params) {
        try {
            return AtomSupport.lookupIn(owner).findConstructor(owner, MethodType.methodType(void.class, params));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(owner.getName() + ".<init>", e);
        }
    }

    /** {@code h} with the static types a call site can name (a private nested receiver as its public interface). */
    static MethodHandle adapt(MethodHandle h, Class<?> returns, Class<?>... params) {
        return h.asType(MethodType.methodType(returns, params));
    }

    static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    static String load(String phase) {
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double system = -1;
        double process = -1;
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            system = sun.getCpuLoad() * PERCENT;
            process = sun.getProcessCpuLoad() * PERCENT;
        }
        return String.format("LOAD\t%s\t%.1f\t%.1f\t%d", phase, system, process, os.getAvailableProcessors());
    }

    static long measure(Bench[] benches) throws Throwable {
        long sink = 0;
        load("prime");
        Thread.sleep(LOAD_SAMPLE_MS);
        System.out.println(load("start"));
        for (int w = 0; w < WARMUP_ROUNDS; w++) {
            for (Bench b : benches) {
                sink += b.body().run(b.ops());
            }
        }
        System.out.println(load("warm"));
        double[][] ns = new double[benches.length][ROUNDS];
        for (int r = 0; r < ROUNDS; r++) {
            for (int j = 0; j < benches.length; j++) {
                int i = (j + r) % benches.length;
                Bench b = benches[i];
                long t0 = System.nanoTime();
                sink += b.body().run(b.ops());
                ns[i][r] = (System.nanoTime() - t0) * NANOS / b.ops();
            }
        }
        for (int i = 0; i < benches.length; i++) {
            double[] s = ns[i].clone();
            Arrays.sort(s);
            Bench b = benches[i];
            System.out.println(String.format("PERF\t%s\t%s\t%s\t%.3f\t%.3f\t%.3f\t%d\t%d", b.atom(), b.label(), b.route(),
                    s[s.length / 2], s[0], s[s.length - 1], b.ops(), ROUNDS));
        }
        Thread.sleep(LOAD_SAMPLE_MS);
        System.out.println(load("end"));
        return sink;
    }
}
