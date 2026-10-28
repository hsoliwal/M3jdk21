/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/** JNI parity kernel. A parent owner opts in only after loading the native library. */
final class M3MaskViewportNative {
    private M3MaskViewportNative() {
    }


    /** Optional JNI parity: only primitive batches, no Java callbacks. */
    static native int projectFramePage(int[] logicalOffsets, int[] states,
            int first, int length, int requiredBits, int forbiddenBits,
            int skipVisible, int maxVisible, int[] rowTarget, int rowTargetOffset,
            int[] slotTarget, int slotTargetOffset, int[] stateTarget, int stateTargetOffset);

    static native int project(int[] logicalOffsets, int[] states, int first,
            int length, int requiredBits, int forbiddenBits, int[] target,
            int targetOffset);
}
