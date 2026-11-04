/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */

/*
 * @test
 * @summary Retained Object.toAdapter M3Object identity remains canonical across Java/JNI threads
 * @run main/othervm/native -Xcheck:jni -Djdk.m3.objectEvents.enabled=false ObjectAdapterConcurrentIdentityTest
 * @run main/othervm/native -Xcheck:jni -Djdk.m3.objectEvents.enabled=true ObjectAdapterConcurrentIdentityTest
 * @run main/othervm/native -Xint -Xcheck:jni -Djdk.m3.objectEvents.enabled=false ObjectAdapterConcurrentIdentityTest
 * @run main/othervm/native -Xint -Xcheck:jni -Djdk.m3.objectEvents.enabled=true ObjectAdapterConcurrentIdentityTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ObjectAdapterConcurrentIdentityTest {
    private static final int EQUAL_RECEIVERS = 16;
    private static final int COLD_RECEIVERS = 64;
    private static final int WORKERS = 6;
    private static final int ROUNDS = 128;

    private static final class EqualButDistinct {
        final int ordinal;

        EqualButDistinct(int ordinal) {
            this.ordinal = ordinal;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualButDistinct;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static native Object adaptNative(Object receiver, Class<?> requested, int mode);

    public static void main(String[] args) throws Exception {
        System.loadLibrary("ObjectAdapterConcurrentIdentityTest");
        nativeInvocationForms();
        coldCreationRace();

        Object[] sources = new Object[EQUAL_RECEIVERS + 3];
        for (int i = 0; i < EQUAL_RECEIVERS; i++) {
            sources[i] = new EqualButDistinct(i);
        }
        sources[EQUAL_RECEIVERS] = new int[] {1};
        sources[EQUAL_RECEIVERS + 1] = new String[] {"m3"};
        sources[EQUAL_RECEIVERS + 2] = new Object[0];

        M3Object[] retained = new M3Object[sources.length];
        for (int i = 0; i < sources.length; i++) {
            retained[i] = sources[i].toAdapter(M3Object.class);
            check(retained[i] != null && retained[i].source() == sources[i],
                    "initial retained capability identity at " + i);
        }
        check(sources[0] != sources[1] && sources[0].equals(sources[1]),
                "equal distinct sources are a useful identity adversary");

        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        CountDownLatch ready = new CountDownLatch(WORKERS);
        CountDownLatch begin = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        long completed = 0;
        try {
            for (int worker = 0; worker < WORKERS; worker++) {
                final int workerIndex = worker;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    check(begin.await(30, TimeUnit.SECONDS), "start latch timed out");
                    long verified = 0;
                    for (int round = 0; round < ROUNDS; round++) {
                        for (int index = 0; index < sources.length; index++) {
                            int slot = (index + workerIndex + round) % sources.length;
                            Object source = sources[slot];
                            M3Object owner = retained[slot];
                            if (source.toAdapter(M3Object.class) != owner
                                    || M3Object.of(source) != owner
                                    || adaptNative(source, M3Object.class, workerIndex % 3) != owner) {
                                throw new AssertionError("capability split at slot " + slot);
                            }
                            if (owner.source() != source
                                    || source.toAdapter(Object.class) != source
                                    || adaptNative(source, Object.class, workerIndex % 3) != source) {
                                throw new AssertionError("receiver identity at slot " + slot);
                            }
                            if (source.toAdapter(Runnable.class) != null
                                    || adaptNative(source, Runnable.class, workerIndex % 3) != null) {
                                throw new AssertionError("unsupported request at slot " + slot);
                            }
                            verified++;
                        }
                    }
                    return verified;
                }));
            }
            check(ready.await(30, TimeUnit.SECONDS), "workers did not become ready");
            begin.countDown();
            for (Future<Long> future : futures) {
                completed += future.get(120, TimeUnit.SECONDS);
            }
        } finally {
            begin.countDown();
            executor.shutdownNow();
            check(executor.awaitTermination(30, TimeUnit.SECONDS),
                    "workers did not terminate");
        }

        check(completed == (long) WORKERS * ROUNDS * sources.length,
                "incomplete deterministic work: " + completed);
        for (int i = 0; i < sources.length; i++) {
            check(retained[i].source() == sources[i], "owner changed source at " + i);
            check(sources[i].toAdapter(M3Object.class) == retained[i],
                    "retained capability changed at " + i);
            for (int j = i + 1; j < sources.length; j++) {
                check(retained[i] != retained[j],
                        "equal-but-distinct sources share an owner: " + i + "," + j);
            }
        }
        System.out.println("ObjectAdapterConcurrentIdentityTest passed: "
                + completed + " hot iterations, " + (WORKERS * COLD_RECEIVERS)
                + " cold adapter results, " + WORKERS + " workers");
    }

    /**
     * No capability for these receivers is constructed before all workers
     * enter. The per-source barriers cause Java and JNI callers to race on
     * each owner. Results are retained by the caller until after comparison.
     */
    private static void coldCreationRace() throws Exception {
        Object[] sources = new Object[COLD_RECEIVERS];
        for (int i = 0; i < COLD_RECEIVERS; i++) {
            sources[i] = new EqualButDistinct(1000 + i);
        }
        M3Object[][] received = new M3Object[WORKERS][COLD_RECEIVERS];
        CyclicBarrier phase = new CyclicBarrier(WORKERS);
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < WORKERS; worker++) {
                final int workerIndex = worker;
                futures.add(executor.submit(() -> {
                    for (int slot = 0; slot < COLD_RECEIVERS; slot++) {
                        phase.await(30, TimeUnit.SECONDS);
                        Object source = sources[slot];
                        M3Object owner = (workerIndex & 1) == 0
                                ? source.toAdapter(M3Object.class)
                                : M3Object.class.cast(adaptNative(source, M3Object.class, (workerIndex / 2) % 3));
                        received[workerIndex][slot] = owner;
                        phase.await(30, TimeUnit.SECONDS);
                    }
                    return COLD_RECEIVERS;
                }));
            }
            int completed = 0;
            for (Future<Integer> future : futures) {
                completed += future.get(120, TimeUnit.SECONDS);
            }
            check(completed == WORKERS * COLD_RECEIVERS,
                    "cold first-creation workload incomplete");
        } finally {
            executor.shutdownNow();
            check(executor.awaitTermination(30, TimeUnit.SECONDS),
                    "cold creation workers did not terminate");
        }
        for (int slot = 0; slot < COLD_RECEIVERS; slot++) {
            M3Object canonical = received[0][slot];
            check(canonical != null && canonical.source() == sources[slot],
                    "cold first capability missing or wrong source: " + slot);
            for (int worker = 1; worker < WORKERS; worker++) {
                check(received[worker][slot] == canonical,
                        "split cold capability at slot " + slot + ", worker " + worker);
            }
            check(M3Object.of(sources[slot]) == canonical,
                    "cold owner not registered at slot " + slot);
            if (slot > 0) {
                check(received[0][slot - 1] != canonical,
                        "distinct cold sources share capability at " + slot);
            }
        }
    }

    /** Validate all three virtual JNI forms and their Java exception contracts. */
    private static void nativeInvocationForms() {
        Object source = new Object();
        M3Object retained = source.toAdapter(M3Object.class);
        for (int mode = 0; mode < 3; mode++) {
            check(adaptNative(source, Object.class, mode) == source,
                    "native mode identity " + mode);
            check(adaptNative(source, M3Object.class, mode) == retained,
                    "native mode capability identity " + mode);
            check(adaptNative(source, Runnable.class, mode) == null,
                    "native mode unsupported request " + mode);
            try {
                adaptNative(source, null, mode);
                throw new AssertionError("native mode accepted null class " + mode);
            } catch (NullPointerException expected) {
                // The original Java exception is retained across the JNI call.
            }
        }
        try {
            adaptNative(source, Object.class, 3);
            throw new AssertionError("native fixture accepted unknown mode");
        } catch (IllegalArgumentException expected) {
            // The native fixture must fail closed on an unknown selector.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
