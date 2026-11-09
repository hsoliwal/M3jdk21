// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** The same exact primitive oracle runs under Maven/JUnit and standalone Java21. */
final class MaskAllSelectionReceiverTest {
    @Test void everyMaskAndDefensiveResultIsCorrect() {
        MaskDenseSelectionProof.main(new String[0]);
    }
}
