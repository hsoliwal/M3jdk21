// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Java 21 proof of balanced explicit ownership across exceptions and threads. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3LeaseScopesSelfTest.java blob 236d58d87439; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class LeaseScopesSelfTest {
    private LeaseScopesSelfTest() { }

    public static void main(String[] args) throws Exception {
        nestedAndExceptionalScope();
        valueReturningAndPin();
        concurrentScope();
        System.out.println("LeaseScopesSelfTest PASS");
    }

    private static void nestedAndExceptionalScope() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            LeaseScopes.withLease(table, id, () -> {
                check(table.leaseCount(id) == 1, "outer lexical count");
                LeaseScopes.withLease(table, id, () -> {
                    check(table.leaseCount(id) == 2, "nested lexical count");
                });
                check(table.leaseCount(id) == 1, "inner finally release");
            });
            check(table.leaseCount(id) == 0, "outer finally release");
            try {
                LeaseScopes.withLease(table, id, (Runnable) () -> {
                    throw new IllegalStateException("callback failure");
                });
                throw new AssertionError("exception must propagate");
            } catch (IllegalStateException expected) {
                check("callback failure".equals(expected.getMessage()), "exact exception");
            }
            check(table.leaseCount(id) == 0 && table.eligible(id),
                    "exception cannot leak registered count");
        }
    }

    private static void valueReturningAndPin() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            int number = LeaseScopes.withLease(table, id, () -> 13);
            check(number == 13 && table.leaseCount(id) == 0, "return and release");
            LeaseScopes.withPin(table, id, () -> {
                check(table.pinCount(id) == 1, "pin held");
                check(!table.tryRetire(id), "cannot retire pinned handle");
            });
            check(table.pinCount(id) == 0 && table.tryRetire(id), "balanced pin");
        }
    }

    private static void concurrentScope() throws Exception {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            long id = table.allocate();
            CountDownLatch gate = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread[] workers = new Thread[4];
            for (int i = 0; i < workers.length; i++) {
                workers[i] = new Thread(() -> {
                    try {
                        gate.await();
                        for (int j = 0; j < 300; j++) {
                            LeaseScopes.withLease(table, id,
                                    () -> check(table.leaseCount(id) > 0, "in lexical scope"));
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    }
                });
                workers[i].start();
            }
            gate.countDown();
            for (Thread worker : workers) worker.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            check(table.leaseCount(id) == 0, "all callbacks balanced");
        }
    }

    private static void check(boolean accepted, String name) {
        if (!accepted) throw new AssertionError(name);
    }
}
