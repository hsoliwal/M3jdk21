// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.os;

/** Optional Linux JNI support. No capability probe is made before the first snapshot request. */
final class NativeProbe {
    static final boolean AVAILABLE = load();
    private NativeProbe() { }

    private static boolean load() {
        try {
            if (Boolean.getBoolean("m3.os.disableNative")) return false;
            String os = System.getProperty("os.name", "");
            String vm = System.getProperty("java.vm.name", "");
            String runtime = System.getProperty("java.runtime.name", "");
            if (OSCapabilities.platformFor(os, vm, runtime) != OSCapabilities.Platform.LINUX)
                return false;
            System.loadLibrary("m3osprobe");
            return abiVersion0() == 1;
        } catch (UnsatisfiedLinkError | SecurityException failure) {
            return false;
        }
    }

    static int query(int id) { return AVAILABLE ? probe0(id) : -1; }
    static int pageSize() { return AVAILABLE ? pageSize0() : 0; }
    private static native int abiVersion0();
    private static native int pageSize0();
    private static native int probe0(int featureId);
}
