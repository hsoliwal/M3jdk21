// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A role identifier is exact evidence; substrings in unadmitted comments are not role markers. */
final class M3PatternMarkerTest {
    private static final String ROLE = "M3-IOP: PURE_INT_EXPRESSION";
    private static final String PATH = "spectrum/Marker.java";

    @Test
    void longerOrPrefixedRoleLookalikesCannotSuppressTheCanonicalMarker() {
        for (String decoy : List.of(ROLE + "_NOT_ADMITTED", "not-a-role " + ROLE, ROLE + "Extra")) {
            Map<String, String> original = Map.of(PATH, source(decoy));
            Map<String, String> after = M3IntSpectrumTest.apply(new M3PatternizePureIntAtomRecipe(), original);
            assertTrue(after.get(PATH).contains("/* " + ROLE + " */"), decoy);
            assertTrue(after.get(PATH).contains("/* " + decoy + " */"), "retain original comment");
            MemJava.compile(after);
            assertEquals(after, M3IntSpectrumTest.apply(new M3PatternizePureIntAtomRecipe(), after));
        }
    }

    @Test
    void exactExistingMarkerIsIdempotentWithSurroundingWhitespace() {
        for (String marker : List.of(ROLE, "  " + ROLE + "  ", "\t" + ROLE + "\t", "\u2003" + ROLE + "\u2003")) {
            Map<String, String> original = Map.of(PATH, source(marker));
            assertEquals(original, M3IntSpectrumTest.apply(new M3PatternizePureIntAtomRecipe(), original));
        }
    }

    private static String source(String comment) {
        return "package spectrum;\nfinal class Marker {\n"
                + "  private static int f(int a, int b) {\n"
                + "    /* " + comment + " */ int m3$pureIntAtom = a + b;\n"
                + "    return m3$pureIntAtom;\n  }\n}\n";
    }
}
