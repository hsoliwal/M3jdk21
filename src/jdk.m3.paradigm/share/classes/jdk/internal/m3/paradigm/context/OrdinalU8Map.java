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

/**
 * Sparse immutable schema-bound unsigned-byte map with no missing-value sentinel.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalU8Map.java}
 * blob {@code 89c05e20a684a3a41d0574288fe8bc866008e409} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalU8Map {
    private static final byte[] EMPTY = new byte[0];

    private final OrdinalSchema schema;
    private final long present;
    private final byte[] values;

    private OrdinalU8Map(OrdinalSchema schema, long present, byte[] values) {
        this.schema = schema;
        this.present = present;
        this.values = values;
    }

    public static OrdinalU8Map empty(OrdinalSchema schema) {
        return new OrdinalU8Map(OrdinalSchema.requireHot(schema), 0L, EMPTY);
    }

    public OrdinalSchema schema() { return schema; }
    public int size() { return values.length; }
    public long presentBits() { return present; }

    public int getOrDefault(String key, int fallback) {
        int ordinal = schema.ordinal(key);
        return ordinal < 0 ? fallback : getOrDefault(ordinal, fallback);
    }

    public int getOrDefault(int ordinal, int fallback) {
        Objects.checkIndex(ordinal, schema.size());
        int dense = OrdinalSchema.denseIndex(present, ordinal);
        return dense < 0 ? fallback : Byte.toUnsignedInt(values[dense]);
    }

    public OrdinalU8Map with(String key, int value) {
        return with(schema.requireOrdinal(key), value);
    }

    public OrdinalU8Map with(int ordinal, int value) {
        Objects.checkIndex(ordinal, schema.size());
        requireU8(value);
        long bit = 1L << ordinal;
        int dense = OrdinalSchema.insertionIndex(present, ordinal);
        byte encoded = (byte) value;
        if ((present & bit) != 0L) {
            if (values[dense] == encoded) return this;
            byte[] copy = values.clone();
            copy[dense] = encoded;
            return new OrdinalU8Map(schema, present, copy);
        }
        byte[] copy = new byte[values.length + 1];
        System.arraycopy(values, 0, copy, 0, dense);
        copy[dense] = encoded;
        System.arraycopy(values, dense, copy, dense + 1, values.length - dense);
        return new OrdinalU8Map(schema, present | bit, copy);
    }

    public OrdinalU8Map without(String key) {
        int ordinal = schema.ordinal(key);
        return ordinal < 0 ? this : without(ordinal);
    }

    public OrdinalU8Map without(int ordinal) {
        Objects.checkIndex(ordinal, schema.size());
        long bit = 1L << ordinal;
        if ((present & bit) == 0L) return this;
        int dense = OrdinalSchema.insertionIndex(present, ordinal);
        if (values.length == 1) return empty(schema);
        byte[] copy = new byte[values.length - 1];
        System.arraycopy(values, 0, copy, 0, dense);
        System.arraycopy(values, dense + 1, copy, dense, values.length - dense - 1);
        return new OrdinalU8Map(schema, present & ~bit, copy);
    }

    public double unit(String key, double fallback) {
        int value = getOrDefault(key, -1);
        return value < 0 ? fallback : value / 255.0d;
    }

    public byte[] valuesCopy() { return values.clone(); }

    private static void requireU8(int value) {
        if ((value & ~0xff) != 0) throw new IllegalArgumentException("value must be in [0,255]");
    }
}
