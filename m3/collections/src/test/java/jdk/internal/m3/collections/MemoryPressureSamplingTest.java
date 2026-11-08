// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Actual-owner oracle; fixture modes require the separately built test-only JNI ABI. */
//FLLDCIM: RELOCATE - donor blob d45586be99b1 at 165ceb5a0202; recipe m3/collections/recipe/residency-paged-array-20261107/relocate.py v4
public final class MemoryPressureSamplingTest {
    private static int checks;
    private MemoryPressureSamplingTest() { }

    @FunctionalInterface
    private interface Attempt { void run() throws Exception; }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void reject(Class<? extends Throwable> type, String message,
                               Attempt action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            check(failure.getClass() == type, "exact exception type: " + failure);
            check(message.equals(failure.getMessage()), "exact exception message: " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void heap(MemoryPressure.Snapshot snapshot) {
        check(snapshot.maxHeap() == Runtime.getRuntime().maxMemory(), "current JVM maximum");
        check(snapshot.usedHeap() >= 0 && snapshot.committedHeap() >= snapshot.usedHeap(),
                "valid sampled heap");
        check(snapshot.headroom() == Math.max(0, snapshot.maxHeap() - snapshot.usedHeap()),
                "same headroom arithmetic");
    }

    private static void physical(MemoryPressure.Snapshot snapshot, long available, long total) {
        heap(snapshot);
        check(snapshot.physicalAvailable() == available, "physical availability " + available);
        check(snapshot.physicalTotal() == total, "physical total " + total);
    }

    private static long[] values(MemoryPressure.Snapshot snapshot) {
        return new long[] {snapshot.maxHeap(), snapshot.committedHeap(), snapshot.usedHeap(),
                snapshot.physicalAvailable(), snapshot.physicalTotal()};
    }

    private static boolean intoSupported() throws ReflectiveOperationException {
        Field field = MemoryPressure.class.getDeclaredField("INTO_SUPPORTED");
        field.setAccessible(true);
        return field.getBoolean(null);
    }

    private static Object outputArray() throws ReflectiveOperationException {
        Field field = MemoryPressure.class.getDeclaredField("PHYSICAL_OUTPUT");
        field.setAccessible(true);
        return ((ThreadLocal<?>) field.get(null)).get();
    }

    static void snapshotContract() throws Exception {
        for (long[] invalid : new long[][] {{0, 0, 0}, {-1, 2, 1}, {10, 1, 2}, {10, 1, -1}}) {
            reject(IllegalArgumentException.class, "invalid JVM heap snapshot",
                    () -> new MemoryPressure.Snapshot(invalid[0], invalid[1], invalid[2], -1, -1));
        }
        MemoryPressure.Snapshot zero = new MemoryPressure.Snapshot(10, 10, 10, 0, 0);
        check(zero.headroom() == 0 && zero.physicalLow(1), "finite zero is exhaustion");
        check(!zero.physicalLow(0), "physical threshold stays strict");
        MemoryPressure.Snapshot unknown = new MemoryPressure.Snapshot(10, 12, 12, -1, -1);
        check(unknown.headroom() == 0 && !unknown.physicalLow(Long.MAX_VALUE),
                "unknown physical evidence and saturated headroom");
        MemoryPressure.Probe java = MemoryPressure.runtime();
        MemoryPressure.Snapshot first = java.sample();
        long[] saved = values(first);
        for (int i = 0; i < 16; i++) {
            MemoryPressure.Snapshot next = java.sample();
            physical(next, -1, -1);
            check(next != first, "no published snapshot recycled");
        }
        check(Arrays.equals(values(first), saved), "retained Java snapshot stable");
    }

    private static void samplePublicPaths(long available, long total) {
        MemoryPressure.Probe held = MemoryPressure.runtimeWithOptionalNative();
        physical(MemoryPressure.current(), available, total);
        physical(held.sample(), available, total);
        physical(MemoryPressure.runtimeWithOptionalNative().sample(), available, total);
        physical(MemoryPressure.runtime().sample(), -1, -1);
    }

    private static void missingLibrary(boolean libraryLoaded) {
        check(MemoryPressure.nativeAvailable() == libraryLoaded, "expected library availability");
        for (int i = 0; i < 8; i++) samplePublicPaths(-1, -1);
    }

    private static void controlledSamples(boolean legacy) throws Exception {
        check(MemoryPressure.nativeAvailable(), "controlled library loaded");
        int[] modes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int mode : modes) {
            configure0(mode);
            long available = mode == 0 ? 64 : mode == 1 ? 0 : mode == 7 ? Long.MAX_VALUE : -1;
            long total = mode == 0 ? 256 : mode == 1 ? 0 : mode == 7 ? Long.MAX_VALUE : -1;
            samplePublicPaths(available, total);
            check(calls0() == (legacy ? 3L << 32 : 3L), "one JNI sample per optional call");
            check(intoSupported() != legacy, "expected ABI dispatch");
        }
        for (int mode : new int[] {10, 11}) {
            configure0(mode);
            Class<? extends Throwable> type = mode == 10 ? IllegalStateException.class : OutOfMemoryError.class;
            String message = mode == 10 ? "fixture state" : "fixture allocation";
            reject(type, message, MemoryPressure::current);
            reject(type, message, () -> MemoryPressure.runtimeWithOptionalNative().sample());
            check(calls0() == (legacy ? 2L << 32 : 2L), "uncaught failure samples once");
        }
        if (legacy) {
            for (int mode : new int[] {13, 14, 15, 16}) {
                configure0(mode);
                samplePublicPaths(-1, -1);
                check(calls0() == 3L << 32, "malformed legacy array samples once");
            }
        }
    }

    private static void controlledLifetime(boolean legacy) throws Exception {
        configure0(0);
        MemoryPressure.Snapshot retained = MemoryPressure.current();
        long[] saved = values(retained);
        Object firstArray = legacy ? null : outputArray();
        for (int i = 0; i < 24; i++) {
            configure0(i % 2 == 0 ? 1 : 6);
            physical(MemoryPressure.current(), i % 2 == 0 ? 0 : -1, i % 2 == 0 ? 0 : -1);
            check(Arrays.equals(values(retained), saved), "native output cannot mutate retained Snapshot");
            if (!legacy) check(outputArray() == firstArray, "reuse existing per-thread output");
        }
        if (!legacy) threadIsolation(true);
    }

    private static void controlledFallback() throws Exception {
        configure0(12);
        samplePublicPaths(64, 256);
        check(calls0() == (3L << 32) + 1, "missing into symbol falls back once");
        check(!intoSupported(), "ABI fallback is permanent");
        configure0(0);
        samplePublicPaths(64, 256);
        check(calls0() == 3L << 32, "old ABI stays selected");
    }

    private static void invokeInto(Object destination) throws Exception {
        Method method = MemoryPressure.class.getDeclaredMethod("physicalMemoryInto0", long[].class);
        method.setAccessible(true);
        try { method.invoke(null, destination); }
        catch (InvocationTargetException wrapped) {
            if (wrapped.getCause() instanceof Exception exception) throw exception;
            if (wrapped.getCause() instanceof Error error) throw error;
            throw new AssertionError(wrapped.getCause());
        }
    }

    private static void realLifetime() throws Exception {
        check(MemoryPressure.nativeAvailable(), "actual native sampler required");
        MemoryPressure.Snapshot retained = MemoryPressure.current();
        long[] saved = values(retained);
        Object array = outputArray();
        for (int i = 0; i < 24; i++) {
            MemoryPressure.Snapshot current = i % 2 == 0 ? MemoryPressure.current()
                    : MemoryPressure.runtimeWithOptionalNative().sample();
            heap(current);
            check(current.physicalAvailable() >= 0 && current.physicalTotal() >= current.physicalAvailable(),
                    "usable actual native sample");
            check(current != retained && Arrays.equals(values(retained), saved), "retained native sample stable");
            check(outputArray() == array, "actual JNI reuses existing output");
        }
        threadIsolation(false);
    }

    private static void realBoundary() throws Exception {
        reject(NullPointerException.class, "physical output", () -> invokeInto(null));
        for (int length : new int[] {0, 1, 3}) {
            reject(IllegalArgumentException.class, "physical output requires exactly two long values",
                    () -> invokeInto(new long[length]));
        }
    }

    private static void threadIsolation(boolean fixture) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Object[] arrays = new Object[2];
        Throwable[] failures = new Throwable[2];
        Thread[] workers = new Thread[2];
        for (int i = 0; i < workers.length; i++) {
            final int slot = i;
            workers[i] = new Thread(() -> {
                try {
                    if (fixture) configure0(slot);
                    MemoryPressure.Snapshot retained = MemoryPressure.current();
                    long[] saved = values(retained);
                    arrays[slot] = outputArray();
                    ready.countDown();
                    if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("release timeout");
                    for (int n = 0; n < 16; n++) {
                        MemoryPressure.current();
                        if (arrays[slot] != outputArray() || !Arrays.equals(saved, values(retained)))
                            throw new AssertionError("thread-local lifetime changed");
                    }
                } catch (Throwable failure) { failures[slot] = failure; ready.countDown(); }
            }, "pressure-sample-" + slot);
            workers[i].setDaemon(true);
            workers[i].start();
        }
        try { check(ready.await(10, TimeUnit.SECONDS), "sampling workers ready"); }
        finally { release.countDown(); }
        for (int i = 0; i < workers.length; i++) {
            workers[i].join(TimeUnit.SECONDS.toMillis(10));
            check(!workers[i].isAlive(), "sampling worker terminated");
            if (failures[i] != null) throw new AssertionError("sampling worker failure", failures[i]);
        }
        check(arrays[0] != null && arrays[1] != null && arrays[0] != arrays[1], "no shared mutable output array");
    }

    static void samplesAndFallback(String mode) throws Exception {
        switch (mode) {
            case "java" -> missingLibrary(false);
            case "missing" -> missingLibrary(true);
            case "fixture" -> controlledSamples(false);
            case "legacy" -> controlledSamples(true);
            case "native" -> {
                check(MemoryPressure.nativeAvailable(), "actual native sampler required");
                MemoryPressure.Probe held = MemoryPressure.runtimeWithOptionalNative();
                for (int path = 0; path < 3; path++) {
                    MemoryPressure.Snapshot snapshot = path == 0 ? MemoryPressure.current()
                            : path == 1 ? held.sample() : MemoryPressure.runtimeWithOptionalNative().sample();
                    heap(snapshot);
                    check(snapshot.physicalAvailable() >= 0
                            && snapshot.physicalTotal() >= snapshot.physicalAvailable(), "usable real JNI sample");
                }
                physical(MemoryPressure.runtime().sample(), -1, -1);
            }
            default -> throw new IllegalArgumentException("unknown mode");
        }
    }

    static void retainedSnapshotsAndThreadOutput(String mode) throws Exception {
        switch (mode) {
            case "fixture" -> controlledLifetime(false);
            case "legacy" -> controlledLifetime(true);
            case "native" -> realLifetime();
            case "java", "missing" -> {
                MemoryPressure.Snapshot retained = MemoryPressure.current();
                long[] saved = values(retained);
                for (int i = 0; i < 24; i++) {
                    MemoryPressure.Snapshot next = MemoryPressure.runtimeWithOptionalNative().sample();
                    physical(next, -1, -1);
                    check(next != retained && Arrays.equals(values(retained), saved), "fallback snapshot stable");
                }
            }
            default -> throw new IllegalArgumentException("unknown mode");
        }
    }

    static void nativeAbiBoundary(String mode) throws Exception {
        switch (mode) {
            case "fixture" -> controlledFallback();
            case "native" -> realBoundary();
            case "java" -> check(intoSupported(), "unavailable library never probes into ABI");
            case "legacy", "missing" -> check(!intoSupported(), "fallback remains permanent");
            default -> throw new IllegalArgumentException("unknown mode");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("java|native|fixture|legacy|missing");
        snapshotContract();
        samplesAndFallback(args[0]);
        retainedSnapshotsAndThreadOutput(args[0]);
        nativeAbiBoundary(args[0]);
        System.out.println("PASS M3MemoryPressureSamplingTest mode=" + args[0] + " checks=" + checks);
    }

    private static native void configure0(int mode);
    private static native long calls0();
}
