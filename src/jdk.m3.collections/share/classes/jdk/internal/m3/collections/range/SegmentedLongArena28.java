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
package jdk.internal.m3.collections.range;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Append-only primitive-long arena with stable int offsets and no whole-arena copy on growth.
 *
 * <p>Storage grows by lazy 4,096-value blocks inside the 8/8/12 segmented address space. This is
 * useful for token arenas, CSR payloads, byte-offset companions and other append-heavy packed data.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/SegmentedLongArena28.java}
 * blob {@code e86e2a1399b7ad6f5b3e230e6155277b4e2f0259} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SegmentedLongArena28 {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public SegmentedLongArena28() {
    }

    private final SegmentedLongLane28 values = new SegmentedLongLane28();
    private int size;

    public int append(long value) {
        if (size == SegmentedAddress28.MAX_SLOTS) {
            throw new IllegalStateException("segmented arena exhausted");
        }
        int index = size++;
        values.set(index, value);
        return index;
    }

    public int appendAll(long[] source) {
        Objects.requireNonNull(source, "source");
        int start = size;
        if ((long) size + source.length > SegmentedAddress28.MAX_SLOTS) {
            throw new IllegalStateException("segmented arena exhausted");
        }
        for (long value : source) {
            values.set(size++, value);
        }
        return start;
    }

    public long get(int index) {
        Objects.checkIndex(index, size);
        return values.get(index);
    }

    public void set(int index, long value) {
        Objects.checkIndex(index, size);
        values.set(index, value);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int allocatedBlockCount() {
        return values.allocatedBlockCount();
    }

    public long payloadBytes() {
        return values.payloadBytes();
    }

    public long[] copyRange(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        long[] result = new long[toExclusive - fromInclusive];
        values.copyTo(fromInclusive, result, 0, result.length);
        return result;
    }

    public void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            consumer.accept(values.get(i));
        }
    }
}
