/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
/*
 * @test
 * @summary JNI notifications preserve weak owner identity after hash-index churn
 * @compile --patch-module java.base=${test.src} --add-reads java.base=ALL-UNNAMED M3ObjectCacheNativeStressTest.java
 * @run main/othervm/native --patch-module java.base=${test.classes} --add-reads java.base=ALL-UNNAMED -Xcheck:jni -Djdk.m3.objectEvents.enabled=false java.lang.M3ObjectCacheNativeStressTest disabled
 * @run main/othervm/native --patch-module java.base=${test.classes} --add-reads java.base=ALL-UNNAMED -Xcheck:jni -Djdk.m3.objectEvents.enabled=true java.lang.M3ObjectCacheNativeStressTest enabled
 */
package java.lang;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class M3ObjectCacheNativeStressTest {
    private static native boolean notifyContent(Object source);

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        // Boot-layer test class: System.loadLibrary would search sun.boot.library.path.
        System.load(java.nio.file.Path.of(System.getProperty("java.library.path"),
                System.mapLibraryName("M3ObjectCacheNativeStressTest")).toString());
        boolean enabled = args.length == 1 && "enabled".equals(args[0]);
        check(M3Object.observationEnabled() == enabled, "startup observation policy");

        final int owners = 4096;
        Object[] sources = new Object[owners];
        M3Object[] retained = new M3Object[owners];
        for (int i = 0; i < owners; i++) {
            sources[i] = new Object();
            retained[i] = M3Object.of(sources[i]);
        }
        Random permutation = new Random(0x4D334F424AL);
        for (int i = 0; i < owners * 3; i++) {
            int slot = permutation.nextInt(owners);
            check(M3Object.of(sources[slot]) == retained[slot], "indexed owner lookup");
            check(notifyContent(sources[slot]), "unobserved JNI notification");
        }

        Object trackedSource = new Object();
        M3Object tracked = M3Object.of(trackedSource);
        CountDownLatch delivery = new CountDownLatch(1);
        ObjectSubscription subscription = tracked.listen(ObjectEvent.CONTENT,
                event -> delivery.countDown());
        try {
            check(subscription.isClosed() != enabled, "enabled vs disabled subscription");
            check(notifyContent(trackedSource), "tracked JNI notification");
            if (enabled) {
                check(delivery.await(20, TimeUnit.SECONDS), "asynchronous JNI delivery");
            } else {
                check(delivery.getCount() == 1L, "disabled notification delivered");
            }
            final int threads = 4;
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            for (int t = 0; t < threads; t++) {
                final int seed = t + 23;
                new Thread(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        Random random = new Random(seed);
                        for (int i = 0; i < 2000; i++) {
                            int slot = random.nextInt(owners);
                            if (M3Object.of(sources[slot]) != retained[slot]
                                    || !notifyContent(sources[slot])) {
                                throw new AssertionError("JNI churn or identity failure");
                            }
                        }
                    } catch (Throwable problem) {
                        failure.compareAndSet(null, problem);
                    } finally {
                        done.countDown();
                    }
                }, "m3-native-lookup-" + t).start();
            }
            ready.await();
            start.countDown();
            check(done.await(40, TimeUnit.SECONDS), "JNI worker deadline");
            if (failure.get() != null) throw new AssertionError("concurrent JNI lookup", failure.get());
            check(M3Object.of(trackedSource) == tracked, "subscribed owner unchanged");
            check(notifyContent(trackedSource), "post-churn JNI notification");
        } finally {
            subscription.close();
        }
        System.out.println("PASS M3ObjectCacheNativeStressTest JNI owner index " + (enabled ? "enabled" : "disabled"));
    }
}
