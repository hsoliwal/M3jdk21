// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entry points over the same independently executable contract corpus. */
class M3FilteredTraversalTest {
    @Test void filteredSizeSaturates() { M3FilteredTraversalProof.filteredSizeSaturates(); }
    @Test void flattenedSizeSaturates() { M3FilteredTraversalProof.flattenedSizeSaturates(); }
    @Test void flatCursorDoesNotCountOrBind() { M3FilteredTraversalProof.flatCursorDoesNotCountOrBind(); }
    @Test void oneShotIterableIsNotConsumedByMetadata() { M3FilteredTraversalProof.oneShotIterableIsNotConsumedByMetadata(); }
    @Test void shortCircuitDoesNotVisitSuffix() { M3FilteredTraversalProof.shortCircuitDoesNotVisitSuffix(); }
    @Test void nullAndEmptyTraversalContracts() { M3FilteredTraversalProof.nullAndEmptyTraversalContracts(); }
    @Test void saturationPreservesLateFailures() { M3FilteredTraversalProof.saturationPreservesLateFailures(); }
}
