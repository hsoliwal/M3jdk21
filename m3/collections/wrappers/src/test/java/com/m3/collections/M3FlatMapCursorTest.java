// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Real Maven/Surefire endpoint for the Java 21 flattened collection cursor proof. */
final class M3FlatMapCursorTest {
    @Test
    void unsizedLazyFlatMapMatchesJdkCollectionContracts() {
        M3FlatMapCursorProof.main(new String[0]);
    }
}
