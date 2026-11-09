// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

/** Wrong native ABI must never be mistaken for an optional missing library. */
public final class M3NativeAhoWrongAbiSelfTest {
    private M3NativeAhoWrongAbiSelfTest() { }

    public static void main(String[] args) {
        try { M3NativeAho.available(); }
        catch (UnsatisfiedLinkError expected) {
            if (!String.valueOf(expected.getMessage()).contains("ABI mismatch")) {
                throw new AssertionError("unexpected linkage failure", expected);
            }
            System.out.println("M3_AHO_WRONG_ABI_REFUSED_PASS");
            return;
        }
        throw new AssertionError("mismatched ABI was accepted");
    }
}
