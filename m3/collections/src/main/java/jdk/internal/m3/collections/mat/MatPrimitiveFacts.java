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
package jdk.internal.m3.collections.mat;

/**
 * Zero-table canonical primitive identities and properties.
 *
 * <p>Full-width primitive domains are already their own optimal fixed-width identities. This
 * class therefore derives facts from bits instead of allocating wrapper objects or enormous
 * lookup tables.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPrimitiveFacts.java}
 * blob {@code 7d0abed542624448f8fa54396f10c8277ccbe9fe} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatPrimitiveFacts {
    public static final int ZERO = 1 << 0;
    public static final int NEGATIVE = 1 << 1;
    public static final int POSITIVE = 1 << 2;
    public static final int EVEN = 1 << 3;
    public static final int ODD = 1 << 4;
    public static final int POWER_OF_TWO = 1 << 5;
    public static final int MIN_VALUE = 1 << 6;
    public static final int MAX_VALUE = 1 << 7;
    public static final int SIGN_BIT_SET = 1 << 8;
    public static final int FINITE = 1 << 9;
    public static final int INFINITE = 1 << 10;
    public static final int NAN = 1 << 11;
    public static final int SUBNORMAL = 1 << 12;
    public static final int NORMAL = 1 << 13;

    private MatPrimitiveFacts() { }

    public static int canonicalInt(int value) {
        return value;
    }

    public static long canonicalLong(long value) {
        return value;
    }

    public static int canonicalFloatBits(float value) {
        return Float.floatToRawIntBits(value);
    }

    public static long canonicalDoubleBits(double value) {
        return Double.doubleToRawLongBits(value);
    }

    public static int intFacts(int value) {
        int facts = parityFacts(value);
        if (value == 0) {
            facts |= ZERO;
        } else if (value < 0) {
            facts |= NEGATIVE | SIGN_BIT_SET;
        } else {
            facts |= POSITIVE;
        }
        if (value > 0 && (value & (value - 1)) == 0) {
            facts |= POWER_OF_TWO;
        }
        if (value == Integer.MIN_VALUE) {
            facts |= MIN_VALUE;
        }
        if (value == Integer.MAX_VALUE) {
            facts |= MAX_VALUE;
        }
        return facts;
    }

    public static int longFacts(long value) {
        int facts = (value & 1L) == 0L ? EVEN : ODD;
        if (value == 0L) {
            facts |= ZERO;
        } else if (value < 0L) {
            facts |= NEGATIVE | SIGN_BIT_SET;
        } else {
            facts |= POSITIVE;
        }
        if (value > 0L && (value & (value - 1L)) == 0L) {
            facts |= POWER_OF_TWO;
        }
        if (value == Long.MIN_VALUE) {
            facts |= MIN_VALUE;
        }
        if (value == Long.MAX_VALUE) {
            facts |= MAX_VALUE;
        }
        return facts;
    }

    public static int floatFacts(float value) {
        return floatFactsRaw(Float.floatToRawIntBits(value));
    }

    public static int floatFactsRaw(int bits) {
        boolean sign = (bits & 0x8000_0000) != 0;
        int exponent = bits & 0x7f80_0000;
        int fraction = bits & 0x007f_ffff;
        int facts = sign ? SIGN_BIT_SET : 0;
        if (exponent == 0x7f80_0000) {
            if (fraction == 0) {
                facts |= INFINITE | (sign ? NEGATIVE : POSITIVE);
            } else {
                facts |= NAN;
            }
            return facts;
        }
        facts |= FINITE;
        if (exponent == 0) {
            if (fraction == 0) {
                facts |= ZERO;
            } else {
                facts |= SUBNORMAL | (sign ? NEGATIVE : POSITIVE);
            }
        } else {
            facts |= NORMAL | (sign ? NEGATIVE : POSITIVE);
        }
        return facts;
    }

    public static int doubleFacts(double value) {
        return doubleFactsRaw(Double.doubleToRawLongBits(value));
    }

    public static int doubleFactsRaw(long bits) {
        boolean sign = (bits & Long.MIN_VALUE) != 0L;
        long exponent = bits & 0x7ff0_0000_0000_0000L;
        long fraction = bits & 0x000f_ffff_ffff_ffffL;
        int facts = sign ? SIGN_BIT_SET : 0;
        if (exponent == 0x7ff0_0000_0000_0000L) {
            if (fraction == 0L) {
                facts |= INFINITE | (sign ? NEGATIVE : POSITIVE);
            } else {
                facts |= NAN;
            }
            return facts;
        }
        facts |= FINITE;
        if (exponent == 0L) {
            if (fraction == 0L) {
                facts |= ZERO;
            } else {
                facts |= SUBNORMAL | (sign ? NEGATIVE : POSITIVE);
            }
        } else {
            facts |= NORMAL | (sign ? NEGATIVE : POSITIVE);
        }
        return facts;
    }

    private static int parityFacts(int value) {
        return (value & 1) == 0 ? EVEN : ODD;
    }
}
