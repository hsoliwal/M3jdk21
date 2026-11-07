// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Paired native array source leases share the single canonical owner monitor. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/7627894f9879c025a2cb2ceece536e3918259189/synexia-primitives/src/test/java/com/synexia/primitives/M3JniPairMonitorAtomicityTest.java blob 0302c1701c3d; recipe m3/native/qualification/twin-fixes-20261107/apply.py v1
final class M3JniPairMonitorAtomicityTest {
    private static void stress(boolean alias) throws InterruptedException {
        try (var table = new M3ScopedLeaseTable()) {
            long left = table.allocate();
            table.registerJniArray(left, new byte[1]);
            long right = alias ? left : table.allocate();
            if (!alias) table.registerJniArray(right, new byte[1]);
            CountDownLatch go = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread writer = new Thread(() -> {
                try {
                    go.await();
                    for (int i = 0; i < 30_000; i++) {
                        M3JniArrayReferences.retainSources(table, left, right);
                        M3JniArrayReferences.releaseSources(table, left, right);
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            }, "M3-pair-writer");
            Thread observer = new Thread(() -> {
                try {
                    go.await();
                    for (int i = 0; i < 30_000; i++) {
                        synchronized (table) {
                            int a = table.jniArrayBorrowCount(left);
                            int b = table.jniArrayBorrowCount(right);
                            if (alias) {
                                if (a != 0 && a != 2) throw new AssertionError("half alias");
                                if (table.activeJniArrayCount() != (a == 0 ? 0 : 1))
                                    throw new AssertionError("alias active count");
                            } else {
                                if (a != b || a < 0 || a > 1) throw new AssertionError("half pair");
                                if (table.activeJniArrayCount() != a + b)
                                    throw new AssertionError("pair active count");
                            }
                        }
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            }, "M3-pair-observer");
            writer.start(); observer.start(); go.countDown();
            writer.join(60_000); observer.join(60_000);
            assertFalse(writer.isAlive(), "writer not finished");
            assertFalse(observer.isAlive(), "observer not finished");
            if (failure.get() != null) throw new AssertionError(failure.get());
            assertEquals(0, table.jniArrayBorrowCount(left));
            assertEquals(0, table.jniArrayBorrowCount(right));
            assertEquals(0, table.activeJniArrayCount());
            assertTrue(table.tryRetire(left));
            if (!alias) assertTrue(table.tryRetire(right));
        }
    }
    @Test void atomicTwoSourceObservation() throws InterruptedException { stress(false); }
    @Test void atomicAliasedTwoOccurrenceObservation() throws InterruptedException { stress(true); }

    @Test void secondInvalidSourceRollsBackFirst() {
        try (var table = new M3ScopedLeaseTable()) {
            long first = table.allocate(), retired = table.allocate();
            table.registerJniArray(first, new byte[1]);
            assertTrue(table.tryRetire(retired));
            assertThrows(IllegalStateException.class,
                    () -> M3JniArrayReferences.retainSources(table, first, retired));
            assertEquals(0, table.jniArrayBorrowCount(first));
            assertTrue(table.eligible(first));
        }
    }
    @Test void reverseReleaseRetainsBothFailures() {
        try (var table = new M3ScopedLeaseTable()) {
            long left = table.allocate(), right = table.allocate();
            table.registerJniArray(left, new byte[1]);
            table.registerJniArray(right, new byte[1]);
            M3JniArrayReferences.retainSources(table, left, right);
            table.endJniArrayAccess(right);
            table.endJniArrayAccess(left);
            var primary = assertThrows(IllegalStateException.class,
                    () -> M3JniArrayReferences.releaseSources(table, left, right));
            assertTrue(primary.getMessage().contains(" " + right));
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0].getMessage().contains(" " + left));
        }
    }
    @Test void nestedWithConcatPreservesIdentityAndCounts() {
        try (var table = new M3ScopedLeaseTable()) {
            long left = table.allocate(), right = table.allocate();
            table.registerJniArray(left, new byte[1]);
            table.registerJniArray(right, new byte[1]);
            Object result = new Object();
            assertSame(result, M3JniArrayReferences.withConcat(table, left, right, () -> {
                assertEquals(1, table.jniArrayBorrowCount(left));
                M3JniArrayReferences.withConcat(table, left, left, (Runnable) () ->
                        assertEquals(3, table.jniArrayBorrowCount(left)));
                assertEquals(1, table.jniArrayBorrowCount(left));
                return result;
            }));
            assertEquals(0, table.activeJniArrayCount());
        }
    }
    @Test void aliasedPairBlocksRetirementUntilBothReleases() {
        try (var table = new M3ScopedLeaseTable()) {
            long id = table.allocate();
            table.registerJniArray(id, new byte[1]);
            M3JniArrayReferences.withConcat(table, id, id, (Runnable) () -> {
                assertEquals(2, table.jniArrayBorrowCount(id));
                assertFalse(table.tryRetire(id));
            });
            assertTrue(table.tryRetire(id));
        }
    }
}
