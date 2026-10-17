// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit receiving entrypoint for the executable primitive contract oracle. */
final class M3LongCompositeLateBindTest {
    @Test void compositeSpliteratorsBindOnFirstTraversal() {
        M3LongCompositeLateBindSelfTest.main(new String[0]);
    }
}
