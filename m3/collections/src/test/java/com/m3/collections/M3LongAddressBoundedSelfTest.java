/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.collections;

import java.util.Arrays;

/** Real-owner finite oracle for duplicate/offset gathers and sorted, mask-selected projections. */
public final class M3LongAddressBoundedSelfTest {
    private static long checks;
    private static long fingerprint;
    private M3LongAddressBoundedSelfTest() { }

    public static void main(String[] args) { run(); }

    static void run() {
        checks = 0;
        fingerprint = 1;
        boolean nativeMode = M3LongAddressKernel.nativeAvailable();
        if (Boolean.getBoolean("m3.address.required") && !nativeMode) {
            throw new AssertionError("JNI required; fallback cannot qualify this test");
        }
        for (var backend : M3IndexMaskCompactor.Backend.values()) {
            if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED && !nativeMode) continue;
            for (int length : new int[] {0, 1, 2, 255, 256, 257, 511, 512, 513, 4097, 65537}) {
                for (int shape = 0; shape < 4; shape++) exercise(length, shape, backend);
            }
            invalid(backend);
            projected(backend);
        }
        for (int state = 0; state < 256; state++) {
            for (int required = 0; required < 256; required++) {
                int forbidden = (~required) & 0x55;
                boolean expected = (state & required) == required && (state & forbidden) == 0;
                check(M3LongAddressKernel.matches(new byte[] {(byte) state}, 0, required, forbidden)
                        == expected, "unsigned masks");
            }
        }
        if (!nativeMode) {
            expect(UnsatisfiedLinkError.class, () -> M3LongAddressKernel.gather(
                    new long[0], new int[0], 0, 0, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        System.out.println("M3_BOUNDED_GATHER_PASS checks=" + checks + " fingerprint=" + fingerprint
                + " native=" + nativeMode);
    }

    private static void exercise(int length, int shape, M3IndexMaskCompactor.Backend backend) {
        long[] payload = new long[Math.max(17, length + 19)];
        for (int i = 0; i < payload.length; i++) payload[i] = ((long) i << 33) ^ (i * -37L);
        payload[0] = Long.MIN_VALUE;
        payload[1] = Long.MAX_VALUE;
        int[] addresses = new int[length + 7];
        Arrays.fill(addresses, -1); // Out-of-window addresses must never be read.
        for (int i = 0; i < length; i++) {
            addresses[3 + i] = switch (shape) {
                case 0 -> i + 2;
                case 1 -> length - i;
                case 2 -> i % 3;
                default -> (int) (((long) i * 137 + 11) % payload.length);
            };
        }
        long[] before = payload.clone();
        int[] oldIndex = addresses.clone();
        long[] actual = new long[length + 11];
        Arrays.fill(actual, -901L);
        M3LongAddressKernel.gatherInto(payload, addresses, 3, length, actual, 5, backend);
        for (int i = 0; i < actual.length; i++) {
            long expected = i < 5 || i >= 5 + length ? -901L : payload[addresses[3 + i - 5]];
            equal(expected, actual[i]);
        }
        check(Arrays.equals(before, payload) && Arrays.equals(oldIndex, addresses), "input ownership");
        long[] detached = M3LongAddressKernel.gather(payload, addresses, 3, length, backend);
        check(detached != payload && detached.length == length, "detached output");
        for (int i = 0; i < length; i++) equal(actual[5 + i], detached[i]);

        // The contract writes the valid prefix before rejecting the first invalid physical address.
        for (int at : new int[] {0, 1, 254, 255, 256, 257, 511, 512, length - 1}) {
            if (at < 0 || at >= length) continue;
            int original = addresses[3 + at];
            for (int bad : new int[] {-1, payload.length, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                addresses[3 + at] = bad;
                Arrays.fill(actual, -901L);
                Throwable error = expect(backend == M3IndexMaskCompactor.Backend.JAVA
                        ? ArrayIndexOutOfBoundsException.class : IndexOutOfBoundsException.class,
                        () -> M3LongAddressKernel.gatherInto(payload, addresses, 3, length, actual, 5, backend));
                if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
                    check("physical address outside payload".equals(error.getMessage()), "native failure contract");
                }
                for (int i = 0; i < actual.length; i++) {
                    long expected = i >= 5 && i < 5 + at ? payload[addresses[i - 2]] : -901L;
                    equal(expected, actual[i]);
                }
            }
            addresses[3 + at] = original;
        }
    }

    private static void invalid(M3IndexMaskCompactor.Backend backend) {
        long[] source = {11, 12, 13};
        long[] output = {91, 92, 93};
        int[] addresses = {0, 1, 2};
        for (int[] r : new int[][] {{-1,1,0}, {0,-1,0}, {0,4,0}, {0,1,-1},
                {0,1,3}, {Integer.MAX_VALUE,1,0}, {0,Integer.MAX_VALUE,0}}) {
            expect(IndexOutOfBoundsException.class,
                    () -> M3LongAddressKernel.gatherInto(source, addresses, r[0], r[1], output, r[2], backend));
            check(Arrays.equals(output, new long[] {91,92,93}), "range preflight wrote output");
        }
        expect(IllegalArgumentException.class,
                () -> M3LongAddressKernel.gatherInto(source, addresses, 0, 0, source, 0, backend));
        expect(NullPointerException.class,
                () -> M3LongAddressKernel.gatherInto(null, addresses, 0, 1, output, 0, backend));
        expect(NullPointerException.class,
                () -> M3LongAddressKernel.gatherInto(source, null, 0, 1, output, 0, backend));
        expect(NullPointerException.class,
                () -> M3LongAddressKernel.gatherInto(source, addresses, 0, 1, null, 0, backend));
        expect(NullPointerException.class,
                () -> M3LongAddressKernel.gatherInto(source, addresses, 0, 1, output, 0, null));
    }

    private static void projected(M3IndexMaskCompactor.Backend backend) {
        long[] source = new long[777];
        for (int i = 0; i < source.length; i++) source[i] = (i * 73L) % 17 - 8;
        var snapshot = M3LongIndexProjection.snapshot(source);
        for (var ordered : new M3LongIndexProjection[] {snapshot, snapshot.sortedAscending(), snapshot.sortedDescending()}) {
            var slice = ordered.slice(11, 753).slice(7, 513);
            byte[] states = new byte[slice.size()];
            for (int i = 0; i < states.length; i++) states[i] = (byte) (i * 37);
            for (int mask = 0; mask < 256; mask++) {
                var selected = slice.filtered(states, mask, M3IndexMaskCompactor.Backend.JAVA);
                int[] ids = selected.physicalIndices();
                long[] gathered = M3LongAddressKernel.gather(source, ids, 0, ids.length, backend);
                int next = 0;
                for (int i = 0; i < states.length; i++) if (((states[i] & 255) & mask) == mask) {
                    check(selected.physicalIndex(next) == slice.physicalIndex(i), "index projection identity");
                    equal(selected.get(next), gathered[next]);
                    next++;
                }
                check(next == selected.size() && selected.order() == slice.order(), "order or count changed");
                long[] copy = selected.toArray();
                check(Arrays.equals(copy, gathered), "native sorted-filtered payload parity");
                if (copy.length != 0) { copy[0] = 12345; check(selected.get(0) != 12345, "escaped payload alias"); }
            }
        }
        long saved = snapshot.get(0);
        source[0] = 2222;
        equal(saved, snapshot.get(0));
    }

    private static Throwable expect(Class<? extends Throwable> kind, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(failure.getClass() == kind, "wrong exception: " + failure);
            return failure;
        }
        throw new AssertionError("missing " + kind.getName());
    }
    private static void equal(long expected, long actual) {
        check(expected == actual, "payload mismatch");
        fingerprint = fingerprint * 31 + actual;
    }
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
