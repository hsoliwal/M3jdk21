// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entry point for the original filtered collection, sorted and deque owners. */
final class M3FilteredCursorStateTest {
    @Test
    void backedFilteredListDequeAndSortedCursorStateRemainsObservableEquivalent() {
        M3FilteredCursorStateProof.main(new String[0]);
    }
}
