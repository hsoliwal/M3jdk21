/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/** Optional explicit JNI acceleration over primitive key/row lanes; Java remains default. */
public final class M3SignalOrderNative {
    private static final boolean AVAILABLE = load();
    private M3SignalOrderNative() { }

    public static boolean available() { return AVAILABLE; }

    static void sort(long[] keys, int[] rows) {
        if (!AVAILABLE) throw new UnsatisfiedLinkError("m3signalorder JNI is unavailable");
        sort0(keys, rows);
    }

    private static boolean load() {
        if (!Boolean.getBoolean("m3.collections.signal.native")) return false;
        try {
            System.loadLibrary("m3signalorder");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException failure) {
            return false;
        }
    }

    private static native void sort0(long[] keys, int[] rows);
}
