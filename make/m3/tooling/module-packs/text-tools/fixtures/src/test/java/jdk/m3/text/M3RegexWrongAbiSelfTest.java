// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.List;

/** A present library with the wrong ABI must NEVER become an AUTO Java fallback. */
public final class M3RegexWrongAbiSelfTest {
    private M3RegexWrongAbiSelfTest() { }

    public static void main(String[] args) {
        var plan = M3RegexLiteralPlan.compile("a", 0);
        for (int i = 0; i < 2; i++) {
            try {
                plan.select(List.of("a", "z"), M3RegexLiteralPlan.Backend.AUTO);
                throw new AssertionError("ABI mismatch silently admitted by AUTO");
            } catch (LinkageError expected) { /* correct fail-closed contract */ }
        }
        try {
            plan.select(List.of("a"), M3RegexLiteralPlan.Backend.NATIVE_REQUIRED);
            throw new AssertionError("ABI mismatch admitted by REQUIRED");
        } catch (LinkageError expected) { /* required JNI refuses */ }
        System.out.println("M3_UNIX_REGEX_WRONG_ABI_REFUSAL_PASS checks=3");
    }
}
