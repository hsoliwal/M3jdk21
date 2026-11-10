/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;
import java.util.Optional;

/**
 * Up to 64 named references with ordinal addressing and no Map.Entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextRefSlots.java}
 * blob {@code cc485602585f957c546794686a3f5fa0360385b7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class RefSlots<V> {
    private final OrdinalSchema schema;
    private final Object[] values;
    private long presence;

    public RefSlots(OrdinalSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        if (schema.size() > Long.SIZE) {
            throw new IllegalArgumentException("ContextRefSlots supports at most 64 slots");
        }
        values = new Object[schema.size()];
    }

    public int size() {
        return Long.bitCount(presence);
    }

    public boolean contains(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
        return (presence & (1L << ordinal)) != 0L;
    }

    @SuppressWarnings("unchecked")
    public Optional<V> get(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
        return contains(ordinal) ? Optional.of((V) values[ordinal]) : Optional.empty();
    }

    public Optional<V> get(String name) {
        int ordinal = schema.ordinal(name);
        return ordinal < 0 ? Optional.empty() : get(ordinal);
    }

    public void set(int ordinal, V value) {
        Objects.checkIndex(ordinal, values.length);
        values[ordinal] = Objects.requireNonNull(value, "value");
        presence |= 1L << ordinal;
    }

    public void set(String name, V value) {
        set(schema.requireOrdinal(name), value);
    }

    public boolean remove(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
        long bit = 1L << ordinal;
        if ((presence & bit) == 0L) {
            return false;
        }
        presence &= ~bit;
        values[ordinal] = null;
        return true;
    }

    public long presenceBits() {
        return presence;
    }

    public OrdinalSchema schema() {
        return schema;
    }
}
