// SPDX-License-Identifier: Apache-2.0
package com.m3.fixture;

/** JNI packaging fixture only; not a production accelerator or performance benchmark. */
public final class NativeProbe {
    private NativeProbe() {}
    private static native int runtimeContract();
    public static void main(String[] args) {
        System.loadLibrary("m3packprobe");
        if (runtimeContract() != 21) throw new AssertionError("JNI payload did not execute");
        System.out.println("PASS: native JMOD payload executed from linked image");
    }
}
