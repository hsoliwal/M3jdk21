// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.List;

/** Run only in a fresh JVM against the deliberately wrong-ABI native library. */
public final class M3RegexRequiredSegmentsWrongAbiSelfTest {
    private M3RegexRequiredSegmentsWrongAbiSelfTest() { }
    public static void main(String[] args) {
        M3RegexRequiredSegments plan = M3RegexRequiredSegments.compile("a.*b", 0);
        int rejected = 0;
        for (M3RegexRequiredSegments.Backend backend : List.of(
                M3RegexRequiredSegments.Backend.NATIVE_REQUIRED,
                M3RegexRequiredSegments.Backend.AUTO)) {
            try { plan.select(List.of("ab"), backend); }
            catch (UnsatisfiedLinkError | NoClassDefFoundError expected) { rejected++; }
        }
        if (rejected != 2) throw new AssertionError("wrong ABI must refuse, not silently use Java");
        System.out.println("M3_REQUIRED_SEGMENTS_WRONG_ABI_PASS checks=2");
    }
}
