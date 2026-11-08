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

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Exact-sized immutable primitive list snapshot.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveFrozenList.java}
 * blob {@code 1f2b3972d7d30f55626ceb83c31ccef3fd50de29} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveFrozenList {
    private final PrimitiveKind kind;
    private final Object elements;

    private PrimitiveFrozenList(PrimitiveKind kind, Object elements) {
        this.kind = kind;
        this.elements = elements;
    }

    public static PrimitiveFrozenList copyOf(PrimitiveArrayList source) {
        Objects.requireNonNull(source, "source");
        return new PrimitiveFrozenList(source.kind(), source.toPrimitiveArray());
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return java.lang.reflect.Array.getLength(elements); }
    public boolean isEmpty() { return size() == 0; }
    public long payloadBytes() { return kind.payloadBytes(size()); }

    public long getBits(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, index);
    }

    public int indexOfBits(long bits) {
        for (int i = 0; i < size(); i++) {
            if (kind.equalBits(kind.readBits(elements, i), bits)) return i;
        }
        return -1;
    }

    public void forEachBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size(); i++) consumer.accept(kind.readBits(elements, i));
    }

    public Object toPrimitiveArray() {
        return kind.copyOf(elements, size());
    }
}
