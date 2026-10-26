// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual module JUnit adapter; independent executable JNI acceptance also required. */
final class M3LongTopKFrequencyTest {
    @Test void signedLongRunTopKMatchesReference() {
        M3LongTopKFrequencyContract.contracts(false);
    }
}
