// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** JDK receiver oracle for current-Xmx facts; no new memory manager is introduced. */
final class M3ObjectResidencyRuntimePressureTest {
    @Test
    void targetUsesTheActualRunningJvmHeap() {
        M3ObjectResidency.HeapSnapshot actual = M3ObjectResidency.HeapSnapshot.current();
        long maximum = Runtime.getRuntime().maxMemory();
        assertEquals(maximum, actual.maxBytes());
        assertTrue(actual.usedBytes() >= 0);
        assertTrue(actual.committedBytes() >= actual.usedBytes());
        assertEquals(Math.max(0L, maximum - actual.usedBytes()), actual.estimatedHeadroomBytes());
    }
}
