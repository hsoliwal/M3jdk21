// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.acceptance;

import com.m3.arrays.M3ArrayNative;
import com.m3.arrays.packed.Int40Array;
import com.m3.arrays.packed.Int48Array;
import com.m3.arrays.packed.Int56Array;
import com.m3.arrays.packed.PackedIntArray;
import com.m3.arrays.packed.VInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Finite installed-image observations of public operations; no universal speedup claim. */
public final class PackedResourceProbe {
    private static final int BATCH = 400;
    private static final int WARMUP_CALLS = 64;
    private static final int SAMPLES = 101;
    private static final int CALLS_PER_SAMPLE = 8;
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static volatile long consumed;
    private static long verifiedCalls;

    private PackedResourceProbe() { }

    public static void main(String[] arguments) throws Exception {
        require(arguments.length == 1, "one explicit installed native-library path is required");
        Module owner = Int40Array.class.getModule();
        require(owner.isNamed() && "com.m3.arrays".equals(owner.getName()), "named arrays owner");
        require(owner == VInt.class.getModule() && owner == M3ArrayNative.class.getModule(), "one arrays owner");
        require(owner.getLayer() == ModuleLayer.boot(), "arrays owner in boot layer");
        require("jrt".equals(ModuleLayer.boot().configuration().findModule("com.m3.arrays")
                .orElseThrow().reference().location().orElseThrow().getScheme()), "installed image module");
        Path installed = Path.of(System.getProperty("java.home"), "lib", System.mapLibraryName("m3_arrays"));
        require(Files.isRegularFile(installed), "installed native payload");
        Path supplied = Path.of(arguments[0]).toRealPath();
        require(supplied.equals(installed.toRealPath()), "explicit library belongs to running image");
        require(!M3ArrayNative.isLoaded(), "fresh process required for library-load observation");
        require(BATCH * 10 <= M3ArrayNative.MAX_CODEC_ITEMS, "all possible 400-value encodings fit JNI bound");
        ThreadMXBean cpu = currentThreadClock();
        printMemory("before_loading");
        long loadCpuStart = cpu.getCurrentThreadCpuTime();
        long loadWallStart = System.nanoTime();
        M3ArrayNative.load(supplied);
        long loadWallNanos = System.nanoTime() - loadWallStart;
        long loadCpuNanos = cpu.getCurrentThreadCpuTime() - loadCpuStart;
        require(M3ArrayNative.isLoaded(), "explicit installed native library loaded");
        require(loadWallNanos >= 0 && loadCpuNanos >= 0, "monotonic library-load clocks");

        List<Routine> routines = routines();
        require(routines.size() == 20, "ten public operations measured in Java and JNI");
        System.out.println("M3_PACKED_RESOURCE_SCOPE batch=" + BATCH + " routines=" + routines.size()
                + " warmup_calls=" + WARMUP_CALLS + " samples=" + SAMPLES
                + " calls_per_sample=" + CALLS_PER_SAMPLE
                + " first_call_scope=shared_process sample_statistic=mean_ns_per_batch"
                + " p99=nearest_rank validation_timing=excluded no_speedup_claim=true");
        System.out.println("M3_PACKED_RESOURCE_LOAD wall_ns=" + loadWallNanos + " thread_cpu_ns=" + loadCpuNanos);
        System.out.println("M3_PACKED_RESOURCE_LIMITS first_calls_share_classes_and_jit=true"
                + " public_api_allocations_and_copies=included clock_overhead=included"
                + " rss_scope=whole_process native_allocation_attribution=false");

        Measurement[] firstCalls = new Measurement[routines.size()];
        for (int index = 0; index < routines.size(); index++) {
            firstCalls[index] = measure(routines.get(index), cpu);
        }
        printMemory("after_first_calls");

        // Rotate the starting operation between rounds without introducing random input or order.
        for (int round = 0; round < WARMUP_CALLS; round++) {
            for (int offset = 0; offset < routines.size(); offset++) {
                Routine routine = routines.get((round + offset) % routines.size());
                routine.verify(routine.operation.run());
            }
        }
        printMemory("after_warmup");

        long[][] wallSamples = new long[routines.size()][SAMPLES];
        long[][] cpuSamples = new long[routines.size()][SAMPLES];
        for (int sample = 0; sample < SAMPLES; sample++) {
            for (int offset = 0; offset < routines.size(); offset++) {
                int index = (sample + offset) % routines.size();
                Routine routine = routines.get(index);
                long wallTotal = 0;
                long cpuTotal = 0;
                for (int call = 0; call < CALLS_PER_SAMPLE; call++) {
                    Measurement result = measure(routine, cpu);
                    wallTotal = Math.addExact(wallTotal, result.wallNanos());
                    cpuTotal = Math.addExact(cpuTotal, result.cpuNanos());
                }
                wallSamples[index][sample] = wallTotal;
                cpuSamples[index][sample] = cpuTotal;
            }
        }
        printMemory("after_samples");
        for (int index = 0; index < routines.size(); index++) {
            Routine routine = routines.get(index);
            Arrays.sort(wallSamples[index]);
            Arrays.sort(cpuSamples[index]);
            System.out.printf(Locale.ROOT,
                    "M3_PACKED_RESOURCE_RESULT routine=%s checksum=%016x"
                    + " first_wall_ns=%d first_thread_cpu_ns=%d"
                    + " warm_wall_median_ns=%.3f warm_wall_p99_ns=%.3f"
                    + " warm_thread_cpu_median_ns=%.3f warm_thread_cpu_p99_ns=%.3f%n",
                    routine.name, routine.expectedChecksum, firstCalls[index].wallNanos(), firstCalls[index].cpuNanos(),
                    percentile(wallSamples[index], 50), percentile(wallSamples[index], 99),
                    percentile(cpuSamples[index], 50), percentile(cpuSamples[index], 99));
        }
        long expectedCalls = (long) routines.size() * (1 + WARMUP_CALLS + SAMPLES * CALLS_PER_SAMPLE);
        require(verifiedCalls == expectedCalls, "every first, warmup and sampled call was verified");
        System.out.printf(Locale.ROOT,
                "M3_PACKED_RESOURCE_PASS verified_calls=%d consumed=%016x module_origin=jrt%n",
                verifiedCalls, consumed);
    }

    private static ThreadMXBean currentThreadClock() {
        ThreadMXBean cpu = ManagementFactory.getThreadMXBean();
        require(cpu.isCurrentThreadCpuTimeSupported(), "current-thread CPU clock must be available");
        if (!cpu.isThreadCpuTimeEnabled()) cpu.setThreadCpuTimeEnabled(true);
        require(cpu.isThreadCpuTimeEnabled() && cpu.getCurrentThreadCpuTime() >= 0,
                "current-thread CPU clock must be enabled");
        return cpu;
    }

    private static Measurement measure(Routine routine, ThreadMXBean cpu) {
        long cpuStart = cpu.getCurrentThreadCpuTime();
        long wallStart = System.nanoTime();
        Object result = routine.operation.run();
        long wallNanos = System.nanoTime() - wallStart;
        long cpuNanos = cpu.getCurrentThreadCpuTime() - cpuStart;
        require(wallNanos >= 0 && cpuNanos >= 0, "monotonic sample clocks");
        // Use the complete returned object after the clocks stop. No timed call can skip validation.
        routine.verify(result);
        return new Measurement(wallNanos, cpuNanos);
    }

    private static double percentile(long[] sortedSampleTotals, int percent) {
        require(sortedSampleTotals.length == SAMPLES && percent >= 1 && percent <= 100,
                "fixed finite nearest-rank sample population");
        int rank = (percent * sortedSampleTotals.length + 99) / 100;
        return (double) sortedSampleTotals[rank - 1] / CALLS_PER_SAMPLE;
    }

    private static List<Routine> routines() {
        List<Routine> routines = new ArrayList<>();
        for (int bits : new int[] {40, 48, 56}) {
            Fixture fixture = packedFixture(bits);
            addPair(routines, "int" + bits + ".encode",
                    () -> encodeJavaPacked(fixture.values(), bits),
                    () -> M3ArrayNative.encodePackedRequired(fixture.values(), bits), fixture.encoded(), fixture);
            addPair(routines, "int" + bits + ".decode",
                    () -> decodeJavaPacked(fixture.encoded(), bits),
                    () -> M3ArrayNative.decodePackedRequired(fixture.encoded(), bits), fixture.values(), fixture);
        }
        Fixture unsigned = literalFixture(
                new long[] {0, 1, 2, 63, 64, 127, 128, 255, 256, 16383, 16384,
                        0xffffffffL, 0x100000000L, Long.MAX_VALUE, Long.MIN_VALUE, -1},
                new String[] {"00", "01", "02", "3f", "40", "7f", "8001", "ff01", "8002", "ff7f",
                        "808001", "ffffffff0f", "8080808010", "ffffffffffffffff7f",
                        "80808080808080808001", "ffffffffffffffffff01"});
        Fixture zigzag = literalFixture(
                new long[] {0, -1, 1, -2, 2, -64, 63, -65, 64, -128, 127, -129, 128,
                        Long.MIN_VALUE, Long.MAX_VALUE, -2147483648L},
                new String[] {"00", "01", "02", "03", "04", "7f", "7e", "8101", "8001", "ff01",
                        "fe01", "8102", "8002", "ffffffffffffffffff01", "feffffffffffffffff01", "ffffffff0f"});
        addPair(routines, "unsigned64.encode", () -> VInt.encodeUnsigned64(unsigned.values()),
                () -> M3ArrayNative.encodeUnsigned64Required(unsigned.values()), unsigned.encoded(), unsigned);
        addPair(routines, "unsigned64.decode", () -> VInt.decodeUnsigned64(unsigned.encoded()),
                () -> M3ArrayNative.decodeUnsigned64Required(unsigned.encoded()), unsigned.values(), unsigned);
        addPair(routines, "zigzag64.encode", () -> VInt.encodeZigZag64(zigzag.values()),
                () -> M3ArrayNative.encodeZigZag64Required(zigzag.values()), zigzag.encoded(), zigzag);
        addPair(routines, "zigzag64.decode", () -> VInt.decodeZigZag64(zigzag.encoded()),
                () -> M3ArrayNative.decodeZigZag64Required(zigzag.encoded()), zigzag.values(), zigzag);
        return List.copyOf(routines);
    }

    private static void addPair(List<Routine> routines, String name, Operation javaOperation,
            Operation nativeOperation, Object expected, Fixture fixture) {
        routines.add(new Routine("java." + name, javaOperation, expected, fixture));
        routines.add(new Routine("native." + name, nativeOperation, expected, fixture));
    }

    private static Fixture packedFixture(int bits) {
        long minimum = -(1L << (bits - 1));
        long maximum = (1L << (bits - 1)) - 1;
        long[] pattern = {minimum, -0x100000000L, -129, -128, -1, 0, 1, 127,
                128, 255, 256, Integer.MAX_VALUE, 0xffffffffL, 0x100000000L, maximum - 1, maximum};
        long[] values = new long[BATCH];
        int width = bits / Byte.SIZE;
        byte[] expected = new byte[BATCH * width];
        // Independent JDK ByteBuffer oracle; no packed product call builds the expected bytes.
        ByteBuffer scratch = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int index = 0; index < BATCH; index++) {
            values[index] = pattern[index % pattern.length];
            scratch.putLong(0, values[index]);
            System.arraycopy(scratch.array(), 0, expected, index * width, width);
        }
        return new Fixture(values, expected);
    }

    private static Fixture literalFixture(long[] pattern, String[] canonicalHex) {
        require(pattern.length == canonicalHex.length && BATCH % pattern.length == 0, "aligned literal fixtures");
        byte[][] encodings = new byte[canonicalHex.length][];
        for (int index = 0; index < canonicalHex.length; index++) {
            encodings[index] = HexFormat.of().parseHex(canonicalHex[index]);
        }
        long[] values = new long[BATCH];
        ByteArrayOutputStream expected = new ByteArrayOutputStream(BATCH * 10);
        for (int index = 0; index < BATCH; index++) {
            int entry = index % pattern.length;
            values[index] = pattern[entry];
            expected.writeBytes(encodings[entry]);
        }
        byte[] encoded = expected.toByteArray();
        require(encoded.length <= M3ArrayNative.MAX_CODEC_ITEMS, "literal native input/output bound");
        return new Fixture(values, encoded);
    }

    private static byte[] encodeJavaPacked(long[] values, int bits) {
        PackedIntArray array = switch (bits) {
            case 40 -> new Int40Array(values.length);
            case 48 -> new Int48Array(values.length);
            case 56 -> new Int56Array(values.length);
            default -> throw new AssertionError("unsupported fixture width: " + bits);
        };
        array.setAll(0, values, 0, values.length);
        return array.toByteArray();
    }

    private static long[] decodeJavaPacked(byte[] bytes, int bits) {
        PackedIntArray array = switch (bits) {
            case 40 -> Int40Array.fromByteArray(bytes);
            case 48 -> Int48Array.fromByteArray(bytes);
            case 56 -> Int56Array.fromByteArray(bytes);
            default -> throw new AssertionError("unsupported fixture width: " + bits);
        };
        return array.toLongArray();
    }

    private static void printMemory(String phase) {
        Path status = Path.of("/proc/self/status");
        if (!Files.isReadable(status)) {
            System.out.println("M3_PACKED_RESOURCE_MEMORY phase=" + phase + " rss_kib=unavailable hwm_kib=unavailable");
            return;
        }
        try {
            List<String> lines = Files.readAllLines(status, StandardCharsets.US_ASCII);
            System.out.println("M3_PACKED_RESOURCE_MEMORY phase=" + phase
                    + " rss_kib=" + procKilobytes(lines, "VmRSS:")
                    + " hwm_kib=" + procKilobytes(lines, "VmHWM:"));
        } catch (IOException failure) {
            System.out.println("M3_PACKED_RESOURCE_MEMORY phase=" + phase
                    + " rss_kib=unavailable hwm_kib=unavailable read_failure=" + failure.getClass().getSimpleName());
        }
    }

    private static String procKilobytes(List<String> lines, String key) {
        for (String line : lines) {
            if (line.startsWith(key)) {
                String[] fields = line.trim().split("\\s+");
                require(fields.length == 3 && "kB".equals(fields[2]), "Linux memory counter units");
                long value = Long.parseLong(fields[1]);
                require(value >= 0, "nonnegative Linux memory counter");
                return Long.toString(value);
            }
        }
        return "unavailable";
    }

    private static Object independentCopy(Object value) {
        if (value instanceof byte[] bytes) return bytes.clone();
        if (value instanceof long[] longs) return longs.clone();
        throw new AssertionError("unexpected result type");
    }

    private static boolean equalContents(Object expected, Object actual) {
        if (expected instanceof byte[] bytes && actual instanceof byte[] actualBytes) {
            return Arrays.equals(bytes, actualBytes);
        }
        if (expected instanceof long[] longs && actual instanceof long[] actualLongs) {
            return Arrays.equals(longs, actualLongs);
        }
        return false;
    }

    private static long checksum(Object value) {
        long hash = FNV_OFFSET;
        if (value instanceof byte[] bytes) {
            hash = (hash ^ bytes.length) * FNV_PRIME;
            for (byte item : bytes) hash = (hash ^ (item & 0xffL)) * FNV_PRIME;
            return hash;
        }
        if (value instanceof long[] longs) {
            hash = (hash ^ longs.length) * FNV_PRIME;
            for (long item : longs) hash = (hash ^ item) * FNV_PRIME;
            return hash;
        }
        throw new AssertionError("unexpected checksum type");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Operation {
        Object run();
    }

    private record Measurement(long wallNanos, long cpuNanos) { }

    private record Fixture(long[] values, byte[] encoded) { }

    private static final class Routine {
        private final String name;
        private final Operation operation;
        private final Object expected;
        private final long expectedChecksum;
        private final Fixture fixture;
        private final long[] originalValues;
        private final byte[] originalBytes;

        private Routine(String name, Operation operation, Object expected, Fixture fixture) {
            this.name = name;
            this.operation = operation;
            this.expected = independentCopy(expected);
            this.expectedChecksum = checksum(this.expected);
            this.fixture = fixture;
            this.originalValues = fixture.values().clone();
            this.originalBytes = fixture.encoded().clone();
        }

        private void verify(Object actual) {
            require(equalContents(expected, actual), name + " complete output equals independent fixture");
            long observedChecksum = checksum(actual);
            require(observedChecksum == expectedChecksum, name + " deterministic checksum");
            require(Arrays.equals(originalValues, fixture.values()) && Arrays.equals(originalBytes, fixture.encoded()),
                    name + " caller-owned inputs unchanged");
            consumed = Long.rotateLeft(consumed, 1) ^ observedChecksum;
            verifiedCalls++;
        }
    }
}
