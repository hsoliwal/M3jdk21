/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit executes the same Java21 indexed-slot differential oracle as CI. */
final class M3ReadonlyFilteredSlotTest {
    @Test void readonlyFilteredNavigableMapUsesOriginalOwnerSlots() {
        M3ReadonlyFilteredSlotSelfTest.main(new String[0]);
    }
}
