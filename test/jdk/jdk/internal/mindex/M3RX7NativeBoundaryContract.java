/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Direct RX7 JNI parity, alias rejection and failure atomicity
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xcheck:jni --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED M3RX7NativeBoundaryContract
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3RX7;

public class M3RX7NativeBoundaryContract {
    private static final Class<?> KERNEL;
    private static final Method COMPOSE, REPEAT, PACKED;
    private static long checks, nativeCalls, nativeRows, nativeNanos;

    static {
        try {
            KERNEL = Class.forName("jdk.internal.mindex.M3RX7Transitions");
            COMPOSE = method(KERNEL, "compose0", short[].class, short[].class, short[].class);
            REPEAT = method(KERNEL, "repeat0", short[].class, long.class, short[].class);
            PACKED = method(M3RX7.class, "buildPacked0", short[].class, byte[].class,
                    int[].class, int[].class, short[].class);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("java-default")) {
            javaDefault();
            return;
        }
        // Host qualification alone supplies this bootstrap-loaded fixture. Production
        // and source-built jtreg use libjava and have no custom loading path.
        String nativePath = System.getProperty("m3.rx7.native.path");
        if (nativePath != null) {
            Class<?> bootstrap = Class.forName("jdk.internal.mindex.M3RX7NativeBootstrap");
            check(bootstrap.getClassLoader() == null, "test loader must be bootstrap-owned");
            bootstrap.getMethod("load", String.class).invoke(null, nativePath);
        }
        check(M3RX7.class.getClassLoader() == null && KERNEL.getClassLoader() == null,
                "production owners are bootstrap-loaded");
        compositionParity();
        transferBoundary();
        packedParity();
        packedBoundary();
        publicCompatibility();
        System.out.println("RX7 native checks=" + checks + " directCalls=" + nativeCalls
                + " outputRows=" + nativeRows + " directCallNanos=" + nativeNanos
                + " (elapsed includes reflection; not a benchmark)");
    }

    private static void javaDefault() throws Exception {
        Method compose = method(KERNEL, "compose", short[].class, short[].class);
        Method repeat = method(KERNEL, "repeat", short[].class, long.class);
        short[] left = new short[1024];
        short[] right = new short[1024];
        for (int state = 0; state < left.length; state++) {
            left[state] = (short) ((state + 1) % left.length);
            right[state] = (short) (left.length - 1 - state);
        }
        same(composeOracle(left, right), (short[]) invoke(compose, left, right), "Java composition without JNI symbols");
        same(repeatOracle(left, Long.MAX_VALUE), (short[]) invoke(repeat, left, Long.MAX_VALUE),
                "Java repetition without JNI symbols");
        missingSymbol(COMPOSE, left, right, new short[left.length]);
        missingSymbol(REPEAT, left, 2L, new short[left.length]);
        System.out.println("RX7 Java default checks=" + checks + " missingSymbols=2 states=1024");
    }

    private static void compositionParity() {
        Random random = new Random(0x4A_4E_49_52_58_37L);
        long[] counts = {0, 1, 2, 3, 7, 63, 64, 255, 256, 1L << 32, Long.MAX_VALUE};
        for (int states : new int[] {1, 2, 3, 31, 64, 65, 1023, 1024, 2048, 4096}) {
            for (int fixture = 0; fixture < 18; fixture++) {
                short[] left = new short[states];
                short[] right = new short[states];
                for (int state = 0; state < states; state++) {
                    left[state] = (short) (random.nextInt(5) == 0 ? -1 : random.nextInt(states));
                    right[state] = (short) (random.nextInt(5) == 0 ? -1 : random.nextInt(states));
                }
                short[] originalLeft = left.clone(), originalRight = right.clone();
                short[] out = sentinel(states);
                nativeCall(COMPOSE, left, right, out);
                same(composeOracle(left, right), out, "native compose rows");
                nativeRows += states;
                nativeCall(COMPOSE, left, left, out);
                same(composeOracle(left, left), out, "native same-input squaring");
                nativeRows += states;
                for (long count : counts) {
                    Arrays.fill(out, Short.MIN_VALUE);
                    nativeCall(REPEAT, left, count, out);
                    same(repeatOracle(left, count), out, "native repeat rows count=" + count);
                    nativeRows += states;
                }
                same(originalLeft, left, "native left input immutable");
                same(originalRight, right, "native right input immutable");
            }
        }
        // A full 4096-state cycle forces a long orbit, including Long.MAX_VALUE.
        short[] cycle = new short[4096], out = sentinel(4096);
        for (int state = 0; state < cycle.length; state++) cycle[state] = (short) ((state + 1) & 4095);
        nativeCall(REPEAT, cycle, Long.MAX_VALUE, out);
        for (int state = 0; state < cycle.length; state++) {
            equal((state + (Long.MAX_VALUE % cycle.length)) % cycle.length, out[state], "long cycle power");
        }
        nativeRows += cycle.length;
    }

    private static void transferBoundary() {
        short[] rows = {1, -1};
        short[] out = sentinel(2);
        reject(out, () -> nativeCall(COMPOSE, null, rows, out));
        reject(out, () -> nativeCall(COMPOSE, rows, null, out));
        reject(null, () -> nativeCall(COMPOSE, rows, rows, null));
        reject(out, () -> nativeCall(REPEAT, null, 1L, out));
        reject(null, () -> nativeCall(REPEAT, rows, 1L, null));
        reject(out, () -> nativeCall(REPEAT, rows, -1L, out));
        reject(rows, () -> nativeCall(COMPOSE, rows, rows, rows));
        reject(rows, () -> nativeCall(COMPOSE, new short[] {0, 1}, rows, rows));
        reject(rows, () -> nativeCall(COMPOSE, rows, new short[] {0, 1}, rows));
        reject(rows, () -> nativeCall(REPEAT, rows, 0L, rows));
        reject(rows, () -> nativeCall(REPEAT, rows, 7L, rows));
        reject(out, () -> nativeCall(COMPOSE, new short[0], new short[0], out));
        reject(out, () -> nativeCall(REPEAT, new short[0], 0L, out));
        reject(out, () -> nativeCall(COMPOSE, rows, new short[3], out));
        reject(out, () -> nativeCall(COMPOSE, new short[3], rows, out));
        short[] shortOutput = sentinel(1);
        reject(shortOutput, () -> nativeCall(COMPOSE, rows, rows, shortOutput));
        reject(out, () -> nativeCall(REPEAT, new short[1], 1L, out));
        short[] tooLarge = new short[4097], bigOut = sentinel(4097);
        reject(bigOut, () -> nativeCall(COMPOSE, tooLarge, tooLarge, bigOut));
        reject(bigOut, () -> nativeCall(REPEAT, tooLarge, 1L, bigOut));
        for (short bad : new short[] {-2, Short.MIN_VALUE, 2, Short.MAX_VALUE}) {
            short[] malformed = {0, bad};
            reject(out, () -> nativeCall(COMPOSE, malformed, rows, out));
            reject(out, () -> nativeCall(COMPOSE, rows, malformed, out));
            reject(out, () -> nativeCall(REPEAT, malformed, 0L, out));
            reject(out, () -> nativeCall(REPEAT, malformed, 3L, out));
        }
    }

    private static void packedParity() {
        Random random = new Random(0x50_41_43_4B_2026L);
        for (int states : new int[] {1, 3, 31, 64, 1024, 4096}) {
            short[] dfa = new short[states * 128];
            for (int cell = 0; cell < dfa.length; cell++) {
                dfa[cell] = (short) (random.nextInt(7) == 0 ? -1 : random.nextInt(states));
            }
            for (int count : new int[] {0, 1, 63, 64, 65, 128, 256}) {
                byte[] bytes = new byte[count * 18 + 1];
                Arrays.fill(bytes, (byte) 0xFF);
                int[] offsets = new int[count], lengths = new int[count];
                for (int token = 0; token < count; token++) {
                    offsets[token] = (count - token - 1) * 18 + 1;
                    lengths[token] = 1 + random.nextInt(16);
                    for (int at = offsets[token]; at < offsets[token] + lengths[token]; at++) {
                        bytes[at] = (byte) random.nextInt(128);
                    }
                }
                short[] out = sentinel(states * count);
                short[] originalDfa = dfa.clone();
                byte[] originalBytes = bytes.clone();
                int[] originalOffsets = offsets.clone(), originalLengths = lengths.clone();
                nativeCall(PACKED, dfa, bytes, offsets, lengths, out);
                same(packedOracle(dfa, bytes, offsets, lengths), out, "native packed rows");
                same(originalDfa, dfa, "native DFA input immutable");
                check(Arrays.equals(originalBytes, bytes), "native bytes immutable");
                check(Arrays.equals(originalOffsets, offsets), "native offsets immutable");
                check(Arrays.equals(originalLengths, lengths), "native lengths immutable");
                nativeRows += out.length;
            }
        }
        short[] dfa = new short[128]; // One accepting self-loop, all 128 ASCII values.
        byte[] alphabet = new byte[128];
        int[] offsets = new int[128], lengths = new int[128];
        for (int token = 0; token < 128; token++) { alphabet[token] = (byte) token; offsets[token] = token; lengths[token] = 1; }
        short[] out = sentinel(128);
        nativeCall(PACKED, dfa, alphabet, offsets, lengths, out);
        same(new short[128], out, "all ASCII values including NUL and DEL");
        nativeRows += out.length;
    }

    private static void packedBoundary() {
        short[] dfa = new short[128];
        byte[] packed = {'a'};
        int[] starts = {0}, lengths = {1};
        short[] out = sentinel(1);
        reject(out, () -> nativeCall(PACKED, null, packed, starts, lengths, out));
        reject(out, () -> nativeCall(PACKED, dfa, null, starts, lengths, out));
        reject(out, () -> nativeCall(PACKED, dfa, packed, null, lengths, out));
        reject(out, () -> nativeCall(PACKED, dfa, packed, starts, null, out));
        reject(null, () -> nativeCall(PACKED, dfa, packed, starts, lengths, null));
        reject(out, () -> nativeCall(PACKED, new short[0], packed, starts, lengths, out));
        reject(out, () -> nativeCall(PACKED, new short[127], packed, starts, lengths, out));
        reject(out, () -> nativeCall(PACKED, new short[129], packed, starts, lengths, out));
        reject(out, () -> nativeCall(PACKED, new short[4097 * 128], packed, starts, lengths, out));
        reject(out, () -> nativeCall(PACKED, dfa, packed, starts, new int[0], out));
        short[] wrongOut = sentinel(2);
        reject(wrongOut, () -> nativeCall(PACKED, dfa, packed, starts, lengths, wrongOut));
        reject(out, () -> nativeCall(PACKED, dfa, new byte[(1 << 20) + 1], starts, lengths, out));
        for (int start : new int[] {-1, 1, 2, Integer.MAX_VALUE}) {
            reject(out, () -> nativeCall(PACKED, dfa, packed, new int[] {start}, lengths, out));
        }
        for (int length : new int[] {-1, 0, 2, 4097, Integer.MAX_VALUE}) {
            reject(out, () -> nativeCall(PACKED, dfa, packed, starts, new int[] {length}, out));
        }
        for (short state : new short[] {-2, Short.MIN_VALUE, 1, Short.MAX_VALUE}) {
            short[] malformed = dfa.clone();
            malformed[127] = state;
            reject(out, () -> nativeCall(PACKED, malformed, packed, starts, lengths, out));
        }
        for (int value = 128; value <= 255; value++) {
            byte[] nonAscii = {(byte) value};
            reject(out, () -> nativeCall(PACKED, dfa, nonAscii, starts, lengths, out));
        }
        int[] aliasStarts = new int[128], aliasLengths = new int[128];
        Arrays.fill(aliasLengths, 1);
        reject(dfa, () -> nativeCall(PACKED, dfa, packed, aliasStarts, aliasLengths, dfa));

        short[] maximalDfa = new short[4096 * 128];
        short[] tooManyCells = new short[0];
        reject(tooManyCells, () -> nativeCall(PACKED, maximalDfa, packed, new int[257], new int[257], tooManyCells));
        byte[] maxToken = new byte[4096];
        short[] excessiveWork = sentinel(4096 * 3);
        reject(excessiveWork, () -> nativeCall(PACKED, maximalDfa, maxToken,
                new int[] {0, 0, 0}, new int[] {4096, 4096, 4096}, excessiveWork));
        short[] exactWork = sentinel(4096 * 2);
        nativeCall(PACKED, maximalDfa, maxToken, new int[] {0, 0}, new int[] {4096, 4096}, exactWork);
        same(new short[exactWork.length], exactWork, "inclusive 32 Mi native work limit");
        nativeRows += exactWork.length;
        byte[] maximalPacked = new byte[1 << 20];
        nativeCall(PACKED, dfa, maximalPacked, new int[] {maximalPacked.length - 1}, lengths, out);
        equal(0, out[0], "inclusive packed bound and last-byte slice");
        nativeRows++;
    }

    private static void publicCompatibility() throws Exception {
        int states = 1024;
        short[] dfa = new short[states * 128];
        Arrays.fill(dfa, (short) -1);
        for (int state = 0; state < states; state++) dfa[state * 128 + 'a'] = (short) ((state + 1) % states);
        byte[] finals = new byte[states];
        finals[0] = 1;
        M3RX7 plan = M3RX7.fromAsciiDfa("9".repeat(64), 0, dfa, finals);
        int[] offsets = new int[65], lengths = new int[65];
        Arrays.fill(lengths, 1);
        M3RX7.TokenIndex index = plan.prepareTokenIndex(new byte[] {'a'}, offsets, lengths);
        check(index != null, "native-dispatch fixture index");
        M3RX7.Transfer a = index.transfer(new int[] {0});
        check(a != null, "native-dispatch fixture transfer");
        equal(2, a.then(a).nextState(0), "public Java composition with libjava present");
        equal(257, a.repeated(257).nextState(0), "public Java repetition with libjava present");
        equal(1023, a.repeated(Long.MAX_VALUE).nextState(0), "public long Java repetition");
        var tokenField = M3RX7.class.getDeclaredField("nativeTokenUnavailable");
        tokenField.setAccessible(true);
        check(!tokenField.getBoolean(null), "packed JNI symbol was resolved");
    }

    private static short[] composeOracle(short[] left, short[] right) {
        short[] out = new short[left.length];
        for (int state = 0; state < left.length; state++) out[state] = left[state] == -1 ? -1 : right[left[state]];
        return out;
    }

    /** Orbit/cycle interpretation, independent of exponentiation by squaring. */
    private static short[] repeatOracle(short[] rows, long count) {
        short[] out = new short[rows.length];
        int[] seen = new int[rows.length], step = new int[rows.length];
        for (int initial = 0; initial < rows.length; initial++) {
            int state = initial, elapsed = 0, generation = initial + 1;
            long remaining = count;
            while (remaining > 0 && state >= 0) {
                if (seen[state] == generation) {
                    remaining %= elapsed - step[state];
                    while (remaining-- > 0) state = rows[state];
                    break;
                }
                seen[state] = generation;
                step[state] = elapsed++;
                state = rows[state];
                remaining--;
            }
            out[initial] = (short) state;
        }
        return out;
    }

    private static short[] packedOracle(short[] dfa, byte[] bytes, int[] starts, int[] lengths) {
        short[] out = new short[(dfa.length / 128) * starts.length];
        for (int state = 0; state < dfa.length / 128; state++) {
            for (int token = 0; token < starts.length; token++) {
                int next = state;
                for (int at = starts[token]; at < starts[token] + lengths[token] && next >= 0; at++) {
                    next = dfa[next * 128 + bytes[at]];
                }
                out[state * starts.length + token] = (short) next;
            }
        }
        return out;
    }

    private static short[] sentinel(int length) {
        short[] out = new short[length];
        Arrays.fill(out, Short.MIN_VALUE);
        return out;
    }

    private static Method method(Class<?> owner, String name, Class<?>... types) throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Method method, Object... args) {
        try { return method.invoke(null, args); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static void nativeCall(Method method, Object... args) {
        long start = System.nanoTime();
        try { invoke(method, args); }
        finally { nativeCalls++; nativeNanos += System.nanoTime() - start; }
    }

    private static void missingSymbol(Method method, Object... args) {
        checks++;
        try { invoke(method, args); }
        catch (UnsatisfiedLinkError expected) { return; }
        throw new AssertionError("host Java-default qualification requires the new JNI symbol to be absent");
    }

    private static void reject(short[] output, Runnable action) {
        short[] snapshot = output == null ? null : output.clone();
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            if (output != null) same(snapshot, output, "native refusal leaves output unchanged");
            return;
        }
        throw new AssertionError("native boundary must reject invalid arguments");
    }

    private static void same(short[] expected, short[] actual, String label) {
        check(expected.length == actual.length, label + " dimensions");
        for (int state = 0; state < expected.length; state++) equal(expected[state], actual[state], label);
    }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
}
