/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit receiver for the identical deterministic Java21 set-slot oracle. */
final class M3FilteredSortedSetSlotTest {
    @Test void backedReadOnlyAndMutableSortedSetSlots() {
        M3FilteredSortedSetSlotSelfTest.main(new String[0]);
    }
}
