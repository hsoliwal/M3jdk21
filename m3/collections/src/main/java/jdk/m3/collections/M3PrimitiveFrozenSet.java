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
package jdk.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Exact-sized immutable sorted primitive set snapshot.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveFrozenSet.java}
 * blob {@code f982315c6a5d4ea008057867eba5755187a14aa4} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveFrozenSet {
    private final PrimitiveKind kind;
    private final Object elements;

    private M3PrimitiveFrozenSet(PrimitiveKind kind, Object elements) {
        this.kind = kind;
        this.elements = elements;
    }

    public static M3PrimitiveFrozenSet copyOf(M3PrimitiveHashSet source) {
        Objects.requireNonNull(source, "source");
        Object values = source.toPrimitiveArray();
        source.kind().sort(values, 0, java.lang.reflect.Array.getLength(values));
        return new M3PrimitiveFrozenSet(source.kind(), values);
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return java.lang.reflect.Array.getLength(elements); }
    public boolean isEmpty() { return size() == 0; }
    public long payloadBytes() { return kind.payloadBytes(size()); }

    public boolean containsBits(long bits) {
        return binarySearch(bits) >= 0;
    }

    public long firstBits() {
        if (isEmpty()) throw new NoSuchElementException();
        return kind.readBits(elements, 0);
    }

    public long lastBits() {
        if (isEmpty()) throw new NoSuchElementException();
        return kind.readBits(elements, size() - 1);
    }

    public long lowerOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultBits : kind.readBits(elements, candidate);
    }

    public long floorOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index : -index - 2;
        return candidate < 0 ? defaultBits : kind.readBits(elements, candidate);
    }

    public long ceilingOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index : -index - 1;
        return candidate >= size() ? defaultBits : kind.readBits(elements, candidate);
    }

    public long higherOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= size() ? defaultBits : kind.readBits(elements, candidate);
    }

    public long getBits(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, index);
    }

    public void forEachBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size(); i++) consumer.accept(kind.readBits(elements, i));
    }

    public Object toPrimitiveArray() { return kind.copyOf(elements, size()); }

    private int binarySearch(long bits) {
        return kind.binarySearch(elements, 0, size(), bits);
    }
}
