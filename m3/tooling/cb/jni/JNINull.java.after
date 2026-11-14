// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Preserve NewStringUTF null-input behavior with M3 string admission enabled
 * @library /jdk/internal/mindex/cb
 * @run main/othervm/native com.m3.cb.JNINull
 * @run main/othervm/native -Xint -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage com.m3.cb.JNINull
 */
package com.m3.cb;

public final class JNINull {
    private static native String nullUtf();
    public static void main(String[] args) {
        System.loadLibrary("M3JNINull");
        for (int i = 0; i < 1000; i++) {
            if (nullUtf() != null) throw new AssertionError("NewStringUTF(null)");
        }
        System.out.println("PASS JNI null-input calls=1000");
    }
}
