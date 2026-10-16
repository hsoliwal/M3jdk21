// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoint: actual primitive map/set with direct mask compaction. */
final class M3IndexMaskKeyedTest {
    @Test
    void deterministicKeyedMaskDifferential() {
        M3IndexMaskKeyedSelfTest.main(new String[0]);
    }
}
