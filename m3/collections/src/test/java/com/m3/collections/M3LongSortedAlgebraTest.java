// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoint for no-entry live primitive set algebra and JDK oracles. */
final class M3LongSortedAlgebraTest {
    @Test
    void seededSortedSetAlgebraAndFilterComposition() throws Exception {
        M3LongSortedAlgebraSelfTest.main(new String[0]);
    }
}
