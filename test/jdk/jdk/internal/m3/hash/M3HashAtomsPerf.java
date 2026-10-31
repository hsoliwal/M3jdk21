/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One performance row per atom of the zero-allocation-hashing absorption (jdk.internal.m3.hash.M3*): the
 *          absorbed function and the vendored one (M3HashOracle, sun.misc.Unsafe reads) hash the same deterministic
 *          corpus through static final MethodHandles bound to the function (invokeExact, inlined by the JIT like
 *          direct calls). Warm-up rounds first, then interleaved A/B rounds (absorbed then vendored, the order
 *          swapped every round); each row reports the median and the best ns/op of both sides, the number of
 *          measured rounds and the mean system CPU load during them (the host is shared). Row groups: the
 *          algorithm atoms over byte[] (lengths 0..1100), the XXH3 64/128-bit atoms one row per input-length class,
 *          the String and ByteBuffer paths with a control row each (StringBuilder, read-only heap ByteBuffer: the
 *          same access on both sides), the access atoms and the kernel atoms. -Dm3.hash.perf.only=<groups> runs a
 *          subset (bytes, xxh3-64, xxh3-128, string, bytebuffer, access, kernel), -Dm3.hash.perf.out=<file> appends
 *          the rows to a file as well, -Dm3.hash.perf.run=<name> names the run. Timing is host-dependent, so the
 *          rows are evidence; -DM3_PERF_SLACK=<factor> turns on a guard that fails when byte[] hashing of an
 *          algorithm atom (median) is slower than the vendored library by more than that factor.
 * @modules java.base/jdk.internal.m3.hash:open
 *          java.base/sun.nio.ch
 *          java.management
 *          jdk.compiler
 *          jdk.management
 *          jdk.unsupported
 * @run main/othervm -Xmx512m M3HashAtomsPerf
 */

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import jdk.internal.m3.hash.M3Access;
import jdk.internal.m3.hash.M3LongHashFunction;
import jdk.internal.m3.hash.M3LongTupleHashFunction;

public class M3HashAtomsPerf {
    private static final int WARMUP = 6;
    private static final int ROUNDS = 9;
    private static final int CORPUS = 2048;
    private static final long WORK_PER_ROUND = 1L << 28;
    private static final int MICRO_OPS_PER_ROUND = 1 << 24;
    private static final int CALL_COST_BYTES = 64;
    private static final int MIN_PASSES = 1;
    private static final int MAX_LENGTH = 1100;
    private static final int LONG_MIN = 1100;
    private static final int LONG_MAX = 4200;
    private static final int ACCESS_BYTES = 4096;
    private static final int KERNEL_VALUES = 4096;
    private static final long CORPUS_SEED = 20261031L;
    private static final int UTF16_BOUND = Character.MAX_VALUE + 1;
    private static final double PERCENT = 100.0;
    private static final String NA = "NA";
    private static final String SLACK_PROPERTY = "M3_PERF_SLACK";
    private static final String ONLY_PROPERTY = "m3.hash.perf.only";
    private static final String OUT_PROPERTY = "m3.hash.perf.out";
    private static final String RUN_PROPERTY = "m3.hash.perf.run";
    private static final String DEFAULT_RUN = "run";
    private static final String GROUP_SEPARATOR = ",";
    private static final String BYTES_GROUP = "bytes";
    private static final String XXH3_64_GROUP = "xxh3-64";
    private static final String XXH3_128_GROUP = "xxh3-128";
    private static final String STRING_GROUP = "string";
    private static final String BUFFER_GROUP = "bytebuffer";
    private static final String ACCESS_GROUP = "access";
    private static final String KERNEL_GROUP = "kernel";
    private static final String HEADER = "run\tgroup\tatom\tform\tlengths\tns_per_op_absorbed_median"
            + "\tns_per_op_vendored_median\tspeedup_median\tns_per_op_absorbed_best\tns_per_op_vendored_best"
            + "\tspeedup_best\trounds\tload_pct";
    private static final String M3 = "jdk.internal.m3.hash.";
    private static final String LHF = "LongHashFunction";
    private static final String LTHF = "LongTupleHashFunction";
    private static final String BYTES = "hashBytes";
    private static final String CHARS = "hashChars";
    private static final MethodType BYTES_TYPE = MethodType.methodType(long.class, byte[].class);
    private static final MethodType TUPLE_TYPE = MethodType.methodType(long[].class, byte[].class);
    private static final MethodType STRING_TYPE = MethodType.methodType(long.class, String.class);
    private static final MethodType BUILDER_TYPE = MethodType.methodType(long.class, StringBuilder.class);
    private static final MethodType BUFFER_TYPE = MethodType.methodType(long.class, ByteBuffer.class, int.class, int.class);
    private static final MethodType READ_TYPE = MethodType.methodType(long.class, Object.class, long.class);
    private static final MethodType BINARY_TYPE = MethodType.methodType(long.class, long.class, long.class);
    private static final MethodType WIDEN_TYPE = MethodType.methodType(long.class, int.class);
    private static final MethodType CHECK_TYPE = MethodType.methodType(void.class, int.class, int.class, int.class);
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
    private static final MethodHandle M_XX3_BUILDER = m3(M3LongHashFunction.xx3(), CHARS, BUILDER_TYPE);
    private static final MethodHandle O_XX3_BUILDER = oracle(LHF, "xx3", CHARS, long.class, StringBuilder.class);
    private static final MethodHandle M_XX3_BUFFER = m3(M3LongHashFunction.xx3(), BYTES, BUFFER_TYPE);
    private static final MethodHandle O_XX3_BUFFER =
            oracle(LHF, "xx3", BYTES, long.class, ByteBuffer.class, int.class, int.class);
    private static final MethodHandle M_READ_BYTES = m3Read(m3ByteArrayAccess());
    private static final MethodHandle O_READ_BYTES = oracleRead(M3HashOracle.call("Access", "unsafe", NO_PARAMS));
    private static final MethodHandle M_READ_BUFFER = m3Read(M3Access.toByteBuffer());
    private static final MethodHandle O_READ_BUFFER = oracleRead(M3HashOracle.call("Access", "toByteBuffer", NO_PARAMS));
    private static final MethodHandle M_READ_CHARS = m3Read(M3Access.toNativeCharSequence());
    private static final MethodHandle O_READ_CHARS =
            oracleRead(M3HashOracle.call("Access", "toNativeCharSequence", NO_PARAMS));
    private static final MethodHandle M_FOLD = m3Static("M3Maths", "unsignedLongMulXorFold", BINARY_TYPE);
    private static final MethodHandle O_FOLD = oracleStatic("Maths", "unsignedLongMulXorFold", BINARY_TYPE);
    private static final MethodHandle M_HIGH = m3Static("M3Maths", "unsignedLongMulHigh", BINARY_TYPE);
    private static final MethodHandle O_HIGH = oracleStatic("Maths", "unsignedLongMulHigh", BINARY_TYPE);
    private static final MethodHandle M_WIDEN = m3Static("M3Primitives", "unsignedInt", WIDEN_TYPE);
    private static final MethodHandle O_WIDEN = oracleStatic("Primitives", "unsignedInt", WIDEN_TYPE);
    private static final MethodHandle M_CHECK = m3Static("M3Util", "checkArrayOffs", CHECK_TYPE);
    private static final MethodHandle O_CHECK = oracleStatic("Util", "checkArrayOffs", CHECK_TYPE);

    private static final com.sun.management.OperatingSystemMXBean OS =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final List<String> SLOW = new ArrayList<>();
    private static long sink;

    interface Op {
        long run() throws Throwable;
    }

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

    private static Object m3ByteArrayAccess() {
        try {
            Class<?> c = Class.forName(M3 + "M3ByteArrayAccess");
            return MethodHandles.privateLookupIn(c, MethodHandles.lookup())
                    .findStaticGetter(c, "INSTANCE", c).invoke();
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle m3Read(Object access) {
        try {
            return MethodHandles.lookup().findVirtual(M3Access.class, "getLong", READ_TYPE).bindTo(access);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle oracleRead(Object access) {
        return M3HashOracle.virtual("Access", "getLong", long.class, Object.class, long.class).bindTo(access);
    }

    private static MethodHandle m3Static(String type, String name, MethodType mt) {
        try {
            Class<?> c = Class.forName(M3 + type);
            return MethodHandles.privateLookupIn(c, MethodHandles.lookup()).findStatic(c, name, mt);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle oracleStatic(String type, String name, MethodType mt) {
        try {
            Class<?> c = M3HashOracle.type(type);
            return MethodHandles.privateLookupIn(c, MethodHandles.lookup()).findStatic(c, name, mt);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** A deterministic corpus: CORPUS byte arrays with lengths uniform in [lo, hi]. */
    private static byte[][] corpus(long seed, int lo, int hi) {
        SplittableRandom r = new SplittableRandom(seed);
        byte[][] b = new byte[CORPUS][];
        for (int i = 0; i < CORPUS; i++) {
            b[i] = new byte[lo + r.nextInt(hi - lo + 1)];
            r.nextBytes(b[i]);
        }
        return b;
    }

    private static long bytes(byte[][] b) {
        long n = 0;
        for (byte[] x : b) {
            n += x.length;
        }
        return n;
    }

    private static int passes(long corpusBytes) {
        return (int) Math.max(MIN_PASSES, WORK_PER_ROUND / (corpusBytes + (long) CORPUS * CALL_COST_BYTES));
    }

    private static double load() {
        return OS.getCpuLoad();
    }

    /** Interleaved A/B rounds; prints and records one row. */
    private static void row(String group, String atom, String form, String lengths, int ops, Op absorbed, Op vendored,
                            boolean guarded) throws Throwable {
        int total = WARMUP + ROUNDS;
        double[] a = new double[ROUNDS];
        double[] v = new double[ROUNDS];
        double loadSum = 0;
        int loadSamples = 0;
        for (int r = 0; r < total; r++) {
            boolean absorbedFirst = (r & 1) == 0;
            double first = time(absorbedFirst ? absorbed : vendored, ops);
            double second = time(absorbedFirst ? vendored : absorbed, ops);
            if (r >= WARMUP) {
                a[r - WARMUP] = absorbedFirst ? first : second;
                v[r - WARMUP] = absorbedFirst ? second : first;
                double l = load();
                if (l >= 0) {
                    loadSum += l;
                    loadSamples++;
                }
            }
        }
        Arrays.sort(a);
        Arrays.sort(v);
        double am = a[ROUNDS / 2];
        double vm = v[ROUNDS / 2];
        String loadPct = loadSamples == 0 ? NA : String.format("%.0f", PERCENT * loadSum / loadSamples);
        String line = String.format("%s\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%s",
                System.getProperty(RUN_PROPERTY, DEFAULT_RUN), group, atom, form, lengths, am, vm, vm / am,
                a[0], v[0], v[0] / a[0], ROUNDS, loadPct);
        System.out.println(line);
        String out = System.getProperty(OUT_PROPERTY);
        if (out != null) {
            append(Path.of(out), line);
        }
        String limit = System.getProperty(SLACK_PROPERTY);
        double slack = limit == null ? Double.POSITIVE_INFINITY : Double.parseDouble(limit);
        if (guarded && am > vm * slack) {
            SLOW.add(atom + " " + form + ": " + am + " vs " + vm + " ns/op (slack " + slack + ")");
        }
    }

    private static double time(Op op, int ops) throws Throwable {
        long t0 = System.nanoTime();
        sink += op.run();
        return (double) (System.nanoTime() - t0) / ops;
    }

    private static void append(Path out, String line) throws IOException {
        if (!Files.exists(out)) {
            Files.writeString(out, HEADER + "\n");
        }
        Files.writeString(out, line + "\n", StandardOpenOption.APPEND);
    }

    public static void main(String[] args) throws Throwable {
        String only = System.getProperty(ONLY_PROPERTY);
        Set<String> groups = only == null
                ? Set.of(BYTES_GROUP, XXH3_64_GROUP, XXH3_128_GROUP, STRING_GROUP, BUFFER_GROUP, ACCESS_GROUP, KERNEL_GROUP)
                : Set.of(only.split(GROUP_SEPARATOR));
        load(); // the first sample only starts the system load counter
        System.out.println(HEADER);
        if (groups.contains(BYTES_GROUP)) {
            bytesRows();
        }
        if (groups.contains(XXH3_64_GROUP)) {
            xxh3Rows(XXH3_64_GROUP);
        }
        if (groups.contains(XXH3_128_GROUP)) {
            xxh3Rows(XXH3_128_GROUP);
        }
        if (groups.contains(STRING_GROUP)) {
            stringRows();
        }
        if (groups.contains(BUFFER_GROUP)) {
            bufferRows();
        }
        if (groups.contains(ACCESS_GROUP)) {
            accessRows();
        }
        if (groups.contains(KERNEL_GROUP)) {
            kernelRows();
        }
        System.out.println("sink " + sink);
        if (!SLOW.isEmpty()) {
            throw new AssertionError("absorbed byte[] hashing slower than the vendored library: " + SLOW);
        }
    }

    // ---- the algorithm atoms over byte[] -------------------------------------------------------------------------

    private static void bytesRows() throws Throwable {
        byte[][] b = corpus(CORPUS_SEED, 0, MAX_LENGTH);
        int p = passes(bytes(b));
        int ops = CORPUS * p;
        String n = "0.." + MAX_LENGTH;
        row(BYTES_GROUP, "M3XxHash", "xx() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_XX.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_XX.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3XXH3", "xx3() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_XX3.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_XX3.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3XXH3", "xx128low() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_XX128LOW.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_XX128LOW.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3XXH3", "xx128() hashBytes(byte[]) -> long[2]", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += ((long[]) M_XX128.invokeExact(x))[0]; return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += ((long[]) O_XX128.invokeExact(x))[0]; return s; }, true);
        row(BYTES_GROUP, "M3WyHash", "wy_3() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_WY.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_WY.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3MurmurHash_3", "murmur_3() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_MURMUR.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_MURMUR.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3MurmurHash_3", "murmur_3() tuple hashBytes(byte[]) -> long[2]", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += ((long[]) M_MURMUR128.invokeExact(x))[0]; return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += ((long[]) O_MURMUR128.invokeExact(x))[0]; return s; }, true);
        row(BYTES_GROUP, "M3CityAndFarmHash_1_1", "city_1_1() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_CITY.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_CITY.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3CityAndFarmHash_1_1", "farmNa() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_FARM_NA.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_FARM_NA.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3CityAndFarmHash_1_1", "farmUo() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_FARM_UO.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_FARM_UO.invokeExact(x); return s; }, true);
        row(BYTES_GROUP, "M3MetroHash", "metro() hashBytes(byte[])", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_METRO.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_METRO.invokeExact(x); return s; }, true);
    }

    // ---- the XXH3 atoms, one row per input-length class -----------------------------------------------------------

    /** [lo, hi] per XXH3 atom: len_0, len_1to3, len_4to8, len_9to16, len_17to128, len_129to240, hashLong (0 or 1
     *  full block of 1024 bytes), hashLong (1 to 4 blocks). */
    private static final int[][] XXH3_CLASSES = {{0, 0}, {1, 3}, {4, 8}, {9, 16}, {17, 128}, {129, 240},
            {241, LONG_MIN}, {LONG_MIN, LONG_MAX}};
    private static final String[] XXH3_ATOMS_64 = {"XXH3_len_0to16_64b (len 0)", "XXH3_len_1to3_64b",
            "XXH3_len_4to8_64b", "XXH3_len_9to16_64b", "XXH3_len_17to128_64b", "XXH3_len_129to240_64b",
            "XXH3_hashLong (64-bit)", "XXH3_hashLong (64-bit, blocks)"};
    private static final String[] XXH3_ATOMS_128 = {"XXH3_len_0to16_128b (len 0)", "XXH3_len_1to3_128b",
            "XXH3_len_4to8_128b", "XXH3_len_9to16_128b", "XXH3_len_17to128_128b", "XXH3_len_129to240_128b",
            "XXH3_hashLong (128-bit)", "XXH3_hashLong (128-bit, blocks)"};

    private static void xxh3Rows(String group) throws Throwable {
        boolean wide = XXH3_128_GROUP.equals(group);
        for (int c = 0; c < XXH3_CLASSES.length; c++) {
            int lo = XXH3_CLASSES[c][0];
            int hi = XXH3_CLASSES[c][1];
            byte[][] b = corpus(CORPUS_SEED + c, lo, hi);
            int p = passes(bytes(b));
            String lengths = lo + ".." + hi;
            if (wide) {
                row(group, "M3XXH3." + XXH3_ATOMS_128[c], "xx128() hashBytes(byte[]) -> long[2]", lengths, CORPUS * p,
                        () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += ((long[]) M_XX128.invokeExact(x))[1]; return s; },
                        () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += ((long[]) O_XX128.invokeExact(x))[1]; return s; }, false);
            } else {
                row(group, "M3XXH3." + XXH3_ATOMS_64[c], "xx3() hashBytes(byte[])", lengths, CORPUS * p,
                        () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) M_XX3.invokeExact(x); return s; },
                        () -> { long s = 0; for (int k = 0; k < p; k++) for (byte[] x : b) s += (long) O_XX3.invokeExact(x); return s; }, false);
            }
        }
    }

    // ---- the String path ------------------------------------------------------------------------------------------

    private static void stringRows() throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        String[] latin1 = new String[CORPUS];
        String[] utf16 = new String[CORPUS];
        StringBuilder[] builders = new StringBuilder[CORPUS];
        long chars = 0;
        for (int i = 0; i < CORPUS; i++) {
            byte[] b = new byte[r.nextInt(MAX_LENGTH + 1)];
            r.nextBytes(b);
            char[] c = new char[b.length / 2];
            for (int k = 0; k < c.length; k++) {
                c[k] = (char) r.nextInt(UTF16_BOUND);
            }
            utf16[i] = new String(c);
            latin1[i] = new String(b, 0, c.length, StandardCharsets.ISO_8859_1);
            builders[i] = new StringBuilder(utf16[i]);
            chars += c.length;
        }
        int p = passes(2 * chars);
        int ops = CORPUS * p;
        String n = "0.." + MAX_LENGTH / 2 + " chars";
        row(STRING_GROUP, "M3CharSequenceAccess", "xx3() hashChars(Latin-1 String)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (String x : latin1) s += (long) M_XX3_STRING.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (String x : latin1) s += (long) O_XX3_STRING.invokeExact(x); return s; }, false);
        row(STRING_GROUP, "M3CharSequenceAccess", "xx3() hashChars(UTF-16 String)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (String x : utf16) s += (long) M_XX3_STRING.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (String x : utf16) s += (long) O_XX3_STRING.invokeExact(x); return s; }, false);
        row(STRING_GROUP, "M3CharSequenceAccess", "xx3() hashChars(StringBuilder) [control: CharSequence access on both sides]", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (StringBuilder x : builders) s += (long) M_XX3_BUILDER.invokeExact(x); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (StringBuilder x : builders) s += (long) O_XX3_BUILDER.invokeExact(x); return s; }, false);
    }

    // ---- the ByteBuffer path --------------------------------------------------------------------------------------

    private static void bufferRows() throws Throwable {
        byte[][] b = corpus(CORPUS_SEED, 0, MAX_LENGTH);
        int total = (int) bytes(b);
        int[] at = new int[CORPUS];
        int[] len = new int[CORPUS];
        ByteBuffer direct = ByteBuffer.allocateDirect(total);
        byte[] heap = new byte[total];
        for (int i = 0, o = 0; i < CORPUS; o += b[i].length, i++) {
            at[i] = o;
            len[i] = b[i].length;
            direct.put(o, b[i]);
            System.arraycopy(b[i], 0, heap, o, b[i].length);
        }
        ByteBuffer directLe = direct.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer readOnly = ByteBuffer.wrap(heap).asReadOnlyBuffer();
        int p = passes(total);
        int ops = CORPUS * p;
        String n = "0.." + MAX_LENGTH;
        row(BUFFER_GROUP, "M3ByteBufferAccess", "xx3() hashBytes(direct ByteBuffer BIG_ENDIAN, off, len)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < CORPUS; i++) s += (long) M_XX3_BUFFER.invokeExact(direct, at[i], len[i]); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < CORPUS; i++) s += (long) O_XX3_BUFFER.invokeExact(direct, at[i], len[i]); return s; }, false);
        row(BUFFER_GROUP, "M3ByteBufferAccess", "xx3() hashBytes(direct ByteBuffer LITTLE_ENDIAN, off, len)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < CORPUS; i++) s += (long) M_XX3_BUFFER.invokeExact(directLe, at[i], len[i]); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < CORPUS; i++) s += (long) O_XX3_BUFFER.invokeExact(directLe, at[i], len[i]); return s; }, false);
        row(BUFFER_GROUP, "M3ByteBufferAccess", "xx3() hashBytes(read-only heap ByteBuffer, off, len) [control: ByteBuffer access on both sides]", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < CORPUS; i++) s += (long) M_XX3_BUFFER.invokeExact(readOnly, at[i], len[i]); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < CORPUS; i++) s += (long) O_XX3_BUFFER.invokeExact(readOnly, at[i], len[i]); return s; }, false);
    }

    // ---- the access atoms: getLong at every offset ----------------------------------------------------------------

    private static void accessRows() throws Throwable {
        byte[] b = new byte[ACCESS_BYTES + Long.BYTES];
        new SplittableRandom(CORPUS_SEED).nextBytes(b);
        long base = M3HashOracleBase.byteBase();
        ByteBuffer direct = ByteBuffer.allocateDirect(b.length).put(0, b);
        char[] c = new char[b.length / 2];
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) ((b[2 * i] & 0xFF) | (b[2 * i + 1] << 8));
        }
        String s16 = new String(c);
        int p = Math.max(MIN_PASSES, MICRO_OPS_PER_ROUND / ACCESS_BYTES);
        int ops = ACCESS_BYTES * p;
        String n = "offsets 0.." + (ACCESS_BYTES - 1);
        row(ACCESS_GROUP, "M3ByteArrayAccess", "getLong(byte[], off)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < ACCESS_BYTES; i++) s += (long) M_READ_BYTES.invokeExact((Object) b, (long) i); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < ACCESS_BYTES; i++) s += (long) O_READ_BYTES.invokeExact((Object) b, base + i); return s; }, false);
        row(ACCESS_GROUP, "M3ByteBufferAccess", "getLong(direct ByteBuffer, off)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < ACCESS_BYTES; i++) s += (long) M_READ_BUFFER.invokeExact((Object) direct, (long) i); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < ACCESS_BYTES; i++) s += (long) O_READ_BUFFER.invokeExact((Object) direct, (long) i); return s; }, false);
        row(ACCESS_GROUP, "M3CharSequenceAccess", "getLong(UTF-16 String, off) native order", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < ACCESS_BYTES; i++) s += (long) M_READ_CHARS.invokeExact((Object) s16, (long) i); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < ACCESS_BYTES; i++) s += (long) O_READ_CHARS.invokeExact((Object) s16, (long) i); return s; }, false);
    }

    // ---- the kernel atoms -----------------------------------------------------------------------------------------

    private static void kernelRows() throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        long[] x = new long[KERNEL_VALUES];
        long[] y = new long[KERNEL_VALUES];
        for (int i = 0; i < KERNEL_VALUES; i++) {
            x[i] = r.nextLong();
            y[i] = r.nextLong();
        }
        int p = Math.max(MIN_PASSES, MICRO_OPS_PER_ROUND / KERNEL_VALUES);
        int ops = KERNEL_VALUES * p;
        String n = KERNEL_VALUES + " random pairs";
        row(KERNEL_GROUP, "M3Maths", "unsignedLongMulXorFold(long, long)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) s += (long) M_FOLD.invokeExact(x[i], y[i]); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) s += (long) O_FOLD.invokeExact(x[i], y[i]); return s; }, false);
        row(KERNEL_GROUP, "M3Maths", "unsignedLongMulHigh(long, long)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) s += (long) M_HIGH.invokeExact(x[i], y[i]); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) s += (long) O_HIGH.invokeExact(x[i], y[i]); return s; }, false);
        row(KERNEL_GROUP, "M3Primitives", "unsignedInt(int)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) s += (long) M_WIDEN.invokeExact((int) x[i]); return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) s += (long) O_WIDEN.invokeExact((int) x[i]); return s; }, false);
        row(KERNEL_GROUP, "M3Util", "checkArrayOffs(int, int, int)", n, ops,
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) { M_CHECK.invokeExact(KERNEL_VALUES, i, KERNEL_VALUES - i); s += i; } return s; },
                () -> { long s = 0; for (int k = 0; k < p; k++) for (int i = 0; i < KERNEL_VALUES; i++) { O_CHECK.invokeExact(KERNEL_VALUES, i, KERNEL_VALUES - i); s += i; } return s; }, false);
    }

    /** The vendored UnsafeAccess.BYTE_BASE (the vendored byte[] access takes Unsafe offsets). */
    private static final class M3HashOracleBase {
        private M3HashOracleBase() {}

        static long byteBase() {
            try {
                Class<?> c = M3HashOracle.type("UnsafeAccess");
                return (long) MethodHandles.privateLookupIn(c, MethodHandles.lookup())
                        .findStaticGetter(c, "BYTE_BASE", long.class).invokeExact();
            } catch (Throwable e) {
                throw new AssertionError("vendored UnsafeAccess.BYTE_BASE", e);
            }
        }
    }
}
