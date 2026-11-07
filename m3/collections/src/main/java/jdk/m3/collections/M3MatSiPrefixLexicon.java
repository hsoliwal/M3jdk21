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
 * Frozen official SI decimal-prefix symbols mapped to base-10 exponents.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatSiPrefixLexicon.java}
 * blob {@code 56ad18a5f529a8605099a5387279dcfefaa53d4c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSiPrefixLexicon {
    private static final M3MatStaticLexicon LEXICON = M3MatStaticLexicon.compile(
            "si-prefix",
            List.of(
                    entry("q", -30), entry("r", -27), entry("y", -24), entry("z", -21),
                    entry("a", -18), entry("f", -15), entry("p", -12), entry("n", -9),
                    entry("µ", -6), entry("m", -3), entry("c", -2), entry("d", -1),
                    entry("da", 1), entry("h", 2), entry("k", 3), entry("M", 6),
                    entry("G", 9), entry("T", 12), entry("P", 15), entry("E", 18),
                    entry("Z", 21), entry("Y", 24), entry("R", 27), entry("Q", 30)));

    private M3MatSiPrefixLexicon() { }

    public static int decimalExponent(String symbol) {
        if (symbol.isEmpty()) {
            return 0;
        }
        int id = LEXICON.idOf(symbol);
        if (id < 0) {
            throw new IllegalArgumentException("Unknown SI prefix: " + symbol);
        }
        return Math.toIntExact(LEXICON.semanticId(id));
    }

    public static int idOf(String symbol) {
        return symbol.isEmpty() ? -1 : LEXICON.idOf(symbol);
    }

    public static String symbol(int id) {
        return LEXICON.token(id);
    }

    public static int size() {
        return LEXICON.size();
    }

    private static M3MatStaticLexicon.Entry entry(String symbol, int exponent) {
        return new M3MatStaticLexicon.Entry(symbol, exponent, 0L);
    }
}
