/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Qualified in the original M3JDK 21 collections Maven module. */
final class IndexedDescendingGroupMaskTest {
    @Test
    void comparatorTapesAndStablePhysicalSlotsStayUnchanged() {
        IndexedDescendingGroupMaskSelfTest.main(new String[0]);
    }
}
