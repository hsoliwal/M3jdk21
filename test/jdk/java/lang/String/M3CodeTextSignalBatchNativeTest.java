/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Explicit libjava batch lexical facts preserve the Java receiver ABI
 * @modules java.base/java.lang:open
 * @compile --patch-module java.base=${test.src} M3CodeTextSignalBatchNativeTest.java
 * @run main/othervm -Xcheck:jni --patch-module=java.base=${test.classes} java.lang.M3CodeTextSignalBatchNativeTest integrated
 */
package java.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** The optional library argument is a bootstrap test loader, never a runtime hook. */
public final class M3CodeTextSignalBatchNativeTest {
    private static final M3CodeTextSignalBatch.Limits LIMITS =
            M3CodeTextSignalBatch.Limits.DEFAULT;
    private static Method analyze;
    private static Method nativeEntry;
    private static Object javaBackend;
    private static Object nativeBackend;
    private static long checks;
    private static long comparedRows;
    private static long batches;

    private M3CodeTextSignalBatchNativeTest() {}

    public static void main(String[] args) throws Exception {
        if (M3CodeTextSignalBatchNativeTest.class.getClassLoader() != null) {
            throw new AssertionError("test must be bootstrap loaded");
        }
        String mode = args.length == 0 ? "integrated" : args[0];
        if (mode.equals("baseline-witness")) {
            long[] old = M3CodeTextSignalBatch.analyze(
                    new char[] {';', ':', ':', '-', '>'}, new int[] {0}, new int[] {5}, LIMITS);
            equal(oracle(new char[] {';', ':', ':', '-', '>'}, 0, 5), old[0], "baseline Java");
            requireBackend();
            throw new AssertionError("baseline witness unexpectedly found the new backend");
        }
        requireBackend();
        surface();
        invalidGeometry();
        if (mode.equals("unavailable")) {
            expect(UnsatisfiedLinkError.class, () -> explicit(
                    new char[] {';'}, new int[] {0}, new int[] {1}, LIMITS, nativeBackend));
            expect(UnsatisfiedLinkError.class, () -> explicit(
                    new char[0], new int[0], new int[0], LIMITS, nativeBackend));
            equal(oracle(new char[] {';'}, 0, 1), explicit(
                    new char[] {';'}, new int[] {0}, new int[] {1}, LIMITS, javaBackend)[0],
                    "Java still available");
            System.out.println("PASS unavailable: explicit native refuses without fallback; checks=" + checks);
            return;
        }
        if (mode.equals("parity")) {
            if (args.length != 2 || !Path.of(args[1]).isAbsolute()) {
                throw new AssertionError("parity requires an exact absolute test-library path");
            }
            System.load(args[1]);
        } else if (!mode.equals("integrated")) {
            throw new AssertionError("unknown mode: " + mode);
        }
        nativeBoundary();
        knownAtoms();
        exhaustiveUtf16();
        slicedAndChunked();
        saturation();
        randomized();
        sequentialMutation();
        concurrentBatches();
        System.out.println("PASS lexical JNI parity: checks=" + checks
                + " comparedRows=" + comparedRows + " batches=" + batches
                + " BMP=65536 surrogateUnits=2048 concurrentBatches=128");
    }

    private static void requireBackend() throws Exception {
        Class<?> backend;
        try {
            backend = Class.forName("java.lang.M3CodeTextSignalBatch$Backend");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("MISSING_REQUIRED_NATIVE_BACKEND", missing);
        }
        Object[] constants = backend.getEnumConstants();
        if (constants == null || constants.length != 2
                || !constants[0].toString().equals("JAVA")
                || !constants[1].toString().equals("NATIVE_REQUIRED")) {
            throw new AssertionError("exact explicit backend contract");
        }
        javaBackend = constants[0];
        nativeBackend = constants[1];
        analyze = M3CodeTextSignalBatch.class.getDeclaredMethod("analyze", char[].class,
                int[].class, int[].class, M3CodeTextSignalBatch.Limits.class, backend);
        nativeEntry = M3CodeTextSignalBatch.class.getDeclaredMethod("analyzeNative", char[].class,
                int[].class, int[].class, long.class, long.class, int.class);
        if (!nativeEntry.trySetAccessible()) throw new AssertionError("test native boundary access");
    }

    private static void surface() {
        if (Modifier.isPublic(M3CodeTextSignalBatch.class.getModifiers())
                || Modifier.isPublic(analyze.getModifiers())
                || Modifier.isProtected(analyze.getModifiers())
                || !Modifier.isPrivate(nativeEntry.getModifiers())
                || !Modifier.isNative(nativeEntry.getModifiers())
                || !Modifier.isStatic(nativeEntry.getModifiers())) {
            throw new AssertionError("internal batch/JNI surface");
        }
        checks++;
    }

    private static long[] invoke(Method method, Object... arguments) throws Exception {
        try {
            return (long[]) method.invoke(null, arguments);
        } catch (InvocationTargetException failed) {
            Throwable cause = failed.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError("unexpected checked target exception", cause);
        }
    }

    private static long[] explicit(char[] units, int[] offsets, int[] lengths,
                                   M3CodeTextSignalBatch.Limits limits, Object backend) throws Exception {
        return invoke(analyze, units, offsets, lengths, limits, backend);
    }

    private static void invalidGeometry() throws Exception {
        char[] units = {'x'};
        int[] zero = {0};
        int[] one = {1};
        for (Object backend : new Object[] {javaBackend, nativeBackend}) {
            expect(NullPointerException.class, () -> explicit(null, zero, one, LIMITS, backend));
            expect(NullPointerException.class, () -> explicit(units, null, one, LIMITS, backend));
            expect(NullPointerException.class, () -> explicit(units, zero, null, LIMITS, backend));
            expect(NullPointerException.class, () -> explicit(units, zero, one, null, backend));
            expect(IllegalArgumentException.class, () -> explicit(units, zero, new int[0], LIMITS, backend));
            expect(IllegalArgumentException.class, () -> explicit(units, new int[] {-1}, one, LIMITS, backend));
            expect(IllegalArgumentException.class, () -> explicit(units, zero, new int[] {-1}, LIMITS, backend));
            expect(IllegalArgumentException.class, () -> explicit(units, new int[] {2}, zero, LIMITS, backend));
            expect(IllegalArgumentException.class, () -> explicit(units, one, one, LIMITS, backend));
            expect(IllegalArgumentException.class, () -> explicit(units,
                    new int[] {Integer.MAX_VALUE}, new int[] {Integer.MAX_VALUE}, LIMITS, backend));
            expect(IllegalArgumentException.class, () -> explicit(units, zero, one,
                    new M3CodeTextSignalBatch.Limits(17, 1, 1), backend));
            expect(IllegalArgumentException.class, () -> explicit(units, zero, one,
                    new M3CodeTextSignalBatch.Limits(Long.MAX_VALUE, 0, 1), backend));
            expect(IllegalArgumentException.class, () -> explicit(units, new int[2], new int[2],
                    new M3CodeTextSignalBatch.Limits(Long.MAX_VALUE, 1, 1), backend));
            // The last row is invalid, after more than one native geometry chunk.
            int[] offsets = new int[257];
            offsets[256] = Integer.MAX_VALUE;
            expect(IllegalArgumentException.class, () -> explicit(units, offsets, new int[257], LIMITS, backend));
        }
        expect(NullPointerException.class, () -> explicit(units, zero, one, LIMITS, null));
        expect(IllegalArgumentException.class, () -> new M3CodeTextSignalBatch.Limits(-1, 1, 1));
        expect(IllegalArgumentException.class, () -> new M3CodeTextSignalBatch.Limits(1, -1, 1));
        expect(IllegalArgumentException.class, () -> new M3CodeTextSignalBatch.Limits(1, 1, 0));
    }

    private static void nativeBoundary() throws Exception {
        char[] units = {';'};
        int[] zero = {0};
        int[] one = {1};
        expect(NullPointerException.class, () -> invoke(nativeEntry, null, zero, one, 18L, 1L, 1));
        expect(NullPointerException.class, () -> invoke(nativeEntry, units, null, one, 18L, 1L, 1));
        expect(NullPointerException.class, () -> invoke(nativeEntry, units, zero, null, 18L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, one, -1L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, one, 18L, -1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, one, 18L, 1L, 0));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, one, 17L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, one, 18L, 0L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, new int[0], 18L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, new int[2], new int[2], 100L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, new int[] {-1}, one, 18L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, zero, new int[] {-1}, 18L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units, one, one, 18L, 1L, 1));
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry, units,
                new int[] {Integer.MAX_VALUE}, new int[] {Integer.MAX_VALUE}, 18L, 1L, 1));
        int[] offsets = new int[257];
        offsets[256] = Integer.MAX_VALUE;
        expect(IllegalArgumentException.class, () -> invoke(nativeEntry,
                units, offsets, new int[257], 10_000L, 1L, 257));
        equal(oracle(units, 0, 1), invoke(nativeEntry, units, zero, one, 18L, 1L, 1)[0],
                "exact native transfer budget");
        equal(0, invoke(nativeEntry, new char[0], new int[0], new int[0], 0L, 0L, 1).length,
                "native zero rows");
    }

    private static void knownAtoms() throws Exception {
        for (String text : new String[] {"", "x", "{}", "()", "\\", ";", ".*+?|^$[]",
                "::", "->", ":", "-", "=<>!,:&", "\uD800\uDC00\uDFFF\uD800",
                "{}();\\.*+?|^$[]=<>!,:&- -> ::\u1680", "\u00a0\u0085\u2007\u202f\ufeff"}) {
            char[] units = text.toCharArray();
            check(units, new int[] {0}, new int[] {units.length});
            equal(M3CodeTextFacts.scan(text).lexicalPacked, oracle(units, 0, units.length),
                    "original facts owner");
        }
        char[] allFlags = "{}();\\.*+?|^$[]=<>!,:&- -> :: ".toCharArray();
        equal(0xffffL, oracle(allFlags, 0, allFlags.length) & 0xffffL, "all sixteen flags");
    }

    private static void exhaustiveUtf16() throws Exception {
        char[] units = new char[1 << 16];
        int[] offsets = new int[units.length];
        int[] lengths = new int[units.length];
        for (int value = 0; value < units.length; value++) {
            units[value] = (char) value;
            offsets[value] = value;
            lengths[value] = 1;
        }
        long[] result = check(units, offsets, lengths);
        int whitespace = 0;
        for (int value = 0; value < units.length; value++) {
            boolean expected = Character.isWhitespace((char) value);
            equal(expected ? 1 : 0, (result[value] >>> 15) & 1L, "BMP whitespace " + value);
            if (expected) whitespace++;
        }
        equal(25, whitespace, "Java 21 BMP whitespace cardinality");
    }

    private static void slicedAndChunked() throws Exception {
        char[] units = new char[4099];
        Arrays.fill(units, 'x');
        for (int seam : new int[] {127, 255, 1023, 2047, 4095}) {
            units[seam] = ':';
            units[seam + 1] = ':';
            check(units, new int[] {0, seam, seam, seam + 1, seam + 2, units.length},
                    new int[] {units.length, 2, 1, 1, 0, 0});
            units[seam] = '-';
            units[seam + 1] = '>';
            check(units, new int[] {seam + 1, 0, seam, seam, 0},
                    new int[] {1, units.length, 1, 2, 0});
            units[seam] = 'x';
            units[seam + 1] = 'x';
        }
        for (int rows : new int[] {0, 1, 127, 128, 129, 255, 256, 257}) {
            int[] offsets = new int[rows];
            int[] lengths = new int[rows];
            for (int row = 0; row < rows; row++) {
                offsets[row] = row % 3 == 0 ? units.length : row;
                lengths[row] = row % 3 == 0 ? 0 : Math.min(1025, units.length - row);
            }
            check(units, offsets, lengths);
        }
        equal(0, explicit(new char[0], new int[0], new int[0],
                new M3CodeTextSignalBatch.Limits(0, 0, 1), nativeBackend).length, "zero budget");
        equal(0, explicit(new char[] {'x'}, new int[] {0}, new int[] {1},
                new M3CodeTextSignalBatch.Limits(18, 1, 1), nativeBackend)[0], "exact budget");
        equal(0, explicit(new char[] {'x'}, new int[] {1}, new int[] {0},
                new M3CodeTextSignalBatch.Limits(Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE),
                nativeBackend)[0], "maximum declared budgets");
    }

    private static void saturation() throws Exception {
        for (char unit : new char[] {';', '{', '(', '\\', '*', ','}) {
            for (int length : new int[] {254, 255, 256, 4097, 1_000_003}) {
                char[] units = new char[length];
                Arrays.fill(units, unit);
                check(units, new int[] {0, 1, length}, new int[] {length, length - 1, 0});
            }
        }
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33434f44454cL);
        String punctuation = "{}();\\.*+?|^$[]=<>!,:&- \t\n";
        for (int trial = 0; trial < 256; trial++) {
            char[] units = new char[random.nextInt(8193)];
            for (int index = 0; index < units.length; index++) {
                units[index] = random.nextBoolean()
                        ? (char) random.nextInt(1 << 16)
                        : punctuation.charAt(random.nextInt(punctuation.length()));
            }
            int[] offsets = new int[random.nextInt(260)];
            int[] lengths = new int[offsets.length];
            for (int row = 0; row < offsets.length; row++) {
                offsets[row] = random.nextInt(units.length + 1);
                lengths[row] = random.nextInt(units.length - offsets[row] + 1);
            }
            check(units, offsets, lengths);
        }
    }

    private static void sequentialMutation() throws Exception {
        char[] units = {'x', ':', ':', ';'};
        int[] offsets = {0};
        int[] lengths = {1};
        check(units, offsets, lengths);
        units[0] = ';';
        check(units, offsets, lengths);
        offsets[0] = 1;
        lengths[0] = 2;
        check(units, offsets, lengths);
        lengths[0] = 1;
        check(units, offsets, lengths);
    }

    private static void concurrentBatches() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[4];
        for (int id = 0; id < threads.length; id++) {
            final int ordinal = id;
            threads[id] = new Thread(() -> {
                try {
                    char[] units = (";::->\u1680\uD800" + ordinal).repeat(257).toCharArray();
                    int[] offsets = {0, 1, units.length, 1023};
                    int[] lengths = {units.length, 5, 0, 257};
                    long[] expected = M3CodeTextSignalBatch.analyze(units, offsets, lengths, LIMITS);
                    for (int repeat = 0; repeat < 32; repeat++) {
                        if (!Arrays.equals(expected, explicit(units, offsets, lengths, LIMITS, nativeBackend))) {
                            throw new AssertionError("concurrent batch " + ordinal);
                        }
                    }
                } catch (Throwable problem) {
                    failure.compareAndSet(null, problem);
                }
            }, "code-text-native-" + id);
            threads[id].start();
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError("concurrent native execution", failure.get());
        checks += 128;
    }

    private static long[] check(char[] units, int[] offsets, int[] lengths) throws Exception {
        char[] oldUnits = units.clone();
        int[] oldOffsets = offsets.clone();
        int[] oldLengths = lengths.clone();
        long[] original = M3CodeTextSignalBatch.analyze(units, offsets, lengths, LIMITS);
        long[] javaResult = explicit(units, offsets, lengths, LIMITS, javaBackend);
        long[] nativeResult = explicit(units, offsets, lengths, LIMITS, nativeBackend);
        equal(offsets.length, nativeResult.length, "result geometry");
        if (nativeResult == original || nativeResult == javaResult) throw new AssertionError("fresh output");
        for (int row = 0; row < offsets.length; row++) {
            long expected = oracle(units, offsets[row], lengths[row]);
            equal(expected, original[row], "original Java row " + row);
            equal(expected, javaResult[row], "explicit Java row " + row);
            equal(expected, nativeResult[row], "native row " + row);
        }
        if (!Arrays.equals(units, oldUnits) || !Arrays.equals(offsets, oldOffsets)
                || !Arrays.equals(lengths, oldLengths)) throw new AssertionError("input mutation");
        comparedRows += offsets.length;
        batches++;
        checks++;
        return nativeResult;
    }

    // Independent membership oracle, deliberately not a copy of the owner's switch.
    private static long oracle(char[] units, int offset, int length) {
        int flags = 0;
        long[] counts = new long[6];
        for (int index = offset; index < offset + length; index++) {
            char unit = units[index];
            if ("{}".indexOf(unit) >= 0) flags |= 1;
            if ("()".indexOf(unit) >= 0) flags |= 1 << 1;
            if (unit == ';') flags |= 1 << 2;
            if (unit == '\\') flags |= 1 << 3;
            if (".*+?|^$[]".indexOf(unit) >= 0) flags |= 1 << 4;
            if (index + 1 < offset + length && unit == '-' && units[index + 1] == '>') flags |= 1 << 5;
            if (index + 1 < offset + length && unit == ':' && units[index + 1] == ':') flags |= 1 << 6;
            if (unit == '=') flags |= 1 << 7;
            if ("<>!".indexOf(unit) >= 0) flags |= 1 << 8;
            if ("*+?".indexOf(unit) >= 0) flags |= 1 << 9;
            if ("[]".indexOf(unit) >= 0) flags |= 1 << 10;
            if (unit == '.') flags |= 1 << 11;
            if (unit == ',') flags |= 1 << 12;
            if (unit == ':') flags |= 1 << 13;
            if ("&|".indexOf(unit) >= 0) flags |= 1 << 14;
            if (Character.isWhitespace(unit)) flags |= 1 << 15;
            if (unit == ';') counts[0]++;
            if ("{}".indexOf(unit) >= 0) counts[1]++;
            if ("()".indexOf(unit) >= 0) counts[2]++;
            if (unit == '\\') counts[3]++;
            if ("{}().*+?|^$[]".indexOf(unit) >= 0) counts[4]++;
            if ("{}();\\.*+?|^$[]=<>!,:&-".indexOf(unit) >= 0) counts[5]++;
        }
        long packed = flags;
        for (int lane = 0; lane < counts.length; lane++) {
            packed |= Math.min(255L, counts[lane]) << (16 + lane * 8);
        }
        return packed;
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> expected, Action action) throws Exception {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) { checks++; return; }
            throw new AssertionError("expected " + expected.getName() + ", got " + actual, actual);
        }
        throw new AssertionError("missing exception " + expected.getName());
    }

    private static void equal(long expected, long actual, String context) {
        checks++;
        if (expected != actual) throw new AssertionError(context + ": expected 0x"
                + Long.toHexString(expected) + ", got 0x" + Long.toHexString(actual));
    }
}
