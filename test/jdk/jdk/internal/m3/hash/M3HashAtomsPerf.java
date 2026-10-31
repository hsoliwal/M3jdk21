/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One performance row per atom of the zero-allocation-hashing absorption (jdk.internal.m3.hash.M3*): the
 *          absorbed function and the vendored one (M3HashOracle, sun.misc.Unsafe reads) hash the same deterministic
 *          corpus (lengths 0..1100) through static final MethodHandles bound to the function (invokeExact, inlined by
 *          the JIT like direct calls), warm-up rounds first, best of the measured rounds. One TSV row per atom and
 *          input form: atom, form, ns/op absorbed, ns/op vendored, speedup (vendored / absorbed). Timing is
 *          host-dependent, so the rows are evidence; -DM3_PERF_SLACK=<factor> turns on a guard that fails when byte[]
 *          hashing of an algorithm atom is slower than the vendored library by more than that factor. The String and
 *          direct ByteBuffer rows show what dropping sun.misc.Unsafe and the String internals costs.
 * @modules java.base/jdk.internal.m3.hash:open
 *          java.base/sun.nio.ch
 *          jdk.compiler
 *          jdk.unsupported
 * @run main/othervm -Xmx512m M3HashAtomsPerf
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import jdk.internal.m3.hash.M3LongHashFunction;
import jdk.internal.m3.hash.M3LongTupleHashFunction;

public class M3HashAtomsPerf {
    private static final int WARMUP = 6;
    private static final int ROUNDS = 7;
    private static final int CORPUS = 2048;
    private static final int PASSES = 40;
    private static final int MAX_LENGTH = 1100;
    private static final long CORPUS_SEED = 20261031L;
    private static final int LATIN1_BOUND = 256;
    private static final int UTF16_BOUND = Character.MAX_VALUE + 1;
    private static final String SLACK_PROPERTY = "M3_PERF_SLACK";
    private static final String LHF = "LongHashFunction";
    private static final String LTHF = "LongTupleHashFunction";
    private static final String BYTES = "hashBytes";
    private static final String CHARS = "hashChars";
    private static final MethodType BYTES_TYPE = MethodType.methodType(long.class, byte[].class);
    private static final MethodType TUPLE_TYPE = MethodType.methodType(long[].class, byte[].class);
    private static final MethodType STRING_TYPE = MethodType.methodType(long.class, String.class);
    private static final MethodType BUFFER_TYPE = MethodType.methodType(long.class, ByteBuffer.class, int.class, int.class);
    private static final Class<?>[] NO_PARAMS = {};

    private static final MethodHandle M_XX = m3(M3LongHashFunction.xx(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_XX = oracle(LHF, "xx", BYTES, long.class, byte[].class);
    private static final MethodHandle M_XX3 = m3(M3LongHashFunction.xx3(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_XX3 = oracle(LHF, "xx3", BYTES, long.class, byte[].class);
    private static final MethodHandle M_XX128LOW = m3(M3LongHashFunction.xx128low(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_XX128LOW = oracle(LHF, "xx128low", BYTES, long.class, byte[].class);
    private static final MethodHandle M_XX128 = m3t(M3LongTupleHashFunction.xx128());
    private static final MethodHandle O_XX128 = oracle(LTHF, "xx128", BYTES, long[].class, byte[].class);
    private static final MethodHandle M_WY = m3(M3LongHashFunction.wy_3(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_WY = oracle(LHF, "wy_3", BYTES, long.class, byte[].class);
    private static final MethodHandle M_MURMUR = m3(M3LongHashFunction.murmur_3(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_MURMUR = oracle(LHF, "murmur_3", BYTES, long.class, byte[].class);
    private static final MethodHandle M_MURMUR128 = m3t(M3LongTupleHashFunction.murmur_3());
    private static final MethodHandle O_MURMUR128 = oracle(LTHF, "murmur_3", BYTES, long[].class, byte[].class);
    private static final MethodHandle M_CITY = m3(M3LongHashFunction.city_1_1(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_CITY = oracle(LHF, "city_1_1", BYTES, long.class, byte[].class);
    private static final MethodHandle M_FARM_NA = m3(M3LongHashFunction.farmNa(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_FARM_NA = oracle(LHF, "farmNa", BYTES, long.class, byte[].class);
    private static final MethodHandle M_FARM_UO = m3(M3LongHashFunction.farmUo(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_FARM_UO = oracle(LHF, "farmUo", BYTES, long.class, byte[].class);
    private static final MethodHandle M_METRO = m3(M3LongHashFunction.metro(), BYTES, BYTES_TYPE);
    private static final MethodHandle O_METRO = oracle(LHF, "metro", BYTES, long.class, byte[].class);
    private static final MethodHandle M_XX3_STRING = m3(M3LongHashFunction.xx3(), CHARS, STRING_TYPE);
    private static final MethodHandle O_XX3_STRING = oracle(LHF, "xx3", CHARS, long.class, String.class);
    private static final MethodHandle M_XX3_BUFFER = m3(M3LongHashFunction.xx3(), BYTES, BUFFER_TYPE);
    private static final MethodHandle O_XX3_BUFFER =
            oracle(LHF, "xx3", BYTES, long.class, ByteBuffer.class, int.class, int.class);

    private static long sink;
    private static final List<String> SLOW = new ArrayList<>();

    private static MethodHandle m3(Object f, String name, MethodType type) {
        try {
            return MethodHandles.lookup().findVirtual(M3LongHashFunction.class, name, type).bindTo(f);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle m3t(M3LongTupleHashFunction f) {
        try {
            return MethodHandles.lookup().findVirtual(M3LongTupleHashFunction.class, BYTES, TUPLE_TYPE).bindTo(f);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle oracle(String type, String factory, String name, Class<?> ret, Class<?>... params) {
        return M3HashOracle.virtual(type, name, ret, params).bindTo(M3HashOracle.call(type, factory, NO_PARAMS));
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

    private static void row(String atom, String form, double absorbed, double vendored, boolean guarded) {
        System.out.printf("%s\t%s\t%.2f\t%.2f\t%.2f%n", atom, form, absorbed, vendored, vendored / absorbed);
        String limit = System.getProperty(SLACK_PROPERTY);
        double slack = limit == null ? Double.POSITIVE_INFINITY : Double.parseDouble(limit);
        if (guarded && absorbed > vendored * slack) {
            SLOW.add(atom + " " + form + ": " + absorbed + " vs " + vendored + " ns/op (slack " + slack + ")");
        }
    }

    public static void main(String[] args) throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        byte[][] b = new byte[CORPUS][];
        String[] latin1 = new String[CORPUS];
        String[] utf16 = new String[CORPUS];
        int[] at = new int[CORPUS];
        int total = 0;
        for (int i = 0; i < CORPUS; i++) {
            b[i] = new byte[r.nextInt(MAX_LENGTH + 1)];
            r.nextBytes(b[i]);
            at[i] = total;
            total += b[i].length;
            char[] c = new char[b[i].length / 2];
            for (int k = 0; k < c.length; k++) {
                c[k] = (char) r.nextInt(UTF16_BOUND);
            }
            utf16[i] = new String(c);
            latin1[i] = new String(b[i], 0, c.length, StandardCharsets.ISO_8859_1);
        }
        ByteBuffer direct = ByteBuffer.allocateDirect(total);
        for (byte[] x : b) {
            direct.put(x);
        }
        int ops = CORPUS * PASSES;
        System.out.println("atom\tform\tns_per_op_absorbed\tns_per_op_vendored\tspeedup");
        row("M3XxHash", "xx() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_XX.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_XX.invokeExact(x); return s; }, ops), true);
        row("M3XXH3", "xx3() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_XX3.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_XX3.invokeExact(x); return s; }, ops), true);
        row("M3XXH3", "xx128low() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_XX128LOW.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_XX128LOW.invokeExact(x); return s; }, ops), true);
        row("M3XXH3", "xx128() hashBytes(byte[]) -> long[2]",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += ((long[]) M_XX128.invokeExact(x))[0]; return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += ((long[]) O_XX128.invokeExact(x))[0]; return s; }, ops), true);
        row("M3WyHash", "wy_3() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_WY.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_WY.invokeExact(x); return s; }, ops), true);
        row("M3MurmurHash_3", "murmur_3() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_MURMUR.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_MURMUR.invokeExact(x); return s; }, ops), true);
        row("M3MurmurHash_3", "murmur_3() tuple hashBytes(byte[]) -> long[2]",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += ((long[]) M_MURMUR128.invokeExact(x))[0]; return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += ((long[]) O_MURMUR128.invokeExact(x))[0]; return s; }, ops), true);
        row("M3CityAndFarmHash_1_1", "city_1_1() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_CITY.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_CITY.invokeExact(x); return s; }, ops), true);
        row("M3CityAndFarmHash_1_1", "farmNa() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_FARM_NA.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_FARM_NA.invokeExact(x); return s; }, ops), true);
        row("M3CityAndFarmHash_1_1", "farmUo() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_FARM_UO.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_FARM_UO.invokeExact(x); return s; }, ops), true);
        row("M3MetroHash", "metro() hashBytes(byte[])",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) M_METRO.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (byte[] x : b) s += (long) O_METRO.invokeExact(x); return s; }, ops), true);
        row("M3CharSequenceAccess", "xx3() hashChars(Latin-1 String)",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (String x : latin1) s += (long) M_XX3_STRING.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (String x : latin1) s += (long) O_XX3_STRING.invokeExact(x); return s; }, ops), false);
        row("M3CharSequenceAccess", "xx3() hashChars(UTF-16 String)",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (String x : utf16) s += (long) M_XX3_STRING.invokeExact(x); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (String x : utf16) s += (long) O_XX3_STRING.invokeExact(x); return s; }, ops), false);
        row("M3ByteBufferAccess", "xx3() hashBytes(direct ByteBuffer, off, len)",
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (int i = 0; i < CORPUS; i++) s += (long) M_XX3_BUFFER.invokeExact(direct, at[i], b[i].length); return s; }, ops),
                time(() -> { long s = 0; for (int p = 0; p < PASSES; p++) for (int i = 0; i < CORPUS; i++) s += (long) O_XX3_BUFFER.invokeExact(direct, at[i], b[i].length); return s; }, ops), false);
        System.out.println("sink " + sink);
        if (!SLOW.isEmpty()) {
            throw new AssertionError("absorbed byte[] hashing slower than the vendored library: " + SLOW);
        }
    }
}
