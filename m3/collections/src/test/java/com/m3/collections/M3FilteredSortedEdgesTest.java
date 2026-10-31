/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit admission for the independent executable sorted-view edge oracles. */
class M3FilteredSortedEdgesTest {
    @Test void nullableNavigationWitness() { M3FilteredSortedEdgesContract.navigationWitness(); }
    @Test void legacyEntryCallbackTrace() { M3FilteredSortedEdgesContract.entryCompatibilityTrace(); }
    @Test void nullableNavigationMatrix() { M3FilteredSortedEdgesContract.navigationMatrix(); }
    @Test void liveNestedRanges() { M3FilteredSortedEdgesContract.liveRanges(); }
    @Test void comparatorMembershipMatrix() { M3FilteredSortedEdgesContract.membershipMatrix(); }
    @Test void storedCallbacksAndExceptions() { M3FilteredSortedEdgesContract.callbacksAndExceptions(); }
    @Test void boundedTraversalWork() { M3FilteredSortedEdgesContract.boundedWork(); }
    @Test void noExtraNaturalComparisons() { M3FilteredSortedEdgesContract.naturalOrderAbsence(); }
}
