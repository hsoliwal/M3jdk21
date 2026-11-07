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

import java.util.List;

/**
 * Frozen SI base and coherent-derived unit symbols mapped to packed SI dimensions.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatSiUnitLexicon.java}
 * blob {@code 92855d2817e791b9d6d063c65183172ea435c80f} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSiUnitLexicon {
    public static final long BASE_UNIT = 1L;
    public static final long DERIVED_UNIT = 1L << 1;
    public static final long DIMENSIONLESS_DERIVED = 1L << 2;

    private static final M3MatStaticLexicon LEXICON = M3MatStaticLexicon.compile(
            "si-unit",
            List.of(
                    base("s", M3MatSiDimension.TIME),
                    base("m", M3MatSiDimension.LENGTH),
                    base("kg", M3MatSiDimension.MASS),
                    base("A", M3MatSiDimension.CURRENT),
                    base("K", M3MatSiDimension.TEMPERATURE),
                    base("mol", M3MatSiDimension.AMOUNT),
                    base("cd", M3MatSiDimension.LUMINOUS_INTENSITY),
                    derived("Hz", d(-1, 0, 0, 0, 0, 0, 0)),
                    derived("N", d(-2, 1, 1, 0, 0, 0, 0)),
                    derived("Pa", d(-2, -1, 1, 0, 0, 0, 0)),
                    derived("J", d(-2, 2, 1, 0, 0, 0, 0)),
                    derived("W", d(-3, 2, 1, 0, 0, 0, 0)),
                    derived("C", d(1, 0, 0, 1, 0, 0, 0)),
                    derived("V", d(-3, 2, 1, -1, 0, 0, 0)),
                    derived("F", d(4, -2, -1, 2, 0, 0, 0)),
                    derived("Ω", d(-3, 2, 1, -2, 0, 0, 0)),
                    derived("S", d(3, -2, -1, 2, 0, 0, 0)),
                    derived("Wb", d(-2, 2, 1, -1, 0, 0, 0)),
                    derived("T", d(-2, 0, 1, -1, 0, 0, 0)),
                    derived("H", d(-2, 2, 1, -2, 0, 0, 0)),
                    derived("lm", d(0, 0, 0, 0, 0, 0, 1)),
                    derived("lx", d(0, -2, 0, 0, 0, 0, 1)),
                    derived("Bq", d(-1, 0, 0, 0, 0, 0, 0)),
                    derived("Gy", d(-2, 2, 0, 0, 0, 0, 0)),
                    derived("Sv", d(-2, 2, 0, 0, 0, 0, 0)),
                    derived("kat", d(-1, 0, 0, 0, 0, 1, 0)),
                    dimensionless("rad"),
                    dimensionless("sr")));

    private M3MatSiUnitLexicon() { }

    public static int idOf(String symbol) {
        return LEXICON.idOf(symbol);
    }

    public static int requireId(String symbol) {
        return LEXICON.requireId(symbol);
    }

    public static long dimensionOf(String symbol) {
        return dimension(requireId(symbol));
    }

    public static long dimension(int id) {
        return LEXICON.semanticId(id);
    }

    public static long flags(int id) {
        return LEXICON.flags(id);
    }

    public static String symbol(int id) {
        return LEXICON.token(id);
    }

    public static int size() {
        return LEXICON.size();
    }

    public static boolean isBase(int id) {
        return (flags(id) & BASE_UNIT) != 0L;
    }

    private static M3MatStaticLexicon.Entry base(String symbol, long dimension) {
        return new M3MatStaticLexicon.Entry(symbol, dimension, BASE_UNIT);
    }

    private static M3MatStaticLexicon.Entry derived(String symbol, long dimension) {
        return new M3MatStaticLexicon.Entry(symbol, dimension, DERIVED_UNIT);
    }

    private static M3MatStaticLexicon.Entry dimensionless(String symbol) {
        return new M3MatStaticLexicon.Entry(
                symbol,
                M3MatSiDimension.DIMENSIONLESS,
                DERIVED_UNIT | DIMENSIONLESS_DERIVED);
    }

    private static long d(
            int time,
            int length,
            int mass,
            int current,
            int temperature,
            int amount,
            int luminousIntensity) {
        return M3MatSiDimension.pack(
                time, length, mass, current, temperature, amount, luminousIntensity);
    }
}
