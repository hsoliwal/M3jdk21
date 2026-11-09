// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoint for no-entry live primitive set algebra and JDK oracles. */
final class LongSortedAlgebraTest {
    @Test
    void seededSortedSetAlgebraAndFilterComposition() throws Exception {
        LongSortedAlgebraSelfTest.main(new String[0]);
    }
}
