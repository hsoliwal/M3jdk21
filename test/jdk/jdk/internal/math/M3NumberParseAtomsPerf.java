/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One performance measurement per atom of the FastDoubleParser absorption (jdk.internal.math.M3*): each atom
 *          and its JDK reference run on the same deterministic inputs through static final MethodHandles (invokeExact,
 *          inlined by the JIT like direct calls), warm-up rounds first, best of the measured rounds. One TSV row per
 *          atom: atom, ns/op atom, ns/op reference, speedup. The guard: the public wiring (Double.parseDouble on valid
 *          literals) must not be slower than the original converter by more than M3_PERF_SLACK (default 1.25); the
 *          other rows are evidence (timing is host-dependent).
 * @modules java.base/jdk.internal.math:open
 * @run main/othervm -Xmx1g M3NumberParseAtomsPerf
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public class M3NumberParseAtomsPerf {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Seed of the deterministic literal corpus. */
    private static final long CORPUS_SEED = 20261031L;
    /** Literals in the corpus. */
    private static final int CORPUS_SIZE = 200_000;
    /** One corpus literal in FLOAT_LITERAL_ONE_IN is a Float.toString literal, the others Double.toString. */
    private static final int FLOAT_LITERAL_ONE_IN = 3;
    /** Masks random bits to a positive finite float (no sign bit, exponent below 0xff). */
    private static final int FINITE_FLOAT_BITS_MASK = 0x7f7fffff;
    /** Masks random bits to a positive finite double (no sign bit, exponent below 0x7ff). */
    private static final long FINITE_DOUBLE_BITS_MASK = 0x7fefffffffffffffL;
    /** Calls per timing of the SWAR and multiply kernels. */
    private static final int KERNEL_OPS = 5_000_000;
    /** The first decimal digit (the SWAR input cycles from it; the reference checks from it). */
    private static final char DIGIT_ZERO = '0';
    /** Masks the loop index to eight values, so the first byte of the SWAR input cycles through eight digits. */
    private static final int DIGIT_CYCLE_MASK = 7;
    /** Bytes one isEightDigits call checks; the reference loop checks as many. */
    private static final int SWAR_WORD_BYTES = 8;
    /** The last decimal digit. */
    private static final char DIGIT_NINE = '9';
    /** The 64-bit golden-ratio constant (SplitMix64 gamma): i * GOLDEN_GAMMA spreads the loop index over all bits. */
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    /** Calls per timing of the decimal fast-path kernels. */
    private static final int FAST_MATH_OPS = 300_000;
    /** A prime: the significand i * SIGNIFICAND_STRIDE walks the significands without a pattern. */
    private static final int SIGNIFICAND_STRIDE = 7919;
    /** The double kernel's decimal powers cycle through DOUBLE_POWER_SPAN values. */
    private static final int DOUBLE_POWER_SPAN = 300;
    /** The double kernel's decimal powers start at -DOUBLE_POWER_OFFSET. */
    private static final int DOUBLE_POWER_OFFSET = 150;
    /** The float kernel's decimal powers cycle through FLOAT_POWER_SPAN values. */
    private static final int FLOAT_POWER_SPAN = 70;
    /** The float kernel's decimal powers start at -FLOAT_POWER_OFFSET. */
    private static final int FLOAT_POWER_OFFSET = 40;
    /** Bit length of each operand of the FFT multiply timing. */
    private static final int FFT_OPERAND_BITS = 400_000;
    /** Seed of the second FFT operand (the first uses 1). */
    private static final int SECOND_OPERAND_SEED = 2;

    private static final int WARMUP = 6;
    private static final int ROUNDS = 7;
    private static long sink;

    private static final MethodHandle EIGHT, READ_BE, UMH, FDM, FFM, MUL, READ, DV, FV;
    private static final MethodHandle P_DS, P_FS, P_DB, P_FB;
    private static final Object I_DS, I_FS, I_DB, I_FB;

    static {
        try {
            EIGHT = sh("M3FastDoubleSwar", "isEightDigits", MethodType.methodType(boolean.class, byte[].class, int.class));
            READ_BE = sh("M3FastDoubleSwar", "readLongBE", MethodType.methodType(long.class, byte[].class, int.class));
            UMH = sh("M3FastIntegerMath", "unsignedMultiplyHigh", MethodType.methodType(long.class, long.class, long.class));
            FDM = sh("M3FastDoubleMath", "tryDecToDoubleWithFastAlgorithm", MethodType.methodType(double.class, boolean.class, long.class, int.class));
            FFM = sh("M3FastFloatMath", "tryDecToFloatWithFastAlgorithm", MethodType.methodType(float.class, boolean.class, long.class, int.class));
            MUL = sh("M3FftMultiplier", "multiply", MethodType.methodType(BigInteger.class, BigInteger.class, BigInteger.class));
            Class<?> fd = Class.forName("jdk.internal.math.FloatingDecimal");
            Class<?> conv = Class.forName("jdk.internal.math.FloatingDecimal$ASCIIToBinaryConverter");
            READ = MethodHandles.privateLookupIn(fd, MethodHandles.lookup())
                    .findStatic(fd, "readJavaFormatString", MethodType.methodType(conv, String.class))
                    .asType(MethodType.methodType(Object.class, String.class));
            MethodHandles.Lookup lc = MethodHandles.privateLookupIn(conv, MethodHandles.lookup());
            DV = lc.findVirtual(conv, "doubleValue", MethodType.methodType(double.class)).asType(MethodType.methodType(double.class, Object.class));
            FV = lc.findVirtual(conv, "floatValue", MethodType.methodType(float.class)).asType(MethodType.methodType(float.class, Object.class));
            P_DS = parser("M3JavaDoubleBitsFromCharSequence", false);
            P_FS = parser("M3JavaFloatBitsFromCharSequence", false);
            P_DB = parser("M3JavaDoubleBitsFromByteArray", true);
            P_FB = parser("M3JavaFloatBitsFromByteArray", true);
            I_DS = inst("M3JavaDoubleBitsFromCharSequence");
            I_FS = inst("M3JavaFloatBitsFromCharSequence");
            I_DB = inst("M3JavaDoubleBitsFromByteArray");
            I_FB = inst("M3JavaFloatBitsFromByteArray");
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle sh(String type, String name, MethodType t) throws ReflectiveOperationException {
        Class<?> c = Class.forName("jdk.internal.math." + type);
        return MethodHandles.privateLookupIn(c, MethodHandles.lookup()).findStatic(c, name, t);
    }

    private static MethodHandle parser(String type, boolean bytes) throws ReflectiveOperationException {
        Class<?> c = Class.forName("jdk.internal.math." + type);
        Class<?> in = bytes ? byte[].class : CharSequence.class;
        return MethodHandles.privateLookupIn(c, MethodHandles.lookup())
                .findVirtual(c, "parseFloatingPointLiteral", MethodType.methodType(long.class, in, int.class, int.class))
                .asType(MethodType.methodType(long.class, Object.class, in, int.class, int.class));
    }

    private static Object inst(String type) throws Throwable {
        Class<?> c = Class.forName("jdk.internal.math." + type);
        return MethodHandles.privateLookupIn(c, MethodHandles.lookup())
                .findConstructor(c, MethodType.methodType(void.class)).invoke();
    }

    interface Op {
        long run() throws Throwable;
    }

    private static double time(Op op, int ops) throws Throwable {
        double best = Double.MAX_VALUE;
        for (int r = 0; r < WARMUP + ROUNDS; r++) {
            long t0 = System.nanoTime();
            sink += op.run();
            long t = System.nanoTime() - t0;
            if (r >= WARMUP) {
                best = Math.min(best, (double) t / ops);
            }
        }
        return best;
    }

    private static void row(String atom, double a, double ref) {
        System.out.printf("%s\t%.2f\t%.2f\t%.2f%n", atom, a, ref, ref / a);
    }

    public static void main(String[] args) throws Throwable {
        List<String> list = new ArrayList<>();
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        while (list.size() < CORPUS_SIZE) {
            list.add(r.nextInt(FLOAT_LITERAL_ONE_IN) == 0 ? Float.toString(Float.intBitsToFloat(r.nextInt() & FINITE_FLOAT_BITS_MASK))
                                       : Double.toString(Double.longBitsToDouble(r.nextLong() & FINITE_DOUBLE_BITS_MASK)));
        }
        String[] v = list.toArray(String[]::new);
        byte[][] b = list.stream().map(s -> s.getBytes(StandardCharsets.ISO_8859_1)).toArray(byte[][]::new);
        int n = v.length;
        System.out.println("atom\tns_per_op_atom\tns_per_op_reference\tspeedup");
        double refD = time(() -> { long s = 0; for (String x : v) s += Double.doubleToRawLongBits((double) DV.invokeExact((Object) READ.invokeExact(x))); return s; }, n);
        double refF = time(() -> { long s = 0; for (String x : v) s += Float.floatToRawIntBits((float) FV.invokeExact((Object) READ.invokeExact(x))); return s; }, n);
        row("M3JavaDoubleBitsFromCharSequence", time(() -> { long s = 0; for (String x : v) s += (long) P_DS.invokeExact(I_DS, (CharSequence) x, 0, x.length()); return s; }, n), refD);
        row("M3JavaFloatBitsFromCharSequence", time(() -> { long s = 0; for (String x : v) s += (long) P_FS.invokeExact(I_FS, (CharSequence) x, 0, x.length()); return s; }, n), refF);
        row("M3JavaDoubleBitsFromByteArray", time(() -> { long s = 0; for (byte[] x : b) s += (long) P_DB.invokeExact(I_DB, x, 0, x.length); return s; }, n), refD);
        row("M3JavaFloatBitsFromByteArray", time(() -> { long s = 0; for (byte[] x : b) s += (long) P_FB.invokeExact(I_FB, x, 0, x.length); return s; }, n), refF);
        double wired = time(() -> { long s = 0; for (String x : v) s += Double.doubleToRawLongBits(Double.parseDouble(x)); return s; }, n);
        row("Double.parseDouble (wiring)", wired, refD);
        double wiredF = time(() -> { long s = 0; for (String x : v) s += Float.floatToRawIntBits(Float.parseFloat(x)); return s; }, n);
        row("Float.parseFloat (wiring)", wiredF, refF);

        byte[] d8 = "12345678".getBytes(StandardCharsets.ISO_8859_1);
        int k = KERNEL_OPS;
        row("M3FastDoubleSwar.isEightDigits", time(() -> { long s = 0; for (int i = 0; i < k; i++) { d8[0] = (byte) (DIGIT_ZERO + (i & DIGIT_CYCLE_MASK)); s += (boolean) EIGHT.invokeExact(d8, 0) ? 1 : 0; } return s; }, k),
            time(() -> { long s = 0; for (int i = 0; i < k; i++) { d8[0] = (byte) (DIGIT_ZERO + (i & DIGIT_CYCLE_MASK)); boolean ok = true; for (int j = 0; j < SWAR_WORD_BYTES; j++) ok &= d8[j] >= DIGIT_ZERO && d8[j] <= DIGIT_NINE; s += ok ? 1 : 0; } return s; }, k));
        row("M3FastDoubleSwar.readLongBE", time(() -> { long s = 0; for (int i = 0; i < k; i++) { d8[1] = (byte) i; s += (long) READ_BE.invokeExact(d8, 0); } return s; }, k),
            time(() -> { long s = 0; ByteBuffer bb = ByteBuffer.wrap(d8); for (int i = 0; i < k; i++) { d8[1] = (byte) i; s += bb.getLong(0); } return s; }, k));
        row("M3FastIntegerMath.unsignedMultiplyHigh", time(() -> { long s = 0; for (int i = 0; i < k; i++) s += (long) UMH.invokeExact((long) i * GOLDEN_GAMMA, s | 1L); return s; }, k),
            time(() -> { long s = 0; for (int i = 0; i < k; i++) s += Math.unsignedMultiplyHigh((long) i * GOLDEN_GAMMA, s | 1L); return s; }, k));
        int q = FAST_MATH_OPS;
        row("M3FastDoubleMath.tryDecToDoubleWithFastAlgorithm", time(() -> { long s = 0; for (int i = 1; i <= q; i++) s += Double.doubleToRawLongBits((double) FDM.invokeExact(false, (long) i * SIGNIFICAND_STRIDE, i % DOUBLE_POWER_SPAN - DOUBLE_POWER_OFFSET)); return s; }, q),
            time(() -> { long s = 0; for (int i = 1; i <= q; i++) s += Double.doubleToRawLongBits(new BigDecimal(BigInteger.valueOf((long) i * SIGNIFICAND_STRIDE)).scaleByPowerOfTen(i % DOUBLE_POWER_SPAN - DOUBLE_POWER_OFFSET).doubleValue()); return s; }, q));
        row("M3FastFloatMath.tryDecToFloatWithFastAlgorithm", time(() -> { long s = 0; for (int i = 1; i <= q; i++) s += Float.floatToRawIntBits((float) FFM.invokeExact(false, (long) i * SIGNIFICAND_STRIDE, i % FLOAT_POWER_SPAN - FLOAT_POWER_OFFSET)); return s; }, q),
            time(() -> { long s = 0; for (int i = 1; i <= q; i++) s += Float.floatToRawIntBits(new BigDecimal(BigInteger.valueOf((long) i * SIGNIFICAND_STRIDE)).scaleByPowerOfTen(i % FLOAT_POWER_SPAN - FLOAT_POWER_OFFSET).floatValue()); return s; }, q));
        BigInteger x = new BigInteger(FFT_OPERAND_BITS, new Random(1)), y = new BigInteger(FFT_OPERAND_BITS, new Random(SECOND_OPERAND_SEED));
        row("M3FftMultiplier.multiply (400k-bit operands)", time(() -> ((BigInteger) MUL.invokeExact(x, y)).bitLength(), 1),
            time(() -> x.multiply(y).bitLength(), 1));
        System.out.println("sink " + sink);
        double slack = Double.parseDouble(System.getProperty("M3_PERF_SLACK", "1.25"));
        if (wired > refD * slack || wiredF > refF * slack) {
            throw new AssertionError("wiring slower than the original converter: double " + wired + " vs " + refD
                    + ", float " + wiredF + " vs " + refF + " ns/op (slack " + slack + ")");
        }
    }
}
