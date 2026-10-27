/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

/** JNI parity kernel. A parent owner opts in only after loading the native library. */
final class M3MaskViewportNative {
    private M3MaskViewportNative() {
    }

    static native int project(int[] logicalOffsets, int[] states, int first,
            int length, int requiredBits, int forbiddenBits, int[] target,
            int targetOffset);

    /** Project a bounded frame while preserving input ordinals and complete state words. */
    static native int projectFramePage(int[] logicalOffsets, int[] states, int first,
            int length, int requiredBits, int forbiddenBits, int skipVisible, int maxVisible,
            int[] rows, int rowOffset, int[] slots, int slotOffset,
            int[] words, int wordOffset);

    /** Select a filtered encounter-order page without a second element owner. */
    static native int projectPage(int[] logicalOffsets, int[] states, int first,
            int length, int requiredBits, int forbiddenBits, int skipVisible, int maxVisible,
            int[] target, int targetOffset);
}
