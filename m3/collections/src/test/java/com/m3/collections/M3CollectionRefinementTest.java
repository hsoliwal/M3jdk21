// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Executes the collection refinement proofs through the owning Maven/JUnit build. */
final class M3CollectionRefinementTest {
    @Test
    void clearReleasesEveryUsedSlotAndPreservesViews() throws Exception {
        M3LinkedListClearRangeProof.main(new String[0]);
    }

    @Test
    void filteredTerminalsPreserveCallbackObservations() throws Exception {
        M3GenericFilteredTerminalProof.main(new String[0]);
    }

    @Test
    void primitiveCountsPreserveCallbackObservations() throws Exception {
        M3PrimitiveSequenceCountProof.main(new String[0]);
    }

    @Test
    void stableIndexSortPreservesSnapshotOwnership() throws Exception {
        M3SnapshotSortIsolationProof.main(new String[0]);
    }

    @Test
    void packedSelectionPreservesFusedSortIsolation() throws Exception {
        M3PackedPredicateSelectionProof.main(new String[0]);
    }
}
