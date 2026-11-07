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

/**
 * Insertion-ordered primitive map without linked entry objects.
 *
 * <p>The hash map owns key/value lanes; a second primitive key lane retains
 * insertion order. Removal shifts only that dense key lane.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveOrderedMap.java}
 * blob {@code ea7e5d0edb8cbce50a8c379528fa1676cb9eff87} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveOrderedMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final M3PrimitiveKind keyKind;
    private final M3PrimitivePrimitiveHashMap values;
    private final M3PrimitiveArrayList order;

    public M3PrimitiveOrderedMap(M3PrimitiveKind keyKind, M3PrimitiveKind valueKind) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        Objects.requireNonNull(valueKind, "valueKind");
        values = new M3PrimitivePrimitiveHashMap(keyKind, valueKind);
        order = new M3PrimitiveArrayList(keyKind);
    }

    public int size() { return order.size(); }
    public boolean isEmpty() { return order.isEmpty(); }
    public long payloadBytes() { return values.payloadBytes() + order.payloadBytes(); }

    public boolean containsKeyBits(long keyBits) { return values.containsKeyBits(keyBits); }

    public long getOrDefaultBits(long keyBits, long defaultBits) {
        return values.getOrDefaultBits(keyBits, defaultBits);
    }

    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        boolean present = values.containsKeyBits(keyBits);
        long old = values.putBits(keyBits, valueBits, absentValueBits);
        if (!present) order.addBits(keyBits);
        return old;
    }

    public long removeOrDefaultBits(long keyBits, long defaultBits) {
        if (!values.containsKeyBits(keyBits)) return defaultBits;
        long old = values.removeOrDefaultBits(keyBits, defaultBits);
        if (!order.removeBits(keyBits)) throw new IllegalStateException("order/map invariant");
        return old;
    }

    public long keyBitsAt(int index) { return order.getBits(index); }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < order.size(); i++) {
            long keyBits = order.getBits(i);
            consumer.accept(keyBits, values.getOrDefaultBits(keyBits, 0L));
        }
    }

    public void clear() {
        values.clear();
        order.clear();
    }
}
