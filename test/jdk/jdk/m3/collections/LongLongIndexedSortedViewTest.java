// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Real JUnit wrapper around the primitive differential oracle. */
//FLLDCIM: RELOCATE - donor blob df1a52f6ddf6 at ca1aafc4d94f; recipe m3/collections/qualification/census-g4-20261106/relocate.py v4
final class LongLongIndexedSortedViewTest {
    @Test void sortedFilteredArrayIndexMatchesTreeMap() {
        LongLongIndexedSortedViewProof.main(new String[0]);
    }
}
