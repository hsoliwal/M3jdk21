// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning Maven/JUnit entrypoint for filtered Collection/Set/SortedSet/NavigableSet. */
final class FilteredIsEmptyTest {
    @Test void directTerminalSelectionMatchesOriginalCursor() {
        FilteredIsEmptyContract.main(new String[0]);
    }
}
