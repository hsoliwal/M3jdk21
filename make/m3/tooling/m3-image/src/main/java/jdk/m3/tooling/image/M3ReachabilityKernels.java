/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.nio.file.Path;

/** Selects the portable or explicitly configured JNI reachability kernel. */
public final class M3ReachabilityKernels {
    /** System property containing the exact JNI library path. */
    public static final String NATIVE_LIBRARY_PROPERTY = "m3.image.reachability.native.library";

    private M3ReachabilityKernels() {
    }

    /** Returns the portable deterministic Java kernel. */
    public static M3ReachabilityKernel javaKernel() {
        return M3JavaReachabilityKernel.INSTANCE;
    }

    /** Loads and returns the JNI kernel from an exact path. */
    public static M3ReachabilityKernel nativeKernel(Path library) {
        return new M3JniReachabilityKernel(library);
    }

    /**
     * Returns the JNI kernel only when its exact path property is explicitly configured; otherwise
     * returns the Java kernel. Invalid explicit configuration fails closed.
     */
    public static M3ReachabilityKernel selected() {
        String configured = System.getProperty(NATIVE_LIBRARY_PROPERTY, "").trim();
        return configured.isEmpty() ? javaKernel() : nativeKernel(Path.of(configured));
    }
}
