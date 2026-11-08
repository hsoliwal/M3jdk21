// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual module JUnit adapter; independent executable JNI acceptance also required. */
final class LongTopKFrequencyTest {
    @Test void signedLongRunTopKMatchesReference() {
        LongTopKFrequencyContract.contracts(false);
    }
}
