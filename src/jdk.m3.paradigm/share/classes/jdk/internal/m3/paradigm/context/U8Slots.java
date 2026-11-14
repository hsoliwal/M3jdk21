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
 * Up to 64 named U8 values: one shared schema, one byte lane, one presence word.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextU8Slots.java}
 * blob {@code f51691fe35483d85bd842ee87c19bd7b32450265} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class U8Slots {
    private final OrdinalSchema schema;
    private final byte[] values;
    private long presence;

    public U8Slots(OrdinalSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        if (schema.size() > Long.SIZE) {
            throw new IllegalArgumentException("ContextU8Slots supports at most 64 slots");
        }
        values = new byte[schema.size()];
    }

    private U8Slots(OrdinalSchema schema, byte[] values, long presence) {
        this.schema = schema;
        this.values = values;
        this.presence = presence;
    }

    public OrdinalSchema schema() {
        return schema;
    }

    public int capacity() {
        return values.length;
    }

    public int size() {
        return Long.bitCount(presence);
    }

    public boolean contains(int ordinal) {
        checkOrdinal(ordinal);
        return (presence & bit(ordinal)) != 0L;
    }

    public boolean contains(String name) {
        int ordinal = schema.ordinal(name);
        return ordinal >= 0 && (presence & bit(ordinal)) != 0L;
    }

    public int get(int ordinal) {
        checkOrdinal(ordinal);
        if ((presence & bit(ordinal)) == 0L) {
            return -1;
        }
        return Byte.toUnsignedInt(values[ordinal]);
    }

    public int get(String name) {
        int ordinal = schema.ordinal(name);
        return ordinal < 0 ? -1 : get(ordinal);
    }

    public int getOrDefault(int ordinal, int fallback) {
        int value = get(ordinal);
        return value < 0 ? fallback : value;
    }

    public void set(int ordinal, int unsigned) {
        checkOrdinal(ordinal);
        requireU8(unsigned);
        values[ordinal] = (byte) unsigned;
        presence |= bit(ordinal);
    }

    public void set(String name, int unsigned) {
        set(schema.requireOrdinal(name), unsigned);
    }

    public boolean remove(int ordinal) {
        checkOrdinal(ordinal);
        long bit = bit(ordinal);
        if ((presence & bit) == 0L) {
            return false;
        }
        presence &= ~bit;
        values[ordinal] = 0;
        return true;
    }

    public void clear() {
        presence = 0L;
        java.util.Arrays.fill(values, (byte) 0);
    }

    public long presenceBits() {
        return presence;
    }

    public byte[] valuesCopy() {
        return values.clone();
    }

    public U8Slots copy() {
        return new U8Slots(schema, values.clone(), presence);
    }

    private void checkOrdinal(int ordinal) {
        Objects.checkIndex(ordinal, values.length);
    }

    private static long bit(int ordinal) {
        return 1L << ordinal;
    }

    private static void requireU8(int value) {
        if ((value & ~0xff) != 0) {
            throw new IllegalArgumentException("U8 must be in [0,255]");
        }
    }
}
