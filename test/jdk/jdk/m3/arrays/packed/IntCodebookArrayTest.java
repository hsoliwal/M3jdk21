// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import org.junit.jupiter.api.Test;

/** JUnit bridge to the standalone Java and optional JNI differential harness. */
final class IntCodebookArrayTest {
    @Test
    void losslessValuesAndRequiredNativeWhenConfigured() {
        IntCodebookSelfTestMain.main(new String[0]);
    }
}
