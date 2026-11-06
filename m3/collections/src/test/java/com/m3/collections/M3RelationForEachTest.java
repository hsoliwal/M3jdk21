/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/Jupiter entry point for the standalone relation traversal contract oracle. */
final class M3RelationForEachTest {
    @Test void preservesRelationTraversalContracts() {
        M3RelationForEachSelfTest.main(new String[] {"contracts"});
    }
}
