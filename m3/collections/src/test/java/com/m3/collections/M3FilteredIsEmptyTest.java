// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning Maven/JUnit entrypoint for filtered Collection/Set/SortedSet/NavigableSet. */
final class M3FilteredIsEmptyTest {
    @Test void directTerminalSelectionMatchesOriginalCursor() {
        M3FilteredIsEmptyContract.main(new String[0]);
    }
}
