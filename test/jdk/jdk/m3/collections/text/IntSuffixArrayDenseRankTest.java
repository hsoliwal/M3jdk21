/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.text;

import org.junit.jupiter.api.Test;

/** Actual JUnit runner for the signed-token and full UTF-16 suffix oracle. */
public final class IntSuffixArrayDenseRankTest {
    @Test
    void signedAndUtf16Oracle() {
        IntSuffixArrayDenseRankSelfTest.main(new String[0]);
    }
}
