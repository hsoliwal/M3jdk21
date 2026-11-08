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
package jdk.internal.m3.collections.shape;

import jdk.internal.m3.collections.range.SegmentedAddress28;


/**
 * Generic reference adaptation of Synexia's 8/8/12 lanes; absent branches read as null.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SegmentedObjectLane28.java}
 * blob {@code e4b40ca1aec5d197d2d548fb5341a60dbec6c9f7} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SegmentedObjectLane28<E> {
    private Object[][][] regions;
    private int blocks;
    E get(int slot) {
        SegmentedAddress28.checkSlot(slot);
        if (regions == null) { return null; }
        Object[][] region = regions[SegmentedAddress28.region(slot)];
        if (region == null) { return null; }
        Object[] block = region[SegmentedAddress28.block(slot)];
        return block == null ? null : ShapeGuard.read(block, SegmentedAddress28.offset(slot));
    }
    void set(int slot, E value) {
        SegmentedAddress28.checkSlot(slot);
        int r = SegmentedAddress28.region(slot);
        int b = SegmentedAddress28.block(slot);
        int offset = SegmentedAddress28.offset(slot);
        if (value == null) {
            if (regions != null && regions[r] != null && regions[r][b] != null) { regions[r][b][offset] = null; }
            return;
        }
        if (regions == null) { regions = new Object[SegmentedAddress28.REGION_COUNT][][]; }
        if (regions[r] == null) { regions[r] = new Object[SegmentedAddress28.BLOCKS_PER_REGION][]; }
        if (regions[r][b] == null) { regions[r][b] = new Object[SegmentedAddress28.VALUES_PER_BLOCK]; blocks++; }
        regions[r][b][offset] = value;
    }
    /** Page-wise memmove; references and nulls keep their identity without entry objects. */
    void copyWithin(int from, int to, int length) {
        SegmentedAddress28.checkRangeSize(from, length);
        SegmentedAddress28.checkRangeSize(to, length);
        boolean backwards = to > from && to - from < length;
        while (length > 0) {
            int read = backwards ? from + length - 1 : from;
            int write = backwards ? to + length - 1 : to;
            int ro = read & SegmentedAddress28.OFFSET_MASK;
            int wo = write & SegmentedAddress28.OFFSET_MASK;
            int count = Math.min(length, backwards ? Math.min(ro, wo) + 1
                    : SegmentedAddress28.VALUES_PER_BLOCK - Math.max(ro, wo));
            if (backwards) { ro -= count - 1; wo -= count - 1; }
            Object[] source = page(read);
            Object[] target = page(write);
            if (target == null && source != null) {
                for (int i = ro; i < ro + count; i++) {
                    if (source[i] != null) {
                        set(write, ShapeGuard.read(source, i)); target = page(write); break;
                    }
                }
            }
            if (target != null) {
                if (source == null) java.util.Arrays.fill(target, wo, wo + count, null);
                else System.arraycopy(source, ro, target, wo, count);
            }
            length -= count;
            if (!backwards) { from += count; to += count; }
        }
    }
    private Object[] page(int slot) {
        if (regions == null) return null;
        Object[][] region = regions[SegmentedAddress28.region(slot)];
        return region == null ? null : region[SegmentedAddress28.block(slot)];
    }
    /** Caller must prove that no live position remains in this entire leaf. */
    void releaseBlockContaining(int slot) {
        SegmentedAddress28.checkSlot(slot);
        int r = SegmentedAddress28.region(slot);
        int b = SegmentedAddress28.block(slot);
        if (regions == null || regions[r] == null || regions[r][b] == null) { return; }
        regions[r][b] = null;
        if (--blocks == 0) { regions = null; return; }
        for (Object[] block : regions[r]) { if (block != null) { return; } }
        regions[r] = null;
    }
    long referenceSlots() { return (long) blocks * SegmentedAddress28.VALUES_PER_BLOCK; }
    int blocks() { return blocks; }
}
