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
 * Shared enum universe plus the narrowest primitive width able to encode ordinal+1.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumSchema.java}
 * blob {@code 2d5ed3bfe393f9ec4813cd0b30f7697c253566c5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumSchema<E extends Enum<E>> {
    public enum Width { BYTE, SHORT, INT }

    private final Class<E> type;
    private final E[] constants;
    private final Width width;

    public EnumSchema(Class<E> type) {
        this.type = Objects.requireNonNull(type, "type");
        E[] values = type.getEnumConstants();
        if (values == null) throw new IllegalArgumentException("not an enum type: " + type.getName());
        constants = values;
        width = widthForConstants(constants.length);
    }

    public Class<E> type() { return type; }
    public int size() { return constants.length; }
    public Width width() { return width; }

    public int code(E value) {
        if (value == null) return 0;
        if (value.getDeclaringClass() != type) throw new IllegalArgumentException("wrong enum type");
        return value.ordinal() + 1;
    }

    public E decode(int code) {
        if (code == 0) return null;
        int ordinal = code - 1;
        if (ordinal < 0 || ordinal >= constants.length) throw new IllegalArgumentException("enum code out of range");
        return constants[ordinal];
    }

    public E constant(int ordinal) {
        return constants[Objects.checkIndex(ordinal, constants.length)];
    }

    public static Width widthForConstants(int constants) {
        if (constants < 0) throw new IllegalArgumentException("negative enum size");
        if (constants <= 255) return Width.BYTE;
        if (constants <= 65_535) return Width.SHORT;
        return Width.INT;
    }
}
