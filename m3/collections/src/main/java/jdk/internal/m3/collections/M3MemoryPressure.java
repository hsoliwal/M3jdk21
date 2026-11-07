// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** JVM heap is authoritative; optional JNI physical-memory samples only tighten pressure. */
//FLLDCIM: RELOCATE - donor blob 14413aa63563 at 165ceb5a0202; recipe m3/collections/recipe/residency-paged-array-20261107/relocate.py v4
public final class M3MemoryPressure {
    private M3MemoryPressure() { }

    public record Snapshot(long maxHeap, long committedHeap, long usedHeap,
                           long physicalAvailable, long physicalTotal) {
        public Snapshot {
            validateHeap(maxHeap, committedHeap, usedHeap);
        }
        private static void validateHeap(long maxHeap, long committedHeap, long usedHeap) {
            if (maxHeap <= 0 || usedHeap < 0 || committedHeap < usedHeap)
                throw new IllegalArgumentException("invalid JVM heap snapshot");
        }
        public long headroom() { return Math.max(0L, maxHeap - usedHeap); }
        public boolean physicalLow(long minimumAvailable) {
            return physicalAvailable >= 0 && physicalAvailable < minimumAvailable;
        }
    }

    @FunctionalInterface
    public interface Probe { Snapshot sample(); }

    public static Probe runtime() {
        return () -> sample(false);
    }

    /** Native sampling is optional; a missing/malformed JNI implementation falls back to JVM evidence. */
    public static Probe runtimeWithOptionalNative() {
        return () -> sample(true);
    }

    private static Snapshot sample(boolean withOptionalNative) {
        Runtime vm = Runtime.getRuntime();
        long committed = vm.totalMemory();
        long used = Math.max(0L, committed - vm.freeMemory());
        long max = vm.maxMemory();
        long available = -1;
        long total = -1;
        if (withOptionalNative && NATIVE_AVAILABLE) {
            // Preserve heap validation before JNI without an intermediate Snapshot.
            Snapshot.validateHeap(max, committed, used);
            try {
                long[] p = physicalMemorySample();
                // A finite zero memory.high/max is exhaustion, not missing evidence.
                if (p != null && p.length == 2 && p[0] >= 0 && p[1] >= 0 && p[0] <= p[1]) {
                    available = p[0];
                    total = p[1];
                }
            } catch (LinkageError | SecurityException e) {
                // Keep the already sampled JVM heap and unknown physical values.
            }
        }
        return new Snapshot(max, committed, used, available, total);
    }

    /** Reuse one tiny Java array per sampling thread, not one native array per sample. */
    private static final ThreadLocal<long[]> PHYSICAL_OUTPUT = ThreadLocal.withInitial(() -> new long[2]);
    /** Old native binaries still support physicalMemory0(); switching off is benign and permanent. */
    private static volatile boolean INTO_SUPPORTED = true;

    private static long[] physicalMemorySample() {
        if (INTO_SUPPORTED) {
            long[] output = PHYSICAL_OUTPUT.get();
            // Refuse stale success if an incompatible native implementation leaves
            // either slot untouched. No stale pressure survives a failed sample.
            output[0] = output[1] = -1L;
            try {
                physicalMemoryInto0(output);
                return output;
            } catch (UnsatisfiedLinkError oldNativeLibrary) {
                INTO_SUPPORTED = false;
            }
        }
        return physicalMemory0();
    }

    /** An on-demand sample of the CURRENT JVM. No caller-supplied -Xmx or budget is required. */
    public static Snapshot current() {
        return sample(true);
    }

    /**
     * One-call, machine-readable runtime diagnostics for array/viewport investigations.
     * Values are bytes, sampled approximately; -1 means physical availability is unknown.
     * This is observation, not a memory reservation, native-allocation accounting or GC guarantee.
     * Call on demand, never on every hot-page add.
     */
    public static String describe() {
        Snapshot s = current();
        return "xmxBytes=" + s.maxHeap()
                + " heapCommittedBytes=" + s.committedHeap()
                + " heapUsedBytes=" + s.usedHeap()
                + " heapHeadroomBytes=" + s.headroom()
                + " physicalAvailableBytes=" + s.physicalAvailable()
                + " physicalTotalBytes=" + s.physicalTotal()
                + " nativeSamplerLoaded=" + nativeAvailable();
    }

    /** True only when the optional JNI library loaded; not proof of usable values. */
    public static boolean nativeAvailable() { return NATIVE_AVAILABLE; }

    private static final boolean NATIVE_AVAILABLE = loadNative();
    private static boolean loadNative() {
        try { System.loadLibrary("m3_residency"); return true; }
        catch (LinkageError | SecurityException e) { return false; }
    }
    private static native long[] physicalMemory0();
    private static native void physicalMemoryInto0(long[] twoLongs);
}
