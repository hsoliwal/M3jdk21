/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.RelationForEachSelfTest;

/** Maven/Jupiter entry point for the standalone relation traversal contract oracle. */
final class M3RelationForEachTest {
    @Test void preservesRelationTraversalContracts() {
        RelationForEachSelfTest.main(new String[] {"contracts"});
    }
}
