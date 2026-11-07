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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Immutable sorted distinct long values; construction and boundary arrays are copies.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedFrozenLongSet.java}
 * blob {@code cfb6ef1c84ae40b151cabee75e852a0833d17a20} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedFrozenLongSet implements M3LongReadOnlySet {
    private final long[] values;
    private M3PackedFrozenLongSet(long[] values) { this.values = values; }
    public static M3PackedFrozenLongSet copyOf(long[] source) {
        long[] copy = Objects.requireNonNull(source, "source").clone(); Arrays.sort(copy);
        int count = copy.length == 0 ? 0 : 1;
        // The unique prefix already occupies its final positions.
        while (count < copy.length && copy[count] != copy[count - 1]) { count++; }
        if (count < copy.length) {
            for (int read = count + 1; read < copy.length; read++) {
                if (copy[read] != copy[count - 1]) { copy[count++] = copy[read]; }
            }
        }
        return new M3PackedFrozenLongSet(count == copy.length ? copy : Arrays.copyOf(copy, count));
    }
    public int size() { return values.length; }
    public boolean contains(long value) { return Arrays.binarySearch(values, value) >= 0; }
    public long get(int ordinal) { return values[Objects.checkIndex(ordinal, values.length)]; }
    public long[] toArray() { return values.clone(); }
    public long arrayPayloadBytes() { return 8L * values.length; }
    public void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); for (long value : values) consumer.accept(value);
    }
}
