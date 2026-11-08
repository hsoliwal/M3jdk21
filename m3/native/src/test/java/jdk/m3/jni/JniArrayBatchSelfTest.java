// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Actual ScopedLeaseTable integration oracle; Java21 standalone, no test framework.
 * The JNI selection is tested in Java and preferNative modes. A native library is optional;
 * nativeAvailable=false does not establish native ABI qualification.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3JniArrayBatchSelfTest.java blob 77c00f8e94b4; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class JniArrayBatchSelfTest {
    private static long checks;
    private JniArrayBatchSelfTest() { }

    public static void main(String[] args) throws Exception {
        repeatedAndMutatedInputs();
        rollbackAndFailureCausality();
        boundsAndCleanup();
        nativeScanAndRandomizedDifferential();
        concurrentBorrowers();
        batchPublicationIsAtomicToIndependentReaders();
        System.out.println("M3_JNI_ARRAY_BATCH_PASS checks=" + checks
                + " nativeAvailable=" + NativeLeaseScan.nativeAvailable());
    }

    private static void repeatedAndMutatedInputs() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long a = array(table), b = array(table), c = array(table);
            long[] requested = {a, b, a};
            JniArrayReferences.withArrayBatch(table, requested, (Runnable) () -> {
                check(table.jniArrayBorrowCount(a) == 2, "aliased source counts twice");
                check(table.jniArrayBorrowCount(b) == 1, "second source counted");
                check(table.jniArrayLeaseBegins() == 3, "one borrow per array occurrence");
                check(!table.eligible(a) && !table.eligible(b), "do not retire borrowed arrays");
                requested[0] = c; // must not change frozen release coordinates
            });
            check(table.jniArrayBorrowCount(a) == 0, "frozen input released original a");
            check(table.jniArrayBorrowCount(b) == 0, "frozen input released b");
            check(table.jniArrayBorrowCount(c) == 0, "mutation did not acquire c");
            int result = JniArrayReferences.withArrayBatch(table, new long[]{c, c},
                    () -> table.jniArrayBorrowCount(c));
            check(result == 2, "returning callback sees all borrows");
            check(table.jniArrayBorrowCount(c) == 0, "returning callback balanced");
        }
    }

    private static void rollbackAndFailureCausality() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long a = array(table), b = array(table), retired = array(table);
            check(table.tryRetire(retired), "retired ID fixture");
            boolean callbackReached = false;
            try {
                JniArrayReferences.withArrayBatch(table, new long[]{a, b, a, retired},
                        (Runnable) () -> { throw new AssertionError("must not run"); });
            } catch (IllegalStateException expected) {
                callbackReached = true;
            }
            check(callbackReached, "failed fourth borrow rejected");
            check(table.jniArrayBorrowCount(a) == 0
                    && table.jniArrayBorrowCount(b) == 0,
                    "reverse rollback releases duplicated prefix");

            IllegalArgumentException primary = new IllegalArgumentException("native callback");
            try {
                JniArrayReferences.withArrayBatch(table, new long[]{a, b},
                        (Runnable) () -> {
                            table.endJniArrayAccess(b); // illegal premature JNI release fixture
                            throw primary;
                        });
                throw new AssertionError("expected callback failure");
            } catch (IllegalArgumentException observed) {
                check(observed == primary, "callback primary not overwritten by failed cleanup");
                check(observed.getSuppressed().length == 1, "one failed release suppressed");
            }
            check(table.jniArrayBorrowCount(a) == 0
                    && table.jniArrayBorrowCount(b) == 0,
                    "remaining JNI edge released even after cleanup fault");
            refuses(() -> JniArrayReferences.withArrayBatch(table, new long[]{a, b},
                    (Runnable) () -> table.endJniArrayAccess(b)),
                    IllegalStateException.class);
            check(table.eligible(a) && table.eligible(b), "cleanup-only failure leaves no pins");
        }
    }

    private static void boundsAndCleanup() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long a = array(table);
            refuses(() -> JniArrayReferences.withArrayBatch(table, new long[0],
                    (Runnable) () -> { }), IllegalArgumentException.class);
            refuses(() -> JniArrayReferences.withArrayBatch(table, new long[]{0L},
                    (Runnable) () -> { }), IllegalArgumentException.class);
            refuses(() -> JniArrayReferences.withArrayBatch(table, new long[]{a, -1},
                    (Runnable) () -> { }), IllegalArgumentException.class);
            refuses(() -> JniArrayReferences.withArrayBatch(table,
                    new long[JniArrayReferences.MAX_SCOPED_ARRAYS + 1],
                    (Runnable) () -> { }), IllegalArgumentException.class);
            refuses(() -> JniArrayReferences.withArrayBatch(table, null,
                    (Runnable) () -> { }), NullPointerException.class);
            refuses(() -> JniArrayReferences.withArrayBatch(table, new long[]{a},
                    (Runnable) null), NullPointerException.class);
            refuses(() -> JniArrayReferences.withArrayBatch(null, new long[]{a},
                    (Runnable) () -> { }), NullPointerException.class);
            long[] full = new long[JniArrayReferences.MAX_SCOPED_ARRAYS];
            Arrays.fill(full, a);
            JniArrayReferences.withArrayBatch(table, full, (Runnable) () ->
                    check(table.jniArrayBorrowCount(a) == full.length,
                            "max bounded repeated source"));
            check(table.jniArrayBorrowCount(a) == 0, "max batch balanced");
        }
    }

    private static void nativeScanAndRandomizedDifferential() {
        Random random = new Random(0x4d334a4e49L);
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long[] resources = new long[6];
            for (int i = 0; i < resources.length; i++) resources[i] = array(table);
            long[] javaSelected = new long[resources.length];
            long[] nativeSelected = new long[resources.length];
            for (int trial = 0; trial < 5000; trial++) {
                int n = 1 + random.nextInt(24);
                long[] sourceIds = new long[n];
                int[] occurrences = new int[resources.length];
                for (int i = 0; i < n; i++) {
                    int k = random.nextInt(resources.length);
                    sourceIds[i] = resources[k];
                    occurrences[k]++;
                }
                JniArrayReferences.withArrayBatch(table, sourceIds, (Runnable) () -> {
                    int distinct = 0;
                    for (int j = 0; j < resources.length; j++) {
                        check(table.jniArrayBorrowCount(resources[j]) == occurrences[j],
                                "per-edge count during JNI window");
                        if (occurrences[j] != 0) {
                            distinct++;
                            check(!table.eligible(resources[j]), "JNI-held resource not eligible");
                        }
                    }
                    int countJava = table.collectEligible(javaSelected, false);
                    int countNative = table.collectEligible(nativeSelected, true);
                    check(countJava == countNative && countJava == resources.length - distinct,
                            "native/Java eligible cardinality");
                    check(Arrays.equals(Arrays.copyOf(javaSelected, countJava),
                            Arrays.copyOf(nativeSelected, countNative)),
                            "native/Java eligible sorted IDs");
                });
                for (long id : resources) check(table.jniArrayBorrowCount(id) == 0,
                        "randomized JNI batch leaves no outstanding lease");
            }
        }
    }

    private static void concurrentBorrowers() throws Exception {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long a = array(table), b = array(table);
            Thread[] threads = new Thread[4];
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            for (int t = 0; t < threads.length; t++) {
                threads[t] = new Thread(() -> {
                    try {
                        start.await();
                        for (int trial = 0; trial < 1500; trial++) {
                            JniArrayReferences.withArrayBatch(table, new long[]{a, b, a},
                                    (Runnable) () -> {
                                        check(!table.eligible(a), "concurrent a pinned");
                                        check(!table.eligible(b), "concurrent b pinned");
                                    });
                        }
                    } catch (Throwable ex) {
                        failure.compareAndSet(null, ex);
                    }
                });
                threads[t].start();
            }
            start.countDown();
            for (Thread thread : threads) thread.join();
            if (failure.get() != null) throw new AssertionError("concurrent failure", failure.get());
            check(table.jniArrayBorrowCount(a) == 0
                    && table.jniArrayBorrowCount(b) == 0, "concurrent balanced");
        }
    }


    /**
     * Under a shared original-owner monitor, readers must see either the whole
     * four-occurrence reservation or the whole release. This is different
     * from count equality after the batch callback finishes.
     */
    private static void batchPublicationIsAtomicToIndependentReaders() throws Exception {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long a = array(table), b = array(table), c = array(table);
            long[] batch = {a, b, a, c};
            CountDownLatch start = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicBoolean finished =
                    new java.util.concurrent.atomic.AtomicBoolean();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread writer = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 16_000; i++) {
                        JniArrayReferences.withArrayBatch(table, batch, (Runnable) () -> {
                            synchronized (table) {
                                check(table.jniArrayBorrowCount(a) == 2
                                        && table.jniArrayBorrowCount(b) == 1
                                        && table.jniArrayBorrowCount(c) == 1,
                                        "all batch counts visible within native callback");
                            }
                        });
                        if ((i & 63) == 0) Thread.yield();
                    }
                } catch (Throwable ex) {
                    failure.compareAndSet(null, ex);
                } finally {
                    finished.set(true);
                }
            }, "m3-doop-batch-atomic-writer");
            Runnable observer = () -> {
                try {
                    start.await();
                    while (!finished.get()) {
                        synchronized (table) {
                            int x = table.jniArrayBorrowCount(a);
                            int y = table.jniArrayBorrowCount(b);
                            int z = table.jniArrayBorrowCount(c);
                            boolean empty = x == 0 && y == 0 && z == 0;
                            boolean full = x == 2 && y == 1 && z == 1;
                            check(empty || full,
                                    "no reader can see partial batch acquire/release");
                        }
                        Thread.yield();
                    }
                } catch (Throwable ex) {
                    failure.compareAndSet(null, ex);
                }
            };
            Thread first = new Thread(observer, "m3-doop-batch-atomic-reader-1");
            Thread second = new Thread(observer, "m3-doop-batch-atomic-reader-2");
            writer.start();
            first.start();
            second.start();
            start.countDown();
            writer.join();
            first.join();
            second.join();
            if (failure.get() != null) throw new AssertionError(
                    "atomic batch publication regression", failure.get());
            synchronized (table) {
                check(table.jniArrayBorrowCount(a) == 0
                        && table.jniArrayBorrowCount(b) == 0
                        && table.jniArrayBorrowCount(c) == 0,
                        "writer returns all original JNI leases");
            }
        }
    }

    private static long array(ScopedLeaseTable table) {
        long id = table.allocate();
        table.registerJniArray(id, new byte[8]);
        return id;
    }

    private static void refuses(Runnable action, Class<? extends Throwable> expected) {
        try { action.run(); }
        catch (Throwable actual) {
            if (expected.isInstance(actual)) { checks++; return; }
            throw actual;
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static synchronized void check(boolean actual, String message) {
        checks++;
        if (!actual) throw new AssertionError(message);
    }
}
