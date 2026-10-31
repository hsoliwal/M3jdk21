/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/** JNI parity kernel. A parent owner opts in only after loading the native library. */
final class M3MaskViewportNative {
    private M3MaskViewportNative() {
    }

    static native int project(int[] logicalOffsets, int[] states, int first,
            int length, int requiredBits, int forbiddenBits, int[] target,
            int targetOffset);

    /** Native resident-page projection; the Java CPU model remains authoritative. */
    static native int projectPage(int[] logicalOffsets, int[] states, int first,
            int length, int requiredBits, int forbiddenBits, int skipVisible,
            int maxVisible, int[] target, int targetOffset);
}
