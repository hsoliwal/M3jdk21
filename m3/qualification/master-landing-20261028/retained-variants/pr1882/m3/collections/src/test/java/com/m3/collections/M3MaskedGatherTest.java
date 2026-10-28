// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Fused masked gather: standalone Java/native differential oracle. */
final class M3MaskedGatherTest {
    @Test
    void nativeAndJavaMaskedIndexParity() {
        M3MaskedGatherContract.main(new String[0]);
    }
}
