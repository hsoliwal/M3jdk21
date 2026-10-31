// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Existing M3JDK21 collection reactor adapter for JNI/Java pair distance contract. */
final class M3LongPairDistanceTest {
    @Test void batchPairDistanceJavaAndJniParity() {
        M3LongPairDistanceContract.contracts();
    }
}
