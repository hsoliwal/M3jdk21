// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class M3FormatPlanTest {
    @Test
    void plansAreIntImagesOverFormatCoordinates() {
        M3FormatPlan p = M3FormatPlan.parse("id=%s n=%d%n");
        assertEquals(M3FormatPlan.Kind.SIMPLE, p.kind());
        assertEquals(2, p.conversions());
        assertArrayEquals(new int[] {1, 0, 3, 2, 's', 0, -1, -1, -1, 1, 5, 8, 2, 'd', 0, -1, -1, -1,
                2, 'n', 0, -1, -1, -1}, p.image());
        assertEquals(M3FormatPlan.Kind.GENERAL, M3FormatPlan.parse("%-08.3f %1$tY").kind());
        assertEquals(M3FormatPlan.Kind.NO_CONVERSION, M3FormatPlan.parse("100%% done%n").kind());
        assertEquals(M3FormatPlan.Kind.INVALID, M3FormatPlan.parse("bad %q").kind());
        assertEquals(M3FormatPlan.Kind.INVALID, M3FormatPlan.parse("trailing %").kind());
    }

    @Test
    void tsvEscapingIsReversible() {
        String text = "a\tb\\c\né\uD800";
        assertEquals(text, M3Tsv.unescape(M3Tsv.escape(text)));
    }
}
