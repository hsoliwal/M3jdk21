// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit bridge; the Java-only contract mains are also runnable without Maven. */
final class M3FilteredSortedMultisetsTest {
    @Test void deterministicCountViewContract() {
        M3FilteredSortedMultisetContract.main(new String[0]);
    }
    @Test void edgeAndLargeCountContract() {
        M3FilteredSortedMultisetEdges.main(new String[0]);
    }
}
