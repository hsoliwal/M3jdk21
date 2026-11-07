// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3LongPairDistanceContract;

/** Existing M3JDK 21 collection reactor adapter for JNI/Java pair distance contract. */
//FLLDCIM: RELOCATE - donor blob 7dbc1c1bc8f7 at d5ad5090d87e; recipe m3/collections/qualification/census2-c4-20261106/relocate.py v1
final class M3LongPairDistanceTest {
    @Test void batchPairDistanceJavaAndJniParity() {
        M3LongPairDistanceContract.contracts();
    }
}
