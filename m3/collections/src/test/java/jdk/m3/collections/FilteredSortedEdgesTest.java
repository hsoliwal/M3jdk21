/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit admission for the independent executable sorted-view edge oracles. */
class FilteredSortedEdgesTest {
    @Test void nullableNavigationWitness() { FilteredSortedEdgesContract.navigationWitness(); }
    @Test void legacyEntryCallbackTrace() { FilteredSortedEdgesContract.entryCompatibilityTrace(); }
    @Test void nullableNavigationMatrix() { FilteredSortedEdgesContract.navigationMatrix(); }
    @Test void liveNestedRanges() { FilteredSortedEdgesContract.liveRanges(); }
    @Test void comparatorMembershipMatrix() { FilteredSortedEdgesContract.membershipMatrix(); }
    @Test void storedCallbacksAndExceptions() { FilteredSortedEdgesContract.callbacksAndExceptions(); }
    @Test void boundedTraversalWork() { FilteredSortedEdgesContract.boundedWork(); }
    @Test void noExtraNaturalComparisons() { FilteredSortedEdgesContract.naturalOrderAbsence(); }
}
