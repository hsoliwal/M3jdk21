/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Random;

/** Finite owner-level oracle; does not substitute for a source-built M3JDK21 image. */
public final class M3CompositeFactAtomsTest {
    private static long checks;
    private M3CompositeFactAtomsTest() { }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(kind.isInstance(failure), "wrong exception: " + failure);
            return;
        }
        throw new AssertionError("missing " + kind.getName());
    }
    private static long[] oracle(long[] left, long[] right) {
        int n = left.length;
        long[] answer = new long[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if ((left[i] & (1L << j)) != 0) answer[i] |= right[j];
            }
        }
        return answer;
    }
    private static long[] identity(int n) {
        long[] rows = new long[n];
        for (int i = 0; i < n; i++) rows[i] = 1L << i;
        return rows;
    }
    private static long[] javaCompose(long[] a, long[] b) {
        long[] result = new long[a.length];
        M3StateRelation.compose(a, 0, b, 0, result, 0, a.length);
        return result;
    }
    private static void relations() {
        Random random = new Random(0x4d33464143544cL);
        for (int n = 1; n <= 64; n++) {
            long mask = n == 64 ? -1L : (1L << n) - 1L;
            for (int trial = 0; trial < 160; trial++) {
                long[] a = new long[n];
                long[] b = new long[n];
                long[] c = new long[n];
                for (int i = 0; i < n; i++) {
                    a[i] = switch (trial % 8) {
                        case 0 -> 0L;
                        case 1 -> 1L << (n - 1);
                        case 2 -> mask;
                        case 3 -> 1L << i;
                        case 4 -> i % 3 == 0 ? 0 : (1L << (n - 1));
                        default -> random.nextLong() & mask;
                    };
                    b[i] = random.nextLong() & mask;
                    c[i] = random.nextLong() & mask;
                }
                long[] savedA = a.clone(), savedB = b.clone();
                long[] expected = oracle(a, b);
                check(Arrays.equals(expected, javaCompose(a, b)), "Java relation n=" + n);
                check(Arrays.equals(expected, M3StateRelation.composeNative(a, b)), "JNI relation n=" + n);
                check(Arrays.equals(a, savedA) && Arrays.equals(b, savedB), "input mutation");
                check(Arrays.equals(javaCompose(javaCompose(a, b), c),
                        javaCompose(a, javaCompose(b, c))), "associativity");
                check(Arrays.equals(javaCompose(a, identity(n)), a), "right identity");
                check(Arrays.equals(javaCompose(identity(n), a), a), "left identity");
                // Three nonoverlapping windows of ONE lane must be legal internally.
                long[] packed = new long[n * 3 + 7];
                Arrays.fill(packed, 0x123456789abcdef0L);
                System.arraycopy(a, 0, packed, 1, n);
                System.arraycopy(b, 0, packed, n + 3, n);
                M3StateRelation.compose(packed, 1, packed, n + 3, packed, 2 * n + 5, n);
                check(Arrays.equals(expected, Arrays.copyOfRange(packed, 2 * n + 5, 3 * n + 5)), "offsets");
                check(packed[0] == 0x123456789abcdef0L && packed[3 * n + 6] == 0x123456789abcdef0L,
                        "offset sentinels");
                if (trial < 18) {
                    long[] repeated = identity(n);
                    for (int i = 0; i < trial; i++) repeated = oracle(repeated, a);
                    check(Arrays.equals(repeated, M3StateRelation.repeatNative(a, trial)), "repeat");
                }
            }
            long[] cycle = new long[n];
            for (int i = 0; i < n; i++) cycle[i] = 1L << ((i + 1) % n);
            for (long count : new long[] {0, 1, 2, 63, 64, 65, 1024, Long.MAX_VALUE}) {
                long[] expected = new long[n];
                int step = (int) (count % n);
                for (int i = 0; i < n; i++) expected[i] = 1L << ((i + step) % n);
                check(Arrays.equals(expected, M3StateRelation.repeatNative(cycle, count)), "huge repeat");
            }
        }
        expect(NullPointerException.class, () -> M3StateRelation.composeNative(null, new long[1]));
        expect(NullPointerException.class, () -> M3StateRelation.composeNative(new long[1], null));
        expect(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[0], new long[0]));
        expect(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[65], new long[65]));
        expect(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[1], new long[2]));
        expect(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[] {2}, new long[] {1}));
        expect(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[] {1}, new long[] {2}));
        expect(NullPointerException.class, () -> M3StateRelation.repeatNative(null, 0));
        expect(IllegalArgumentException.class, () -> M3StateRelation.repeatNative(new long[1], -1));
        expect(IllegalArgumentException.class, () -> M3StateRelation.repeatNative(new long[] {2}, 0));
        expect(IllegalArgumentException.class, () -> M3StateRelation.repeatNative(new long[0], 0));
    }
    private static void compactors() {
        Random random = new Random(0x4d334d41534bL);
        Object[] shared = {new Object(), new Object(), null, "same"};
        int[] lengths = {0, 1, 2, 63, 64, 65, 127, 128, 129, 511, 512, 513, 1023, 1024, 4097, 8193};
        for (int length : lengths) {
            for (int trial = 0; trial < 100; trial++) {
                int from = trial % 11;
                int to = from + length;
                Object[] input = new Object[to + 9];
                for (int i = 0; i < input.length; i++) input[i] = shared[random.nextInt(shared.length)];
                long[] mask = new long[(length + 63) >>> 6];
                for (int i = 0; i < mask.length; i++) {
                    mask[i] = switch (trial % 6) {
                        case 0 -> 0;
                        case 1 -> -1L;
                        case 2 -> 0x5555555555555555L;
                        case 3 -> i == 0 ? -1L : 0;
                        case 4 -> i % 3 == 0 ? -1L : 0;
                        default -> random.nextLong();
                    };
                }
                long[] maskBefore = mask.clone();
                Object[] expected = input.clone();
                int end = from;
                for (int i = 0; i < length; i++) {
                    if ((mask[i >>> 6] & (1L << (i & 63))) == 0) expected[end++] = input[from + i];
                }
                check(M3CollectionMaskCompactor.compact(input, from, to, mask) == end, "survivor end");
                for (int i = 0; i < input.length; i++) check(input[i] == expected[i], "reference/tail identity");
                check(Arrays.equals(maskBefore, mask), "mask mutation");
            }
        }
        Object[] values = {new Object(), new Object()};
        Object[] before = values.clone();
        expect(NullPointerException.class, () -> M3CollectionMaskCompactor.compact(null, 0, 0, new long[0]));
        expect(NullPointerException.class, () -> M3CollectionMaskCompactor.compact(values, 0, 0, null));
        expect(IndexOutOfBoundsException.class, () -> M3CollectionMaskCompactor.compact(values, -1, 0, new long[0]));
        expect(IllegalArgumentException.class, () -> M3CollectionMaskCompactor.compact(values, 0, 2, new long[0]));
        check(values[0] == before[0] && values[1] == before[1], "validation mutated output");
    }
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("absolute native library path required");
        System.load(args[0]);
        relations();
        compactors();
        System.out.println("PASS owner assertions=" + checks);
    }
}
