/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import com.m3.util.M3PrimitiveIndexViewFilterContract;
import org.junit.jupiter.api.Test;

/** Maven delegates to the same independent mains used in the linked-image proof. */
final class M3CollectionsPackTest {
    @Test
    void packedDecisionBoundariesAndComposition() {
        com.m3.util.M3PrimitiveSelectionWordContract.main(new String[0]);
    }

    @Test
    void readOnlyFilteredAndSortedViews() {
        M3FilteredViewsContract.main(new String[0]);
    }

    @Test
    void mutableFilteredAndSortedViews() {
        M3MutableFilteredViewsContract.main(new String[0]);
    }

    @Test
    void versionedPrimitiveFilterProjection() {
        M3PrimitiveIndexViewFilterContract.main(new String[0]);
    }
}
