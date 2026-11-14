/* *****************************************************************************
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
package jdk.m3.collections.bits;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Chunked compressed 64-bit integer set.
 *
 * <p>The high 32 bits select a sorted directory entry; each directory entry
 * owns one {@link RoaringIntSet} for the low 32 bits. Object overhead
 * is therefore per populated 2^32-value chunk, never per retained long.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveRoaringLongSet.java}
 * blob {@code 0bf00378ef98df9b36954a97f25c3aaa5a844593} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class RoaringLongSet {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public RoaringLongSet() {
    }

    private int[] highKeys = new int[4];
    private Object[] containers = new Object[4];
    private int containerCount;
    private long cardinality;

    public int containerCount() { return containerCount; }
    public long cardinality() { return cardinality; }
    public boolean isEmpty() { return cardinality == 0L; }
    public int containerReferenceSlots() { return containers.length; }

    public boolean contains(long value) {
        int high = (int) (value >>> 32);
        int index = findHigh(high);
        return index >= 0 && container(index).contains((int) value);
    }

    public boolean add(long value) {
        int high = (int) (value >>> 32);
        int index = findHigh(high);
        if (index < 0) {
            index = -index - 1;
            insertContainer(index, high);
        }
        boolean added = container(index).add((int) value);
        if (added) cardinality++;
        return added;
    }

    public boolean remove(long value) {
        int high = (int) (value >>> 32);
        int index = findHigh(high);
        if (index < 0) return false;
        RoaringIntSet low = container(index);
        if (!low.remove((int) value)) return false;
        cardinality--;
        if (low.isEmpty()) removeContainer(index);
        return true;
    }

    public void clear() {
        Arrays.fill(highKeys, 0, containerCount, 0);
        Arrays.fill(containers, 0, containerCount, null);
        containerCount = 0;
        cardinality = 0L;
    }

    public void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < containerCount; i++) {
            long high = Integer.toUnsignedLong(highKeys[i]) << 32;
            container(i).forEach(low ->
                    consumer.accept(high | Integer.toUnsignedLong(low)));
        }
    }

    public RoaringLongSet union(RoaringLongSet other) {
        Objects.requireNonNull(other, "other");
        RoaringLongSet result = copy();
        other.forEach(result::add);
        return result;
    }

    public RoaringLongSet intersection(RoaringLongSet other) {
        Objects.requireNonNull(other, "other");
        RoaringLongSet result = new RoaringLongSet();
        RoaringLongSet smaller = cardinality <= other.cardinality ? this : other;
        RoaringLongSet larger = smaller == this ? other : this;
        smaller.forEach(value -> {
            if (larger.contains(value)) result.add(value);
        });
        return result;
    }

    public RoaringLongSet copy() {
        RoaringLongSet result = new RoaringLongSet();
        result.ensureCapacity(containerCount);
        result.containerCount = containerCount;
        result.cardinality = cardinality;
        System.arraycopy(highKeys, 0, result.highKeys, 0, containerCount);
        for (int i = 0; i < containerCount; i++) {
            result.containers[i] = container(i).copy();
        }
        return result;
    }

    private int findHigh(int high) {
        int low = 0;
        int upper = containerCount - 1;
        while (low <= upper) {
            int mid = (low + upper) >>> 1;
            int comparison = Integer.compareUnsigned(highKeys[mid], high);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) upper = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    private void insertContainer(int index, int high) {
        ensureCapacity(containerCount + 1);
        int moved = containerCount - index;
        if (moved > 0) {
            System.arraycopy(highKeys, index, highKeys, index + 1, moved);
            System.arraycopy(containers, index, containers, index + 1, moved);
        }
        highKeys[index] = high;
        containers[index] = new RoaringIntSet();
        containerCount++;
    }

    private void removeContainer(int index) {
        int moved = containerCount - index - 1;
        if (moved > 0) {
            System.arraycopy(highKeys, index + 1, highKeys, index, moved);
            System.arraycopy(containers, index + 1, containers, index, moved);
        }
        int last = --containerCount;
        highKeys[last] = 0;
        containers[last] = null;
    }

    private void ensureCapacity(int required) {
        if (required <= highKeys.length) return;
        int next = highKeys.length + (highKeys.length >>> 1) + 1;
        if (next < required) next = required;
        highKeys = Arrays.copyOf(highKeys, next);
        containers = Arrays.copyOf(containers, next);
    }

    private RoaringIntSet container(int index) {
        return (RoaringIntSet) containers[index];
    }
}
