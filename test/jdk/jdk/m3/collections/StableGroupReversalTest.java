// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Execute the recipe-received main oracle in the module's existing JUnit5 lane. */
//FLLDCIM: RELOCATE - donor blob c8bcdbbdbfe0 at d5ad5090d87e; recipe make/m3/collections/qualification/census2-c3-20261106/relocate.py v1
final class StableGroupReversalTest {
    @Test void stableDescendingRunsAndMasks() {
        StableGroupReversalSelfTest.main(new String[0]);
    }
}
