// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.List;

/** Probe the absent-library fallback in a fresh process, without fake JNI. */
public final class M3NativeAhoMissingSelfTest {
    private M3NativeAhoMissingSelfTest() { }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            if (M3NativeAho.available()) throw new AssertionError("unexpected Aho library");
        }
        M3NativeAho plan = M3NativeAho.compile(List.of("alpha", "omega"));
        try { plan.selectNative(List.of("alpha omega")); }
        catch (UnsatisfiedLinkError expected) {
            System.out.println("M3_AHO_ABSENT_FALLBACK_PASS");
            return;
        }
        throw new AssertionError("missing native must be explicit to this owner");
    }
}
