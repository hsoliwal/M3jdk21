/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

/** Test-only bootstrap-owned loader for host JNI qualification, never production source. */
public final class M3RX7NativeBootstrap {
    private M3RX7NativeBootstrap() { }

    public static void load(String absolutePath) {
        if (M3RX7NativeBootstrap.class.getClassLoader() != null) {
            throw new AssertionError("qualification JNI library must belong to the bootstrap loader");
        }
        System.load(absolutePath);
    }
}
