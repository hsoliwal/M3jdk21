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
import java.util.OptionalLong;

/**
 * Up to 64 named long values with one presence word and no boxing in storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextLongSlots.java}
 * blob {@code 1b6d2c3102e6f9995b256c6d0972149204614878} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongSlots {
    private final OrdinalSchema schema;
    private final long[] values;
    private long presence;

    public LongSlots(OrdinalSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        if (schema.size() > Long.SIZE) {
            throw new IllegalArgumentException("ContextLongSlots supports at most 64 slots");
        }
        values = new long[schema.size()];
    }

    public int size() {
        return Long.bitCount(presence);
    }

    public boolean contains(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
        return (presence & (1L << ordinal)) != 0L;
    }

    public OptionalLong get(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
        return contains(ordinal) ? OptionalLong.of(values[ordinal]) : OptionalLong.empty();
    }

    public OptionalLong get(String name) {
        int ordinal = schema.ordinal(name);
        return ordinal < 0 ? OptionalLong.empty() : get(ordinal);
    }

    public long getOrDefault(int ordinal, long fallback) {
        return contains(ordinal) ? values[ordinal] : fallback;
    }

    public void set(int ordinal, long value) {
        Objects.checkIndex(ordinal, values.length);
        values[ordinal] = value;
        presence |= 1L << ordinal;
    }

    public void set(String name, long value) {
        set(schema.requireOrdinal(name), value);
    }

    public boolean remove(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
        long bit = 1L << ordinal;
        if ((presence & bit) == 0L) {
            return false;
        }
        presence &= ~bit;
        values[ordinal] = 0L;
        return true;
    }

    public long presenceBits() {
        return presence;
    }

    public OrdinalSchema schema() {
        return schema;
    }
}
