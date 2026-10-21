/* Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 */
package jdk.internal.misc;

import java.util.Objects;

/**
 * M3JDK21 internal, on-demand VM object diagnostic facilities.
 * Shallow bytes come from JVMTI GetObjectSize; identity tags are stable through GC
 * relocation; reference edges are an expensive reachable-graph snapshot, not a
 * continuously maintained heap reference count. No raw moving oop is exported.
 */
public final class M3ObjectMemory {
    private static final int SIZE_CAPABLE = 1;
    private static final int TAG_CAPABLE = 2;
    private static final int FEATURES;

    static {
        System.loadLibrary("java");
        FEATURES = initialize0();
    }

    private M3ObjectMemory() { }

    /** JVM-reported shallow bytes; does not include children, backing or native storage. */
    public static long shallowSize(Object object) {
        Objects.requireNonNull(object, "object");
        require(SIZE_CAPABLE, "JVMTI object sizing unavailable");
        return size0(object);
    }

    /** Opaque, GC-movement-stable tag scoped to this VM's M3 JVMTI environment. */
    public static synchronized long objectId(Object object) {
        Objects.requireNonNull(object, "object");
        require(TAG_CAPABLE, "JVMTI object tagging unavailable");
        return id0(object);
    }

    /**
     * An expensive on-demand count of observed incoming edges from JVMTI root traversal.
     * Heap and root categories are not guaranteed to be exhaustive under every VM/GC
     * implementation. Never treat zero as proof that an object is recyclable.
     */
    public static synchronized ReferenceSnapshot referenceSnapshot(Object object) {
        Objects.requireNonNull(object, "object");
        require(TAG_CAPABLE, "JVMTI reference traversal unavailable");
        long began = System.nanoTime();
        long[] counts = references0(object);
        if (counts == null || counts.length != 3 || counts[0] < 0
                || counts[1] < 0 || counts[2] < 0
                || counts[1] > counts[0] || counts[2] > counts[0]) {
            throw new IllegalStateException("invalid JVMTI reference observation");
        }
        return new ReferenceSnapshot(counts[0], counts[1], counts[2], began,
                System.nanoTime());
    }

    public record ReferenceSnapshot(long observedIncomingEdges, long heapEdges,
            long rootEdges, long startedNanos, long completedNanos) {
        public ReferenceSnapshot {
            if (observedIncomingEdges < 0 || heapEdges < 0 || rootEdges < 0
                    || heapEdges + rootEdges != observedIncomingEdges) {
                throw new IllegalArgumentException("invalid reference snapshot");
            }
        }
    }

    /** Object address is not exposed: an oop can move immediately after a JNI return. */
    public static long addressOf(Object object) {
        Objects.requireNonNull(object, "object");
        throw new UnsupportedOperationException(
                "raw object address requires a safepoint-scoped HotSpot diagnostic; do not use as identity");
    }

    public static boolean sizingAvailable() { return (FEATURES & SIZE_CAPABLE) != 0; }
    public static boolean taggingAvailable() { return (FEATURES & TAG_CAPABLE) != 0; }

    private static void require(int capability, String reason) {
        if ((FEATURES & capability) == 0) throw new UnsupportedOperationException(reason);
    }

    private static native int initialize0();
    private static native long size0(Object object);
    private static native long id0(Object object);
    private static native long[] references0(Object object);
}
