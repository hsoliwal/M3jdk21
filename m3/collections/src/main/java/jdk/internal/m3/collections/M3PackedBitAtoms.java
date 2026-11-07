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
package jdk.internal.m3.collections;

/**
 * Shared zero-object bit-address arithmetic for packed primitive bit lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedBitAtoms.java}
 * blob {@code 9858def13146479794370dd56387d34edfaa95ef} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3PackedBitAtoms {
    private M3PackedBitAtoms() { }

    static int wordIndex(int bitIndex) {
        return bitIndex >>> 6;
    }

    static long bitMask(int bitIndex) {
        return 1L << (bitIndex & 63);
    }

    static int wordsForBits(int bitCount) {
        return (int) ((((long) bitCount) + 63L) >>> 6);
    }

    static int bitCapacity(int wordCount) {
        return wordCount << 6;
    }
}
