// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.FilteredListsContract;
import jdk.internal.m3.collections.FilteredListsEdges;
import jdk.internal.m3.collections.FilteredListsWorkProof;

/** Maven/JUnit receiver, authored but not represented as an executed SDK/JUnit run. */
class FilteredListsTest {
    @Test void oracleHistories() { FilteredListsContract.main(new String[0]); }
    @Test void mutationAndNullEdges() { FilteredListsEdges.main(new String[0]); }
    @Test void linearTraversalEvidence() { FilteredListsWorkProof.main(new String[0]); }
}
