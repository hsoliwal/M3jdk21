// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Permanent counterexample: failed JNI initialization marks its holder erroneous. */
public final class M3RegexMissingNativeSelfTest {
    private M3RegexMissingNativeSelfTest() { }

    public static void main(String[] args) {
        List<String> lines = List.of("a", "abc", "z", "a");
        var plan = M3RegexLiteralPlan.compile("a", 0);
        for (int repeat = 0; repeat < 8; repeat++) {
            int[] positions = plan.select(lines, M3RegexLiteralPlan.Backend.AUTO);
            if (!Arrays.equals(positions, new int[] {0, 1, 3})) {
                throw new AssertionError("AUTO must remain Java-semantic after library rejection");
            }
        }
        // An unavailable native backend is not silently treated as native success.
        try {
            plan.select(lines, M3RegexLiteralPlan.Backend.NATIVE_REQUIRED);
            throw new AssertionError("required native call unexpectedly succeeded");
        } catch (LinkageError expected) { /* explicit fail-closed */ }
        var unknown = M3RegexLiteralPlan.fromPattern(Pattern.compile("(a)\\1"));
        if (!Arrays.equals(unknown.select(lines, M3RegexLiteralPlan.Backend.AUTO),
                new int[] {})) throw new AssertionError("unknown regex fallback changed");
        System.out.println("M3_UNIX_REGEX_MISSING_NATIVE_PASS checks=10");
    }
}
