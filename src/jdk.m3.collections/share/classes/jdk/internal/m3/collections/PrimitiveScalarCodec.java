/* *****************************************************************************
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
package jdk.internal.m3.collections;

/**
 * Canonical scalar-to-word atom shared by primitive collection adapters.
 *
 * <p>Every Java primitive scalar is encoded into one long word. Integral types preserve ordinary
 * Java narrowing/widening semantics. Float/double preserve raw IEEE-754 bits, including NaN
 * payloads and signed zero. This class owns encoding only; collection semantics remain with their
 * existing packed/concurrent owners.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitiveScalarCodec.java}
 * blob {@code 3324dc4796428f84f21f45bf145116f990b71ded} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PrimitiveScalarCodec {
    private PrimitiveScalarCodec() { }

    static long encode(boolean value) { return value ? 1L : 0L; }
    static long encode(byte value) { return value; }
    static long encode(char value) { return value; }
    static long encode(short value) { return value; }
    static long encode(int value) { return value; }
    static long encode(long value) { return value; }
    static long encode(float value) {
        return Integer.toUnsignedLong(Float.floatToRawIntBits(value));
    }
    static long encode(double value) { return Double.doubleToRawLongBits(value); }

    static boolean booleanValue(long word) { return word != 0L; }
    static byte byteValue(long word) { return (byte) word; }
    static char charValue(long word) { return (char) word; }
    static short shortValue(long word) { return (short) word; }
    static int intValue(long word) { return (int) word; }
    static long longValue(long word) { return word; }
    static float floatValue(long word) { return Float.intBitsToFloat((int) word); }
    static double doubleValue(long word) { return Double.longBitsToDouble(word); }
}
