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
 * Fixed-size unsigned 8-bit strengths. Storage is exactly byte[]; decimals exist only at the boundary.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/QuantizedBytes.java}
 * blob {@code e8d3ce08b1d418e74d3200f884756cd69b90370d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedBytes {
    private final byte[] values;
    public QuantizedBytes(int size) { values = new byte[size]; }
    public int size() { return values.length; }
    public int get(int index) { return values[index] & 0xff; }
    public void set(int index, int strength) {
        Objects.checkIndex(index, values.length);
        if ((strength & ~0xff) != 0) throw new IllegalArgumentException("Strength must be 0..255");
        values[index] = (byte) strength;
    }
    public double normalized(int index) { return get(index) / 255.0; }
    public byte[] copyBytes() { return values.clone(); }
}
