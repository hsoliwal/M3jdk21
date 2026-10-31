// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** The same exact primitive oracle runs under Maven/JUnit and standalone Java21. */
final class M3MaskAllSelectionReceiverTest {
    @Test void everyMaskAndDefensiveResultIsCorrect() {
        M3MaskDenseSelectionProof.main(new String[0]);
    }
}
