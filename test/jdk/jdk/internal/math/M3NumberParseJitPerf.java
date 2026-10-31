/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary JIT and perf stage of the number-parse leaf: a warm Double.parseDouble / Float.parseFloat loop over five
 *          input classes (decimal, hex, short, long, subnormal; double literals for parseDouble, float literals for
 *          parseFloat; fixed seeds) through static final MethodHandles (invokeExact). Warm-up rounds run before any
 *          timing; then one row per input class and operation with the
 *          median ns/op of the wiring, the median ns/op of the JDK's original converter
 *          (FloatingDecimal.readJavaFormatString) on the same inputs, their ratio and the system CPU load sampled
 *          around the measured rounds (other sessions keep the host busy). Only public API plus the original
 *          converter is used, so the same class runs on the stock JDK and on a patched java.base. Run with
 *          -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining it is the JIT probe; its timings
 *          are then not used.
 * @modules java.base/jdk.internal.math:open
 * @run main/othervm -Xmx1g M3NumberParseJitPerf
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

public class M3NumberParseJitPerf {
    /** Seed of every input class (the leaf's fixed seed). */
    private static final long SEED = 20261031L;
    /** Inputs per class. */
    private static final int INPUTS = 20_000;
    /** Untimed rounds per row before the measured ones (tier 4 compiles of the whole parse path). */
    private static final int WARMUP = 12;
    /** Measured rounds per row; the row reports their median. */
    private static final int ROUNDS = 9;
    /** Significand digits of the LONG class: more than 19, so the truncated path runs. */
    private static final int LONG_MIN_DIGITS = 20;
    /** Extra significand digits of the LONG class (20 to 39 digits). */
    private static final int LONG_EXTRA_DIGITS = 20;
    /** Largest value of a SHORT input before scaling. */
    private static final int SHORT_BOUND = 100_000;
    /** Scale of a SHORT input (two decimals). */
    private static final double SHORT_SCALE = 100.0;
    /** Mask of the 52 fraction bits: a double with a zero exponent field is subnormal (or zero). */
    private static final long DOUBLE_FRACTION_MASK = 0x000f_ffff_ffff_ffffL;
    /** Mask of the 23 fraction bits: a float with a zero exponent field is subnormal (or zero). */
    private static final int FLOAT_FRACTION_MASK = 0x007f_ffff;
    /** Decimal exponents of a double LONG input: DOUBLE_EXPONENT_MIN .. DOUBLE_EXPONENT_MIN + DOUBLE_EXPONENT_SPAN - 1. */
    private static final int DOUBLE_EXPONENT_MIN = -300;
    private static final int DOUBLE_EXPONENT_SPAN = 600;
    /** Decimal exponents of a float LONG input (the literal has 20 to 39 digits; most values stay in float range). */
    private static final int FLOAT_EXPONENT_MIN = -40;
    private static final int FLOAT_EXPONENT_SPAN = 60;
    private static final String[] CLASSES = {"decimal", "hex", "short", "long", "subnormal"};
    private static final com.sun.management.OperatingSystemMXBean OS =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static long sink;

    private static final MethodHandle PARSE_DOUBLE, PARSE_FLOAT, READ, DV, FV;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            PARSE_DOUBLE = l.findStatic(Double.class, "parseDouble", MethodType.methodType(double.class, String.class));
            PARSE_FLOAT = l.findStatic(Float.class, "parseFloat", MethodType.methodType(float.class, String.class));
            Class<?> fd = Class.forName("jdk.internal.math.FloatingDecimal");
            Class<?> conv = Class.forName("jdk.internal.math.FloatingDecimal$ASCIIToBinaryConverter");
            READ = MethodHandles.privateLookupIn(fd, l)
                    .findStatic(fd, "readJavaFormatString", MethodType.methodType(conv, String.class))
                    .asType(MethodType.methodType(Object.class, String.class));
            MethodHandles.Lookup lc = MethodHandles.privateLookupIn(conv, l);
            DV = lc.findVirtual(conv, "doubleValue", MethodType.methodType(double.class))
                    .asType(MethodType.methodType(double.class, Object.class));
            FV = lc.findVirtual(conv, "floatValue", MethodType.methodType(float.class))
                    .asType(MethodType.methodType(float.class, Object.class));
            OS.getCpuLoad(); // the first sample only primes the counters
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** The inputs of one class for double or float literals, deterministic for the seed. */
    static String[] inputs(String kind, boolean asFloat) {
        SplittableRandom r = new SplittableRandom(SEED + kind.hashCode() + (asFloat ? 1 : 0));
        List<String> out = new ArrayList<>(INPUTS);
        while (out.size() < INPUTS) {
            out.add(switch (kind) {
                case "decimal" -> asFloat ? Float.toString(Float.intBitsToFloat(r.nextInt() & Integer.MAX_VALUE))
                                          : Double.toString(Double.longBitsToDouble(r.nextLong() & Long.MAX_VALUE));
                case "hex" -> asFloat ? Float.toHexString(Float.intBitsToFloat(r.nextInt()))
                                      : Double.toHexString(Double.longBitsToDouble(r.nextLong()));
                case "short" -> Double.toString(r.nextInt(SHORT_BOUND) / SHORT_SCALE);
                case "long" -> asFloat ? longLiteral(r, FLOAT_EXPONENT_MIN, FLOAT_EXPONENT_SPAN)
                                       : longLiteral(r, DOUBLE_EXPONENT_MIN, DOUBLE_EXPONENT_SPAN);
                default -> asFloat ? Float.toString(Float.intBitsToFloat(r.nextInt() & FLOAT_FRACTION_MASK))
                                   : Double.toString(Double.longBitsToDouble(r.nextLong() & DOUBLE_FRACTION_MASK));
            });
        }
        out.removeIf(s -> s.contains("Infinity") || s.contains("NaN"));
        return out.toArray(String[]::new);
    }

    private static String longLiteral(SplittableRandom r, int exponentMin, int exponentSpan) {
        StringBuilder b = new StringBuilder();
        int digits = LONG_MIN_DIGITS + r.nextInt(LONG_EXTRA_DIGITS);
        b.append((char) ('1' + r.nextInt(9)));
        for (int i = 1; i < digits; i++) {
            b.append((char) ('0' + r.nextInt(10)));
        }
        b.insert(1 + r.nextInt(digits - 1), '.');
        return b.append('e').append(exponentMin + r.nextInt(exponentSpan)).toString();
    }

    interface Op {
        long run(String[] in) throws Throwable;
    }

    static long parseDoubles(String[] in) throws Throwable {
        long s = 0;
        for (String x : in) {
            s += Double.doubleToRawLongBits((double) PARSE_DOUBLE.invokeExact(x));
        }
        return s;
    }

    static long parseFloats(String[] in) throws Throwable {
        long s = 0;
        for (String x : in) {
            s += Float.floatToRawIntBits((float) PARSE_FLOAT.invokeExact(x));
        }
        return s;
    }

    static long referenceDoubles(String[] in) throws Throwable {
        long s = 0;
        for (String x : in) {
            s += Double.doubleToRawLongBits((double) DV.invokeExact((Object) READ.invokeExact(x)));
        }
        return s;
    }

    static long referenceFloats(String[] in) throws Throwable {
        long s = 0;
        for (String x : in) {
            s += Float.floatToRawIntBits((float) FV.invokeExact((Object) READ.invokeExact(x)));
        }
        return s;
    }

    /** Median ns/op over the measured rounds, wiring and reference interleaved round by round. */
    static double[] measure(Op atom, Op ref, String[] in) throws Throwable {
        double[] a = new double[ROUNDS], b = new double[ROUNDS];
        for (int r = 0; r < WARMUP; r++) {
            sink += atom.run(in) + ref.run(in);
        }
        double load0 = OS.getCpuLoad();
        for (int r = 0; r < ROUNDS; r++) {
            long t0 = System.nanoTime();
            sink += atom.run(in);
            long t1 = System.nanoTime();
            sink += ref.run(in);
            long t2 = System.nanoTime();
            a[r] = (double) (t1 - t0) / in.length;
            b[r] = (double) (t2 - t1) / in.length;
        }
        double load = OS.getCpuLoad();
        Arrays.sort(a);
        Arrays.sort(b);
        return new double[] {a[ROUNDS / 2], b[ROUNDS / 2], load0, load};
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("atom\tns_per_op_atom\tns_per_op_reference\tspeedup\tsystem_cpu_load");
        for (String kind : CLASSES) {
            row("Double.parseDouble " + kind + " (wiring)", measure(M3NumberParseJitPerf::parseDoubles,
                    M3NumberParseJitPerf::referenceDoubles, inputs(kind, false)));
            row("Float.parseFloat " + kind + " (wiring)", measure(M3NumberParseJitPerf::parseFloats,
                    M3NumberParseJitPerf::referenceFloats, inputs(kind, true)));
        }
        System.out.println("sink " + sink);
    }

    private static void row(String atom, double[] m) {
        System.out.printf("%s\t%.2f\t%.2f\t%.3f\t%.3f%n", atom, m[0], m[1], m[1] / m[0], m[3]);
    }
}
