/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * @test
 * @summary Concurrent first use and repeated actual-JVMTI shallow sizing
 * @modules java.base/jdk.internal.m3
 * @run main/othervm -ea -esa -Xcheck:jni M3ObjectMetricsCacheTest
 */
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.m3.M3ObjectMetrics;

public final class M3ObjectMetricsCacheTest {
    public static void main(String[] args) throws Exception {
        int threads = 16;
        int rounds = 1000;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                try {
                    byte[] small = new byte[0];
                    byte[] large = new byte[8192];
                    Object plain = new Object();
                    ready.countDown();
                    start.await();
                    long smallSize = M3ObjectMetrics.shallowSize(small);
                    long largeSize = M3ObjectMetrics.shallowSize(large);
                    long objectSize = M3ObjectMetrics.shallowSize(plain);
                    if (smallSize <= 0 || largeSize < smallSize + 8192 || objectSize <= 0) {
                        throw new AssertionError("invalid actual VM sizes");
                    }
                    for (int n = 0; n < rounds; n++) {
                        if (M3ObjectMetrics.shallowSize(small) != smallSize
                                || M3ObjectMetrics.shallowSize(large) != largeSize
                                || M3ObjectMetrics.shallowSize(plain) != objectSize) {
                            throw new AssertionError("concurrent size changed for fixed-layout objects");
                        }
                    }
                } catch (Throwable problem) {
                    failure.compareAndSet(null, problem);
                }
            }, "m3-object-size-" + i);
            workers[i].start();
        }
        ready.await();
        start.countDown();
        for (Thread worker : workers) worker.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        try {
            M3ObjectMetrics.shallowSize(null);
            throw new AssertionError("missing null rejection");
        } catch (NullPointerException expected) {
            if (!"object".equals(expected.getMessage())) throw new AssertionError(expected);
        }
        System.out.println("M3ObjectMetricsCacheTest PASS threads=" + threads
                + " nativeSizeCalls=" + threads * 3 * (rounds + 1));
    }
}
