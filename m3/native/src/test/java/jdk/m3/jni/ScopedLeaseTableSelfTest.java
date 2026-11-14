// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Runnable with Java 21 javac alone; JUnit delegates to this immutable contract suite. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3ScopedLeaseTableSelfTest.java blob d8e6debed226; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class ScopedLeaseTableSelfTest {
    private ScopedLeaseTableSelfTest() { }

    public static void main(String[] args) throws Exception {
        basicTransitions();
        refusalAndClose();
        deterministicScan();
        concurrentLeases();
        jniArrayCounting();
        concurrentJniArrayCounting();
        poolOwnerRefcount();
        concurrentPoolRefcount();
        atomicPoolOwnershipTransfer();
        System.out.println("ScopedLeaseTableSelfTest PASS native="
                + NativeLeaseScan.nativeAvailable());
    }

    private static void basicTransitions() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long a = table.allocate();
            long b = table.allocate();
            check(a == 1L && b == 2L, "stable monotonic ids");
            table.acquire(a);
            table.pin(b);
            check(!table.eligible(a) && !table.eligible(b), "active holders deny trim");
            table.release(a);
            table.markDirty(a);
            check(!table.tryRetire(a), "dirty blocks retire");
            table.markCommitted(a);
            check(table.tryRetire(a), "clean empty handle retires");
            check(!table.eligible(b), "still pinned");
            table.unpin(b);
            check(table.eligible(b), "unpin enables candidate");
            refusal(() -> table.acquire(a), IllegalStateException.class);
            long c = table.allocate();
            check(c == 3L, "retired ID never recycled");
        }
    }

    private static void refusalAndClose() {
        ScopedLeaseTable table = new ScopedLeaseTable();
        long id = table.allocate();
        refusal(() -> table.release(id), IllegalStateException.class);
        refusal(() -> table.unpin(id), IllegalStateException.class);
        refusal(() -> table.acquire(0), IllegalArgumentException.class);
        refusal(() -> table.acquire(Long.MAX_VALUE), IllegalArgumentException.class);
        table.acquire(id);
        refusal(table::close, IllegalStateException.class);
        table.release(id);
        table.markDirty(id);
        refusal(table::close, IllegalStateException.class);
        table.markCommitted(id);
        long[] shortOutput = new long[0];
        refusal(() -> table.collectEligible(shortOutput), IllegalArgumentException.class);
        table.close();
        refusal(table::allocate, IllegalStateException.class);
    }

    private static void deterministicScan() {
        Random random = new Random(0x4d335f6c696665L);
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            final int count = 1000;
            for (int i = 0; i < count; i++) table.allocate();
            for (long id = 1; id <= count; id++) {
                switch (random.nextInt(4)) {
                    case 0 -> table.acquire(id);
                    case 1 -> table.pin(id);
                    case 2 -> table.markDirty(id);
                    default -> { }
                }
            }
            long[] javaIds = new long[count];
            long[] nativeIds = new long[count];
            int a = table.collectEligible(javaIds, false);
            int b = table.collectEligible(nativeIds, true);
            check(a == b, "candidate counts parity");
            check(Arrays.equals(Arrays.copyOf(javaIds, a), Arrays.copyOf(nativeIds, b)),
                    "Java/JNI stable-order parity");
            long previous = 0;
            for (int i = 0; i < a; i++) {
                check(javaIds[i] > previous, "stable sorted selection");
                check(table.eligible(javaIds[i]), "candidate follows registered state");
                previous = javaIds[i];
            }
            for (long id = 1; id <= count; id++) {
                if (table.leaseCount(id) != 0) table.release(id);
                if (table.pinCount(id) != 0) table.unpin(id);
                table.markCommitted(id);
            }
            check(table.collectEligible(nativeIds, true) == count, "all clean empty counts");
        }
    }

    private static void concurrentLeases() throws Exception {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            int workers = 6;
            Thread[] threads = new Thread[workers];
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            for (int i = 0; i < workers; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < 2000; j++) {
                            table.acquire(id);
                            table.release(id);
                        }
                    } catch (Throwable ex) {
                        failure.compareAndSet(null, ex);
                    }
                });
                threads[i].start();
            }
            start.countDown();
            for (Thread thread : threads) thread.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            check(table.leaseCount(id) == 0 && table.eligible(id), "concurrent balance");
        }
    }


    private static void jniArrayCounting() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long primitive = table.allocate();
            long references = table.allocate();
            refusal(() -> table.registerJniArray(primitive, new Object()),
                    IllegalArgumentException.class);
            refusal(() -> table.beginJniArrayAccess(primitive), IllegalStateException.class);
            table.registerJniArray(primitive, new int[4]);
            table.registerJniArray(references, new Object[3]);
            refusal(() -> table.registerJniArray(primitive, new byte[4]),
                    IllegalStateException.class);
            check(table.registeredJniArrayCount() == 2, "two registered arrays");
            table.beginJniArrayAccess(primitive);
            table.beginJniArrayAccess(primitive);
            table.beginJniArrayAccess(references);
            check(table.activeJniArrayCount() == 2, "distinct active arrays");
            check(table.jniArrayBorrowCount(primitive) == 2, "nested JNI borrows");
            check(table.jniArrayLeaseBegins() == 3L, "lifetime JNI lease count");
            check(table.pinCount(primitive) == 2, "JNI borrows counted as pins");
            refusal(() -> table.unpin(primitive), IllegalStateException.class);
            refusal(table::close, IllegalStateException.class);
            long[] results = new long[2];
            check(table.collectEligible(results, true) == 0,
                    "native candidate scan blocks outstanding JNI arrays");
            table.endJniArrayAccess(primitive);
            check(table.jniArrayBorrowCount(primitive) == 1, "one native lease remains");
            table.pin(primitive);
            table.unpin(primitive);
            refusal(() -> table.unpin(primitive), IllegalStateException.class);
            table.endJniArrayAccess(primitive);
            table.endJniArrayAccess(references);
            refusal(() -> table.endJniArrayAccess(primitive), IllegalStateException.class);
            check(table.activeJniArrayCount() == 0, "all JNI borrows returned");
            check(table.collectEligible(results, false) == 2, "Java candidates restored");
            check(table.collectEligible(results, true) == 2, "JNI candidates restored");
            check(table.tryRetire(references), "clean JNI array can retire on owner proof");
            check(table.registeredJniArrayCount() == 1, "retired array removed from count");
        }
    }

    private static void concurrentJniArrayCounting() throws Exception {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new long[7]);
            int workers = 4;
            Thread[] threads = new Thread[workers];
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            for (int i = 0; i < workers; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < 1_000; j++) {
                            table.beginJniArrayAccess(id);
                            try {
                                check(!table.eligible(id), "JNI native window cannot retire");
                            } finally {
                                table.endJniArrayAccess(id);
                            }
                        }
                    } catch (Throwable ex) {
                        failure.compareAndSet(null, ex);
                    }
                });
                threads[i].start();
            }
            start.countDown();
            for (Thread thread : threads) thread.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            check(table.activeJniArrayCount() == 0 && table.jniArrayBorrowCount(id) == 0,
                    "concurrent JNI windows balanced");
            check(table.jniArrayLeaseBegins() == workers * 1_000L, "all JNI access counted");
            check(table.pinCount(id) == 0 && table.eligible(id), "safe native lease exit");
        }
    }


    private static void poolOwnerRefcount() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            int[] array = new int[8];
            table.registerJniArray(id, array);
            check(table.managedReferenceCount(id) == 0L, "fresh array has no managed shares");
            refusal(() -> table.releasePoolReference(id), IllegalStateException.class);
            Object[] poolSlots = {array};
            table.retainPoolReference(id);
            check(poolSlots[0] == array, "pool physically holds the array");
            check(table.poolReferenceCount(id) == 1, "pool contributes exactly one");
            check(table.managedReferenceCount(id) == 1L, "pool-only count is one");
            check(table.poolReuseEligible(id), "pool-only is a candidate");
            check(!table.eligible(id), "cannot generically retire pool-held array");
            refusal(() -> table.retainPoolReference(id), IllegalStateException.class);
            refusal(() -> table.unpin(id), IllegalStateException.class);
            refusal(table::close, IllegalStateException.class);
            long[] candidates = new long[1];
            check(table.collectEligible(candidates, false) == 0, "Java scan excludes pool owner");
            check(table.collectEligible(candidates, true) == 0, "JNI scan excludes pool owner");

            table.acquire(id);
            check(table.managedReferenceCount(id) == 2L, "pool plus Java lease is two");
            check(!table.poolReuseEligible(id), "Java lease blocks pool reuse");
            table.beginJniArrayAccess(id);
            check(table.managedReferenceCount(id) == 3L, "pool + Java + JNI is three");
            refusal(() -> table.unpin(id), IllegalStateException.class);
            table.pin(id);
            check(table.managedReferenceCount(id) == 4L, "additional explicit pin counts");
            table.unpin(id);
            check(table.managedReferenceCount(id) == 3L, "reserved pins remain");
            table.endJniArrayAccess(id);
            check(table.managedReferenceCount(id) == 2L, "native release keeps Java and pool");
            table.release(id);
            check(table.managedReferenceCount(id) == 1L, "only pool share remains");
            check(table.poolReuseEligible(id), "pool can consider reuse");
            table.markDirty(id);
            check(!table.poolReuseEligible(id), "dirty owner refuses reuse");
            table.markCommitted(id);
            check(table.poolReuseEligible(id), "committed owner is candidate");

            poolSlots[0] = null;
            table.releasePoolReference(id);
            check(table.poolReferenceCount(id) == 0, "pool owner removed");
            check(table.managedReferenceCount(id) == 0L, "cooperative count returned to zero");
            check(table.eligible(id), "generic retire becomes possible");
            check(table.tryRetire(id), "retire after last managed owner release");
        }
    }

    private static void concurrentPoolRefcount() throws Exception {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.retainPoolReference(id);
            Thread[] threads = new Thread[4];
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < 1_000; j++) {
                            table.acquire(id);
                            try {
                                check(!table.poolReuseEligible(id), "live lease blocks rebind");
                            } finally {
                                table.release(id);
                            }
                        }
                    } catch (Throwable ex) {
                        failure.compareAndSet(null, ex);
                    }
                });
                threads[i].start();
            }
            start.countDown();
            for (Thread thread : threads) thread.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            check(table.managedReferenceCount(id) == 1L, "pool retains exactly one share");
            check(table.poolReuseEligible(id), "all borrowers have released");
            table.releasePoolReference(id);
            check(table.managedReferenceCount(id) == 0L, "pool releases last share");
        }
    }

    private static void atomicPoolOwnershipTransfer() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new int[16]);
            table.retainPoolReference(id);
            table.acquire(id);
            refusal(() -> table.borrowPoolReference(id), IllegalStateException.class);
            table.release(id);
            check(table.poolReuseEligible(id), "idle pool has exactly +1");

            for (int i = 0; i < 10_000; i++) {
                table.borrowPoolReference(id);
                check(table.poolReferenceCount(id) == 0, "pool slot transferred");
                check(table.managedReferenceCount(id) == 1L, "ownership share transferred");
                refusal(() -> table.release(id), IllegalStateException.class);
                refusal(() -> table.borrowPoolReference(id), IllegalStateException.class);
                check(!table.tryRetire(id), "checked-out handle cannot retire");
                long[] ids = new long[1];
                check(table.collectEligible(ids, false) == 0, "Java scan excludes checkout");
                if (i == 0) {
                    // Checkout moved the pool's +1 from pins to an exclusive lease.
                    // Releasing a non-idle pool owner must not decrement pins below zero.
                    refusal(() -> table.releasePoolReference(id), IllegalStateException.class);
                    check(table.pinCount(id) == 0, "invalid pool release preserves pin lane");
                    check(table.leaseCount(id) == 1, "invalid pool release preserves checkout");
                    check(table.managedReferenceCount(id) == 1L,
                            "invalid pool release preserves one managed owner");
                    table.pin(id);
                    refusal(() -> table.releasePoolReference(id), IllegalStateException.class);
                    check(table.pinCount(id) == 1, "invalid pool release preserves separate pin");
                    table.unpin(id);
                    table.beginJniArrayAccess(id);
                    check(table.managedReferenceCount(id) == 2L, "JNI borrow adds a share");
                    refusal(() -> table.returnPoolReference(id), IllegalStateException.class);
                    table.endJniArrayAccess(id);
                }
                table.returnPoolReference(id);
                check(table.poolReuseEligible(id), "clean physical pool slot returns");
            }
            check(table.size() == 1, "no new handle per checkout");
            table.borrowPoolReference(id);
            table.markDirty(id);
            refusal(() -> table.returnPoolReference(id), IllegalStateException.class);
            table.discardPoolBorrow(id);
            table.markCommitted(id);
            check(table.managedReferenceCount(id) == 0L, "discard releases final managed share");
            check(table.tryRetire(id), "retire only after exclusive borrower leaves");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void refusal(Runnable operation, Class<? extends Throwable> expected) {
        try {
            operation.run();
            throw new AssertionError("expected " + expected.getName());
        } catch (Throwable result) {
            if (!expected.isInstance(result)) throw result;
        }
    }
}
