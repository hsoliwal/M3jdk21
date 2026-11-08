// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Arrays;
import java.util.Random;

/** Standalone Java21 compiler-and-runtime proof, with optional JNI parity in the same process. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3ScopedLeaseWindowSelfTest.java blob 96a098376fb4; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class ScopedLeaseWindowSelfTest {
    private ScopedLeaseWindowSelfTest() { }

    public static void main(String[] args) {
        sparseAndDenseParity();
        zeroCandidatesAndEndCursor();
        refusalAndDirtyState();
        concurrentOwnerRecheck();
        System.out.println("ScopedLeaseWindowSelfTest PASS native="
                + NativeLeaseScan.nativeAvailable());
    }

    private static void sparseAndDenseParity() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            int count = 65536;
            Random random = new Random(0x4d33_2026_1014L);
            boolean[] retired = new boolean[count + 1];
            for (int i = 0; i < count; i++) {
                long id = table.allocate();
                switch (random.nextInt(6)) {
                    case 0 -> table.acquire(id);
                    case 1 -> table.pin(id);
                    case 2 -> table.markDirty(id);
                    case 3 -> {
                        check(table.tryRetire(id), "clean retirement");
                        retired[(int) id] = true;
                    }
                    default -> { }
                }
            }
            long[] expected = new long[count];
            int oldWritten = table.collectEligible(expected);
            for (int cap : new int[] {1, 2, 7, 127, 128, 129, 1024}) {
                long[] a = collect(table, count, cap, false);
                long[] b = collect(table, count, cap, true);
                check(a.length == oldWritten, "length parity for cap " + cap);
                check(Arrays.equals(a, Arrays.copyOf(expected, oldWritten)),
                        "existing full-table API parity cap " + cap);
                check(Arrays.equals(a, b), "Java/JNI window parity cap " + cap);
            }
            // Release all explicitly managed native/page leases; retired IDs stay retired.
            for (long id = 1; id <= count; id++) {
                if (retired[(int) id]) continue;
                if (table.leaseCount(id) != 0) table.release(id);
                if (table.pinCount(id) != 0) table.unpin(id);
                table.markCommitted(id);
            }
        }
    }

    private static long[] collect(ScopedLeaseTable table, int count, int capacity,
                                  boolean preferNative) {
        long[] out = new long[capacity];
        long[] received = new long[count];
        int first = 0;
        int total = 0;
        int steps = 0;
        while (first < count) {
            long result = LeaseWindowScan.scan(table, first, out, preferNative);
            int next = LeaseWindowScan.nextIndex(result);
            int n = LeaseWindowScan.written(result);
            check(n >= 0 && n <= out.length, "bounded copied count");
            check(next > first && next <= count, "strict scan progress");
            System.arraycopy(out, 0, received, total, n);
            total += n;
            first = next;
            check(++steps <= count, "no stalled cursor");
        }
        return Arrays.copyOf(received, total);
    }

    private static void zeroCandidatesAndEndCursor() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long[] one = new long[] {Long.MIN_VALUE};
            long empty = LeaseWindowScan.scan(table, 0, one, true);
            check(LeaseWindowScan.nextIndex(empty) == 0
                    && LeaseWindowScan.written(empty) == 0,
                    "empty table has empty result");
            for (int i = 0; i < 300; i++) {
                long id = table.allocate();
                table.acquire(id);
            }
            long packed = LeaseWindowScan.scan(table, 0, one, true);
            check(LeaseWindowScan.nextIndex(packed) == table.size()
                    && LeaseWindowScan.written(packed) == 0,
                    "busy slots scanned with no candidates");
            check(one[0] == Long.MIN_VALUE, "zero-candidate scan does not write output");
            packed = LeaseWindowScan.scan(table, table.size(), one, false);
            check(LeaseWindowScan.written(packed) == 0
                    && LeaseWindowScan.nextIndex(packed) == table.size(),
                    "terminal cursor is stable");
            for (int i = 1; i <= 300; i++) table.release(i);
        }
    }

    private static void refusalAndDirtyState() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            long[] one = {0};
            fail(() -> LeaseWindowScan.scan(table, -1, one, false),
                    IllegalArgumentException.class);
            fail(() -> LeaseWindowScan.scan(table, 2, one, false),
                    IllegalArgumentException.class);
            fail(() -> LeaseWindowScan.scan(table, 0, new long[0], false),
                    IllegalArgumentException.class);
            fail(() -> LeaseWindowScan.scan(table, 0, null, false),
                    NullPointerException.class);
            table.markDirty(id);
            long result = LeaseWindowScan.scan(table, 0, one, true);
            check(LeaseWindowScan.written(result) == 0, "dirty never appears eligible");
            table.markCommitted(id);
            result = LeaseWindowScan.scan(table, 0, one, true);
            check(LeaseWindowScan.written(result) == 1 && one[0] == id,
                    "committed and unleased may be a candidate");
        }
        long[] ids = {1, 2, 3};
        int[] zero = {0, 0, 0};
        byte[] invalid = {0, -1, 0};
        for (boolean nativeMode : new boolean[]{false, true}) {
            fail(() -> NativeLeaseScan.collectWindow(ids, zero, zero, invalid,
                    3, 0, new long[3], nativeMode), IllegalArgumentException.class);
            fail(() -> NativeLeaseScan.collectWindow(ids, zero, zero, new byte[3],
                    3, 0, ids, nativeMode), IllegalArgumentException.class);
        }
        check(LeaseWindowScan.nextIndex(NativeLeaseScan.packWindow(19, 4)) == 19,
                "cursor packing roundtrip");
        check(LeaseWindowScan.written(NativeLeaseScan.packWindow(19, 4)) == 4,
                "written packing roundtrip");
    }

    private static void concurrentOwnerRecheck() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            long[] one = new long[1];
            long packed = LeaseWindowScan.scan(table, 0, one, true);
            check(LeaseWindowScan.written(packed) == 1, "candidate observed");
            table.acquire(id);
            check(!table.tryRetire(id), "candidate never authorizes retirement after re-lease");
            table.release(id);
            table.pin(id);
            check(!table.tryRetire(id), "pin wins over stale candidate");
            table.unpin(id);
            table.markDirty(id);
            check(!table.tryRetire(id), "dirty wins over stale candidate");
            table.markCommitted(id);
            check(table.tryRetire(id), "owner explicitly validates and retires");
        }
    }

    private static void fail(Runnable run, Class<? extends Throwable> type) {
        try {
            run.run();
            throw new AssertionError("expected " + type.getSimpleName());
        } catch (Throwable thrown) {
            if (!type.isInstance(thrown)) throw thrown;
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
