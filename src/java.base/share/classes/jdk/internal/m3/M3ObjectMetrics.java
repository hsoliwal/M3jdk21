/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package jdk.internal.m3;

import java.util.Objects;

/**
 * Experimental direct JNI/JVMTI shallow size query for M3JDK21.
 *
 * <p>Returns the VM-specific storage footprint of one Java object; it does
 * not compute its reachable size, retained size, or incoming reference count.
 * Requires the experimental native library and an available JVMTI environment.
 */
public final class M3ObjectMetrics {
    static {
        // Local patch-module tests cannot install into the bootstrap library path.
        // In a built M3JDK21 the native entrypoint resides in libjava.
        String testLibrary = System.getProperty("jdk.m3.native.path");
        if (testLibrary == null) {
            System.loadLibrary("java");
        } else {
            System.load(testLibrary);
        }
    }

    private M3ObjectMetrics() { }

    public static long shallowSize(Object object) {
        Objects.requireNonNull(object, "object");
        return shallowSize0(object);
    }

    private static native long shallowSize0(Object object);
}
