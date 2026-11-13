// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Arrays;
import java.util.List;

/** Run only in a fresh JVM with an intentionally empty native library search path. */
public final class M3RegexRequiredSegmentsNoNativeSelfTest {
    private M3RegexRequiredSegmentsNoNativeSelfTest() { }
    public static void main(String[] args) {
        M3RegexRequiredSegments plan = M3RegexRequiredSegments.compile("^a.*b$", 0);
        List<String> lines = List.of("ab", "acb", "a\nb", "bb", "a🙂b", "a", "");
        int[] expected = plan.select(lines, M3RegexRequiredSegments.Backend.JAVA);
        for (int repeat = 0; repeat < 8; repeat++) {
            int[] result = plan.select(lines, M3RegexRequiredSegments.Backend.AUTO);
            if (!Arrays.equals(expected, result)) {
                throw new AssertionError("missing-native AUTO fallback " + repeat);
            }
        }
        boolean rejected = false;
        try { plan.select(lines, M3RegexRequiredSegments.Backend.NATIVE_REQUIRED); }
        catch (UnsatisfiedLinkError | NoClassDefFoundError expectedFailure) { rejected = true; }
        if (!rejected) throw new AssertionError("required native unexpectedly available");
        System.out.println("M3_REQUIRED_SEGMENTS_MISSING_NATIVE_PASS checks=9");
    }
}
