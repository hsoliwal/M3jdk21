// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

/**
 * Packed storage for the IntWidth / signedness combinations without a typed class: 32- and 64-bit, and unsigned
 * widths (PACKED_UNION U9). Obtained from {@link PackedIntArray#of(int, jdk.m3.arrays.IntWidth, boolean)}.
 */
final class FixedWidthIntArray extends PackedIntArray {
    FixedWidthIntArray(int length, int bits, boolean signed) {
        super(length, bits, signed);
    }
}
