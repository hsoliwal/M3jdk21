// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.optane;

import java.nio.ByteBuffer;

final class NativeSupport {
    static final String STATUS = load();
    static final boolean AVAILABLE = STATUS.equals("available; ABI=1");
    private NativeSupport() {}
    private static String load() {
        if (Boolean.getBoolean("m3.optane.disableNative")) return "disabled by operator";
        if (!System.getProperty("os.name", "").equals("Linux")) return "unsupported OS; Java planning remains available";
        try {
            System.loadLibrary("m3optane");
            int abi = abiVersion0();
            return abi == 1 ? "available; ABI=1" : "rejected native ABI=" + abi;
        } catch (UnsatisfiedLinkError | SecurityException e) {
            return "unavailable: " + e.getClass().getSimpleName();
        }
    }
    private static native int abiVersion0();
    static native long advise0(ByteBuffer buffer, int offset, int length, int advice);
}
