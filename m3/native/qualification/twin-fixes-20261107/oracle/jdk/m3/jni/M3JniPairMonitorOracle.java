// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Actual production-owner Java21 stress; no native pointer acquired. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/7627894f9879c025a2cb2ceece536e3918259189/synexia-openrewrite-recipes/recipe-crates/m3-jni-pair-monitor-atomicity-20261107/oracle/M3JniPairMonitorOracle.java blob 227a1d60e002; recipe m3/native/qualification/twin-fixes-20261107/apply.py v1
public final class M3JniPairMonitorOracle {
    private M3JniPairMonitorOracle() { }
    private static void require(boolean yes, String why) {
        if (!yes) throw new AssertionError(why);
    }
    private static void run(boolean alias) throws InterruptedException {
        try (var table = new M3ScopedLeaseTable()) {
            long left = table.allocate();
            table.registerJniArray(left, new byte[4]);
            long right = alias ? left : table.allocate();
            if (!alias) table.registerJniArray(right, new byte[4]);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread writer = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 100_000; i++) {
                        M3JniArrayReferences.retainSources(table, left, right);
                        M3JniArrayReferences.releaseSources(table, left, right);
                    }
                } catch (Throwable failure) { error.compareAndSet(null, failure); }
            });
            Thread observer = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 100_000; i++) {
                        synchronized (table) {
                            int a = table.jniArrayBorrowCount(left);
                            int b = table.jniArrayBorrowCount(right);
                            if (alias) {
                                require(a == 0 || a == 2, "half alias");
                                require(table.activeJniArrayCount() == (a == 0 ? 0 : 1),
                                        "alias active");
                            } else {
                                require(a == b && (a == 0 || a == 1), "half pair");
                                require(table.activeJniArrayCount() == a + b, "active pair");
                            }
                        }
                    }
                } catch (Throwable failure) { error.compareAndSet(null, failure); }
            });
            writer.start(); observer.start(); start.countDown();
            writer.join(120_000); observer.join(120_000);
            require(!writer.isAlive() && !observer.isAlive(), "stalled threads");
            if (error.get() != null) throw new AssertionError(error.get());
            require(table.activeJniArrayCount() == 0, "borrow leak");
            require(table.tryRetire(left), "left should retire");
            if (!alias) require(table.tryRetire(right), "right should retire");
        }
    }
    private static void errors() {
        try (var table = new M3ScopedLeaseTable()) {
            long left = table.allocate(), missing = table.allocate();
            table.registerJniArray(left, new byte[1]);
            require(table.tryRetire(missing), "setup");
            try { M3JniArrayReferences.retainSources(table, left, missing);
                throw new AssertionError("missing ID admitted");
            } catch (IllegalStateException expected) {
                require(table.jniArrayBorrowCount(left) == 0, "failed rollback");
            }
        }
    }
    public static void main(String[] args) throws InterruptedException {
        run(false); run(true); errors();
        System.out.println("PASS: production M3 source 100000 two-ID + 100000 alias JNI borrow pairs");
    }
}
