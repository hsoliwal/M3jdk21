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
 * Sparse immutable schema-bound primitive long map.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalLongMap.java}
 * blob {@code 43bf6063916a92979e535d180713c09f773f8b47} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalLongMap {
    private static final long[] EMPTY = new long[0];

    private final OrdinalSchema schema;
    private final long present;
    private final long[] values;

    private OrdinalLongMap(OrdinalSchema schema, long present, long[] values) {
        this.schema = schema; this.present = present; this.values = values;
    }

    public static OrdinalLongMap empty(OrdinalSchema schema) {
        return new OrdinalLongMap(OrdinalSchema.requireHot(schema), 0L, EMPTY);
    }

    public OrdinalSchema schema() { return schema; }
    public int size() { return values.length; }
    public long presentBits() { return present; }

    public long getOrDefault(String key, long fallback) {
        int ordinal = schema.ordinal(key);
        return ordinal < 0 ? fallback : getOrDefault(ordinal, fallback);
    }

    public long getOrDefault(int ordinal, long fallback) {
        Objects.checkIndex(ordinal, schema.size());
        int dense = OrdinalSchema.denseIndex(present, ordinal);
        return dense < 0 ? fallback : values[dense];
    }

    public OrdinalLongMap with(String key, long value) {
        return with(schema.requireOrdinal(key), value);
    }

    public OrdinalLongMap with(int ordinal, long value) {
        Objects.checkIndex(ordinal, schema.size());
        long bit = 1L << ordinal;
        int dense = OrdinalSchema.insertionIndex(present, ordinal);
        if ((present & bit) != 0L) {
            if (values[dense] == value) return this;
            long[] copy = values.clone(); copy[dense] = value;
            return new OrdinalLongMap(schema, present, copy);
        }
        long[] copy = new long[values.length + 1];
        System.arraycopy(values, 0, copy, 0, dense);
        copy[dense] = value;
        System.arraycopy(values, dense, copy, dense + 1, values.length - dense);
        return new OrdinalLongMap(schema, present | bit, copy);
    }

    public OrdinalLongMap without(int ordinal) {
        Objects.checkIndex(ordinal, schema.size());
        long bit = 1L << ordinal;
        if ((present & bit) == 0L) return this;
        int dense = OrdinalSchema.insertionIndex(present, ordinal);
        if (values.length == 1) return empty(schema);
        long[] copy = new long[values.length - 1];
        System.arraycopy(values, 0, copy, 0, dense);
        System.arraycopy(values, dense + 1, copy, dense, values.length - dense - 1);
        return new OrdinalLongMap(schema, present & ~bit, copy);
    }

    public long[] valuesCopy() { return values.clone(); }
}
