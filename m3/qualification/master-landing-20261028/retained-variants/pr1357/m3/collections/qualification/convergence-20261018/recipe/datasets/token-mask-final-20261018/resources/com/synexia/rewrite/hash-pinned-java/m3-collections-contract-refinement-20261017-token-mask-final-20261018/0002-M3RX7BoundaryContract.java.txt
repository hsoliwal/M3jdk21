/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * @test
 * @summary Scalar oracle for token coordinates, masks, budgets and native fallback
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3RX7BoundaryContract
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3RX7;

public class M3RX7BoundaryContract {
    private static final String ROOT = "0123456789abcdef".repeat(4);
    private static long checks;
    private static Method nativeBuild;
    private static native void registerKernel();
    private static native long nativeCalls();

    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
        checks++;
    }

    private static void reject(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) { checks++; return; }
            throw new AssertionError("expected " + type + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type);
    }

    private static void nativeRows(short[] dfa, byte[] packed, int[] starts,
                                   int[] lengths, short[] out) {
        try {
            nativeBuild.invoke(null, dfa, packed, starts, lengths, out);
        } catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof RuntimeException failure) throw failure;
            if (cause instanceof Error failure) throw failure;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    // Forward graph traversal per state is independent of production's reverse CSR.
    private static boolean reachesFinal(short[] dfa, byte[] finals, int start) {
        boolean[] seen = new boolean[finals.length];
        int[] queue = new int[finals.length];
        int head = 0, tail = 1;
        queue[0] = start;
        seen[start] = true;
        while (head < tail) {
            int state = queue[head++];
            if (finals[state] != 0) return true;
            for (int unit = 0; unit < 128; unit++) {
                int next = dfa[state * 128 + unit];
                if (next >= 0 && !seen[next]) {
                    seen[next] = true;
                    queue[tail++] = next;
                }
            }
        }
        return false;
    }

    private static int scalar(short[] dfa, int state, byte[] packed, int at, int length) {
        for (int stop = at + length; at < stop && state >= 0; at++) {
            state = dfa[state * 128 + packed[at]];
        }
        return state;
    }

    private static void rows(Random random, int states, int tokens) {
        short[] dfa = new short[states * 128];
        Arrays.fill(dfa, (short) -1);
        byte[] finals = new byte[states];
        for (int s = 0; s < states; s++) {
            finals[s] = (byte) (random.nextInt(5) == 0 ? 1 : 0);
            for (int c = 0; c < 8; c++) dfa[s * 128 + c] = (short) (random.nextInt(states + 1) - 1);
        }
        byte[] packed = new byte[137];
        random.nextBytes(packed);
        for (int i = 0; i < packed.length; i++) packed[i] &= 7;
        int[] starts = new int[tokens], lengths = new int[tokens];
        for (int t = 0; t < tokens; t++) {
            starts[t] = random.nextInt(129);
            lengths[t] = 1 + random.nextInt(8);
        }
        int initial = random.nextInt(states);
        M3RX7 owner = M3RX7.fromAsciiDfa(ROOT, initial, dfa, finals);
        M3RX7.TokenIndex index = owner.prepareTokenIndex(packed, starts, lengths);
        check(index != null, "budget unexpectedly refused");
        check(index.tokenCount() == tokens && index.initialState() == initial, "geometry");
        int words = (tokens + 63) / 64;
        check(index.allowedMaskWords() == words, "word count");
        check(index.retainedPrimitiveBytes() == (long) states * (2L * tokens + 8L * words), "retention");
        boolean[] useful = new boolean[states];
        for (int s = 0; s < states; s++) useful[s] = reachesFinal(dfa, finals, s);
        short[] expected = new short[states * tokens];
        for (int s = 0; s < states; s++) {
            check(index.isFinal(s) == (finals[s] != 0), "final flag");
            for (int t = 0; t < tokens; t++) {
                int next = scalar(dfa, s, packed, starts[t], lengths[t]);
                expected[s * tokens + t] = (short) next;
                check(index.nextState(s, t) == next, "transition");
                boolean allowed = (index.allowedMaskWord(s, t / 64) & (1L << (t % 64))) != 0;
                check(allowed == (next >= 0 && useful[next]), "mask bit");
            }
            if (tokens % 64 != 0) check((index.allowedMaskWord(s, words - 1) >>> (tokens % 64)) == 0, "padding");
        }
        if (nativeBuild != null) {
            short[] actual = new short[expected.length];
            Arrays.fill(actual, (short) -2);
            nativeRows(dfa, packed, starts, lengths, actual);
            check(Arrays.equals(expected, actual), "direct native rows");
        }
        for (int trial = 0; trial < 32; trial++) {
            int[] stream = new int[tokens == 0 ? 0 : random.nextInt(12)];
            int state = initial;
            for (int i = 0; i < stream.length; i++) {
                stream[i] = random.nextInt(tokens);
                state = state < 0 ? -1 : expected[state * tokens + stream[i]];
            }
            check(index.matches(stream) == (state >= 0 && finals[state] != 0), "stream");
        }
        Arrays.fill(dfa, (short) -1);
        Arrays.fill(finals, (byte) 0);
        Arrays.fill(packed, (byte) -1);
        Arrays.fill(starts, Integer.MAX_VALUE);
        Arrays.fill(lengths, Integer.MAX_VALUE);
        for (int s = 0; s < states; s++) for (int t = 0; t < tokens; t++) {
            check(index.nextState(s, t) == expected[s * tokens + t], "defensive snapshot");
        }
        reject(IndexOutOfBoundsException.class, () -> index.nextState(-1, tokens));
        reject(IndexOutOfBoundsException.class, () -> index.nextState(0, -1));
        reject(IndexOutOfBoundsException.class, () -> index.allowedMaskWord(states, 0));
        reject(IndexOutOfBoundsException.class, () -> index.allowedMaskWord(0, words));
        if (tokens != 0) {
            check(index.nextState(-1, 0) == -1, "absorbing dead state");
            reject(IndexOutOfBoundsException.class, () -> index.nextState(-2, 0));
            reject(IndexOutOfBoundsException.class, () -> index.nextState(states, 0));
        }
    }

    private static void validation() {
        short[] dfa = new short[128];
        Arrays.fill(dfa, (short) -1);
        M3RX7 owner = M3RX7.fromAsciiDfa(ROOT, 0, dfa, new byte[]{1});
        reject(NullPointerException.class, () -> owner.prepareTokenIndex(null, new int[0], new int[0]));
        reject(IllegalArgumentException.class, () -> owner.prepareTokenIndex(new byte[1], new int[1], new int[0]));
        reject(IndexOutOfBoundsException.class, () -> owner.prepareTokenIndex(new byte[1], new int[]{-1}, new int[]{1}));
        reject(IndexOutOfBoundsException.class, () -> owner.prepareTokenIndex(new byte[1], new int[]{1}, new int[]{Integer.MAX_VALUE}));
        reject(IllegalArgumentException.class, () -> owner.prepareTokenIndex(new byte[1], new int[]{0}, new int[]{0}));
        reject(IllegalArgumentException.class, () -> owner.prepareTokenIndex(new byte[4097], new int[]{0}, new int[]{4097}));
        reject(IllegalArgumentException.class, () -> owner.prepareTokenIndex(new byte[]{-1}, new int[]{0}, new int[]{1}));
        check(owner.prepareTokenIndex(new byte[(1 << 20) + 1], new int[0], new int[0]) == null, "packed budget");
        int[] starts = new int[(1 << 20) + 1], lengths = new int[starts.length];
        Arrays.fill(lengths, 1);
        check(owner.prepareTokenIndex(new byte[1], starts, lengths) == null, "cell budget");
        int[] workLengths = new int[8193];
        Arrays.fill(workLengths, 4096);
        check(owner.prepareTokenIndex(new byte[4096], new int[8193], workLengths) == null, "work budget");
        check(owner.matchesCandidate((CharSequence) "x\u0080") == -1, "non-ASCII after dead state");
        check(owner.matchesChunksCandidate(new CharSequence[]{"x", "\u0080"}, new int[]{0, 0}, new int[]{1, 1}) == -1, "non-ASCII after dead chunk");
        reject(IndexOutOfBoundsException.class, () -> owner.matchesChunksCandidate(new CharSequence[]{"x"}, new int[]{1}, new int[]{1}));
        reject(NullPointerException.class, () -> owner.matchesChunksCandidate(new CharSequence[]{null}, new int[]{0}, new int[]{0}));
        reject(IllegalArgumentException.class, () -> owner.matchesChunksCandidate(new CharSequence[0], new int[1], new int[0]));
        // Reachability uses the ASCII graph even when no vocabulary token finishes it.
        short[] two = new short[256];
        Arrays.fill(two, (short) -1);
        two['a'] = 0;
        two['b'] = 1;
        M3RX7.TokenIndex onlyA = M3RX7.fromAsciiDfa(ROOT, 0, two, new byte[]{0, 1})
                .prepareTokenIndex(new byte[]{'a'}, new int[]{0}, new int[]{1});
        check(onlyA.allowedMaskWord(0, 0) == 1 && !onlyA.matches(new int[]{0}), "ASCII reachability contract");
        if (nativeBuild != null) {
            short[] out = {123};
            reject(IllegalArgumentException.class, () -> nativeRows(null, new byte[1], new int[]{0}, new int[]{1}, out));
            reject(IllegalArgumentException.class, () -> nativeRows(new short[127], new byte[1], new int[]{0}, new int[]{1}, out));
            reject(IllegalArgumentException.class, () -> nativeRows(dfa, new byte[1], new int[]{Integer.MAX_VALUE}, new int[]{1}, out));
            reject(IllegalArgumentException.class, () -> nativeRows(dfa, new byte[]{-1}, new int[]{0}, new int[]{1}, out));
            reject(IllegalArgumentException.class, () -> nativeRows(dfa, new byte[1], new int[]{0}, new int[]{0}, out));
            short[] invalid = dfa.clone();
            invalid[0] = 1;
            reject(IllegalArgumentException.class, () -> nativeRows(invalid, new byte[1], new int[]{0}, new int[]{1}, out));
            check(out[0] == 123, "native rejection must not write");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            System.load(args[0]);
            registerKernel();
            nativeBuild = M3RX7.class.getDeclaredMethod("buildPacked0", short[].class,
                    byte[].class, int[].class, int[].class, short[].class);
            nativeBuild.setAccessible(true);
        }
        Random random = new Random(0x3B0A_2026L);
        int[] sizes = {0, 1, 63, 64, 65, 127, 128, 129};
        for (int repeat = 0; repeat < 24; repeat++) for (int tokens : sizes) {
            rows(random, 1 + random.nextInt(31), tokens);
        }
        long before = nativeBuild == null ? 0 : nativeCalls();
        rows(random, 64, 1025); // crosses the production native threshold
        if (nativeBuild != null) check(nativeCalls() >= before + 2, "public native dispatch plus direct oracle");
        validation();
        System.out.println("M3RX7BoundaryContract PASS checks=" + checks + " native=" + (nativeBuild != null));
    }
}
