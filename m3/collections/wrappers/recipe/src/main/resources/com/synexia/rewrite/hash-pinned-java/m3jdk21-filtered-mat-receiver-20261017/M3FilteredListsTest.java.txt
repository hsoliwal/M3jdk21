// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit receiver, authored but not represented as an executed SDK/JUnit run. */
class M3FilteredListsTest {
    @Test void oracleHistories() { M3FilteredListsContract.main(new String[0]); }
    @Test void mutationAndNullEdges() { M3FilteredListsEdges.main(new String[0]); }
    @Test void linearTraversalEvidence() { M3FilteredListsWorkProof.main(new String[0]); }
}
