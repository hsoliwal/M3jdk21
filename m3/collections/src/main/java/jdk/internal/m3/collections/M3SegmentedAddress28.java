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
 * Fixed 28-bit logical address split for lazily allocated segmented primitive storage.
 *
 * <p>Address bits are 8/8/12: region, block, offset. The logical slot space is therefore
 * 2^28 entries while physical blocks are allocated independently.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/SegmentedAddress28.java}
 * blob {@code 4d7629fac2a5fc13e2008570d8b62caacdd800bc} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3SegmentedAddress28 {
    public static final int REGION_BITS = 8;
    public static final int BLOCK_BITS = 8;
    public static final int OFFSET_BITS = 12;

    public static final int REGION_COUNT = 1 << REGION_BITS;
    public static final int BLOCKS_PER_REGION = 1 << BLOCK_BITS;
    public static final int VALUES_PER_BLOCK = 1 << OFFSET_BITS;
    public static final int OCCUPANCY_WORDS_PER_BLOCK = VALUES_PER_BLOCK >>> 6;
    public static final int MAX_SLOTS = 1 << (REGION_BITS + BLOCK_BITS + OFFSET_BITS);
    public static final int MAX_SLOT = MAX_SLOTS - 1;

    public static final int OFFSET_MASK = VALUES_PER_BLOCK - 1;
    public static final int BLOCK_MASK = BLOCKS_PER_REGION - 1;

    private M3SegmentedAddress28() {}

    public static int region(int slot) {
        checkSlot(slot);
        return slot >>> (BLOCK_BITS + OFFSET_BITS);
    }

    public static int block(int slot) {
        checkSlot(slot);
        return (slot >>> OFFSET_BITS) & BLOCK_MASK;
    }

    public static int offset(int slot) {
        checkSlot(slot);
        return slot & OFFSET_MASK;
    }

    public static int compose(int region, int block, int offset) {
        if ((region & ~(REGION_COUNT - 1)) != 0) {
            throw new IndexOutOfBoundsException("region=" + region);
        }
        if ((block & ~BLOCK_MASK) != 0) {
            throw new IndexOutOfBoundsException("block=" + block);
        }
        if ((offset & ~OFFSET_MASK) != 0) {
            throw new IndexOutOfBoundsException("offset=" + offset);
        }
        return (region << (BLOCK_BITS + OFFSET_BITS))
                | (block << OFFSET_BITS)
                | offset;
    }

    /** Validates a half-open range, including an empty range at MAX_SLOTS. */
    public static void checkRange(int from, int to) {
        java.util.Objects.checkFromToIndex(from, to, MAX_SLOTS);
    }

    /** Validates offset/length without an overflowing offset + length addition. */
    public static void checkRangeSize(int offset, int length) {
        java.util.Objects.checkFromIndexSize(offset, length, MAX_SLOTS);
    }

    public static void checkSlot(int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            throw new IndexOutOfBoundsException("slot=" + slot);
        }
    }
}
